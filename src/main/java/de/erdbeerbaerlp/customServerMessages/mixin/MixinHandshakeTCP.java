package de.erdbeerbaerlp.customServerMessages.mixin;

import com.mojang.authlib.GameProfile;
import de.erdbeerbaerlp.customServerMessages.CustomMessages;
import de.erdbeerbaerlp.customServerMessages.CustomServerMessagesMod;
import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.ServerStatusResponse;
import net.minecraft.network.handshake.client.C00Handshake;
import net.minecraft.network.login.server.SPacketDisconnect;
import net.minecraft.network.status.server.SPacketServerInfo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.network.NetHandlerHandshakeTCP;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.FMLLog;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import java.util.UUID;

@Mixin(NetHandlerHandshakeTCP.class)
public class MixinHandshakeTCP {
    @Final
    @Shadow
    private MinecraftServer server;
    @Final
    @Shadow
    private NetworkManager networkManager;


    @Redirect(method = "processHandshake", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fml/common/FMLCommonHandler;handleServerHandshake(Lnet/minecraft/network/handshake/client/C00Handshake;Lnet/minecraft/network/NetworkManager;)Z", ordinal = 0, remap = false))
    private boolean packet(FMLCommonHandler fmlCommonHandler, C00Handshake packet, NetworkManager manager) {
        DedicatedServer.allowPlayerLogins = true;
        final boolean fml = fmlCommonHandler.handleServerHandshake(packet, manager);
        if (!fml) return false;
        int version = 0;
        //noinspection ConstantConditions
        if (MinecraftForge.MC_VERSION.equals("1.12.2"))
            version = 340;
        else if (MinecraftForge.MC_VERSION.equals("1.12.1"))
            version = 338;
        else if (MinecraftForge.MC_VERSION.equals("1.12"))
            version = 335;
        if (CustomServerMessagesMod.serverStarted) {
            if (packet.getRequestedState() != EnumConnectionState.STATUS) return true;
            if (packet.getRequestedState() == EnumConnectionState.STATUS && CustomMessages.CUSTOM_MOTD_ENABLED) {
                final ServerStatusResponse statusResp = server.getServerStatusResponse();
                final ServerStatusResponse.Players statusPlayers = new ServerStatusResponse.Players(server.getMaxPlayers(), server.getCurrentPlayerCount());
                final GameProfile[] playersIn = new GameProfile[]{new GameProfile(UUID.randomUUID(),
                        CustomMessages.CUSTOM_MOTD_PLAYER_HOVER
                                .replace("%online%", server.getCurrentPlayerCount() + "")
                                .replace("%max%", server.getMaxPlayers() + "")
                                .replace("%gamemode%", server.getGameType().getName())
                                .replace("%playerlist%", getPlayerList())
                )};
                statusPlayers.setPlayers(playersIn);
                statusResp.setVersion(new ServerStatusResponse.Version(CustomMessages.CUSTOM_MOTD_USE_VERSION ?
                        CustomMessages.CUSTOM_MOTD_VERSION
                                .replace("%online%", server.getCurrentPlayerCount() + "")
                                .replace("%max%", server.getMaxPlayers() + "")
                        : MinecraftForge.MC_VERSION, CustomMessages.CUSTOM_MOTD_USE_VERSION ? Integer.MAX_VALUE : version));
                statusResp.setPlayers(statusPlayers);
                statusResp.setServerDescription(new TextComponentString(CustomMessages.getRandomMOTD()
                        .replace("%online%", server.getCurrentPlayerCount() + "")
                        .replace("%max%", "" + server.getMaxPlayers())
                        .replace("%time%", CustomServerMessagesMod.getOverworldTime(false))
                        .replace("%time-colored%", CustomServerMessagesMod.getOverworldTime(true))));
                server.applyServerIconToResponse(statusResp);
                return true;
            }
            return true;
        } else {
            final TextComponentString startKick = new TextComponentString(CustomMessages.START_KICK_MSG);
            if (packet.getRequestedState().equals(EnumConnectionState.LOGIN)) {
                if (CustomMessages.LOG_START_DISCONNECT)
                    FMLLog.log.info("Disconnecting Player: Server is starting");
                networkManager.sendPacket(new SPacketDisconnect(startKick));
                networkManager.closeChannel(startKick);
                return false;
            }
            if (packet.getRequestedState().equals(EnumConnectionState.STATUS)) {
                final ServerStatusResponse statusResp = server.getServerStatusResponse();
                final ServerStatusResponse.Players statusPlayers = new ServerStatusResponse.Players(-1, -1);
                final GameProfile[] playersIn = new GameProfile[]{new GameProfile(UUID.randomUUID(), CustomMessages.START_VERSION_HOVER)};
                statusPlayers.setPlayers(playersIn);
                statusResp.setVersion(new ServerStatusResponse.Version(CustomMessages.USE_VERSION ? CustomMessages.START_VERSION.replace("%time%", CustomServerMessagesMod.getEstimatedStartTime()) : MinecraftForge.MC_VERSION, CustomMessages.USE_VERSION ? Integer.MAX_VALUE : version));
                statusResp.setPlayers(statusPlayers);
                statusResp.setServerDescription(new TextComponentString(CustomMessages.START_MOTD.replace("%time%", CustomServerMessagesMod.getEstimatedStartTime())));
                //server.applyServerIconToResponse(statusResp);
                networkManager.sendPacket(new SPacketServerInfo(statusResp));
                return false;
            }
        }
        return true;
    }

    private String getPlayerList() {
        final GameProfile[] agameprofile = new GameProfile[Math.min(server.getCurrentPlayerCount(), 12)];
        int j = MathHelper.getInt(new Random(), 0, server.getCurrentPlayerCount() - agameprofile.length);

        for (int k = 0; k < agameprofile.length; ++k) {
            agameprofile[k] = server.getPlayerList().getPlayers().get(j + k).getGameProfile();
        }

        Collections.shuffle(Arrays.asList(agameprofile));
        String out = "";
        for (GameProfile p : agameprofile) {
            out = out + p.getName();
        }
        return out;
    }
}
