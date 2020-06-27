package de.erdbeerbaerlp.customServerMessages.mixin;

import com.mojang.authlib.GameProfile;
import de.erdbeerbaerlp.customServerMessages.CustomMessagesConfig;
import de.erdbeerbaerlp.customServerMessages.CustomServerMessagesMod;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.ProtocolType;
import net.minecraft.network.ServerStatusResponse;
import net.minecraft.network.handshake.ServerHandshakeNetHandler;
import net.minecraft.network.handshake.client.CHandshakePacket;
import net.minecraft.network.login.server.SDisconnectLoginPacket;
import net.minecraft.network.status.server.SServerInfoPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.SharedConstants;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.storage.SaveFormat;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Mixin(ServerHandshakeNetHandler.class)
public class MixinServerHandshakeNetHandler {
    @Final
    @Shadow
    private MinecraftServer server;
    @Final
    @Shadow
    private NetworkManager networkManager;


    @Redirect(method = "processHandshake", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fml/server/ServerLifecycleHooks;handleServerLogin(Lnet/minecraft/network/handshake/client/CHandshakePacket;Lnet/minecraft/network/NetworkManager;)Z", ordinal = 0, remap = false))
    private boolean packet(CHandshakePacket packet, NetworkManager manager) {
        try {
            final Field allowLogins = ServerLifecycleHooks.class.getDeclaredField("allowLogins");
            allowLogins.setAccessible(true);
            allowLogins.set(null, new AtomicBoolean(true));
        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        }
        final boolean fml = ServerLifecycleHooks.handleServerLogin(packet, manager);
        if (!fml) return false;
        int version = SharedConstants.getVersion().getProtocolVersion();
        if (CustomServerMessagesMod.serverStarted) {
            if (packet.getRequestedState() != ProtocolType.STATUS) return true;
            if (packet.getRequestedState() == ProtocolType.STATUS && CustomMessagesConfig.instance().general.enableCustomMOTD) {
                final ServerStatusResponse statusResp = server.getServerStatusResponse();
                final ServerStatusResponse.Players statusPlayers = new ServerStatusResponse.Players(server.getMaxPlayers(), server.getCurrentPlayerCount());
                final GameProfile[] playersIn = new GameProfile[]{new GameProfile(UUID.randomUUID(),
                        CustomMessagesConfig.instance().messages.customMOTDPlayerListHover
                                .replace("%online%", server.getCurrentPlayerCount() + "")
                                .replace("%max%", server.getMaxPlayers() + "")
                                .replace("%gamemode%", server.getGameType().getName())
                                .replace("%playerlist%", getPlayerList())
                                .replace("%time%", CustomServerMessagesMod.getOverworldTime(false))
                                .replace("%time-colored%", CustomServerMessagesMod.getOverworldTime(true))
                )};
                statusPlayers.setPlayers(playersIn);
                statusResp.setVersion(new ServerStatusResponse.Version(CustomMessagesConfig.instance().general.useCustomMOTDVersion ?
                        CustomMessagesConfig.instance().messages.customMOTDPlayerListText
                                .replace("%online%", server.getCurrentPlayerCount() + "")
                                .replace("%max%", server.getMaxPlayers() + "")
                        : SharedConstants.getVersion().getName(), CustomMessagesConfig.instance().general.useCustomMOTDVersion ? Integer.MAX_VALUE : version));
                statusResp.setPlayers(statusPlayers);
                statusResp.setServerDescription(new StringTextComponent(CustomMessagesConfig.instance().getRandomMOTD()
                        .replace("%online%", server.getCurrentPlayerCount() + "")
                        .replace("%max%", "" + server.getMaxPlayers())
                        .replace("%time%", CustomServerMessagesMod.getOverworldTime(false))
                        .replace("%time-colored%", CustomServerMessagesMod.getOverworldTime(true))));
                addServerIcon(statusResp);
                return true;
            }
            return true;
        } else {
            final StringTextComponent startKick = new StringTextComponent(CustomMessagesConfig.instance().messages.serverStartingKickMessage);
            if (packet.getRequestedState().equals(ProtocolType.LOGIN)) {
                networkManager.setConnectionState(ProtocolType.LOGIN);
                if (CustomMessagesConfig.instance().general.logStartDisconnects)
                    LogManager.getLogger().info("Disconnecting Player: Server is starting");
                networkManager.sendPacket(new SDisconnectLoginPacket(startKick));
                networkManager.closeChannel(startKick);
                return false;
            }
            if (packet.getRequestedState().equals(ProtocolType.STATUS)) {
                final ServerStatusResponse statusResp = server.getServerStatusResponse();
                final ServerStatusResponse.Players statusPlayers = new ServerStatusResponse.Players(-1, -1);
                final GameProfile[] playersIn = new GameProfile[]{new GameProfile(UUID.randomUUID(), CustomMessagesConfig.instance().messages.startPlayerListTextHover)};
                statusPlayers.setPlayers(playersIn);
                statusResp.setVersion(new ServerStatusResponse.Version(CustomMessagesConfig.instance().general.useStartVersion ? CustomMessagesConfig.instance().messages.startPlayerListText.replace("%time%", CustomServerMessagesMod.getEstimatedStartTime()) : SharedConstants.getVersion().getName(), CustomMessagesConfig.instance().general.useStartVersion ? Integer.MAX_VALUE : version));
                statusResp.setPlayers(statusPlayers);
                statusResp.setServerDescription(new StringTextComponent(CustomMessagesConfig.instance().messages.serverStartingMOTD.replace("%time%", CustomServerMessagesMod.getEstimatedStartTime())));
                addServerIcon(statusResp);
                this.networkManager.setConnectionState(ProtocolType.STATUS);
                networkManager.sendPacket(new SServerInfoPacket(statusResp));
                return false;
            }
        }
        return true;
    }
    private void addServerIcon(ServerStatusResponse response) {
        File file1 = new File(CustomMessagesConfig.instance().general.startServerIconPath);
        if (CustomServerMessagesMod.serverStarted || !file1.exists()) {
            file1 = new File(CustomMessagesConfig.instance().general.serverIconPath);
            if (!file1.exists()) {
                final SaveFormat.LevelSave anvilConverterForAnvilFile = ObfuscationReflectionHelper.getPrivateValue(MinecraftServer.class, server, "field_71310_m");
                file1 = anvilConverterForAnvilFile.func_237298_f_();
            }
        }
        if (file1.isFile()) {
            ByteBuf bytebuf = Unpooled.buffer();
            try {
                BufferedImage bufferedimage = ImageIO.read(file1);
                Validate.validState(bufferedimage.getWidth() == 64, "Must be 64 pixels wide");
                Validate.validState(bufferedimage.getHeight() == 64, "Must be 64 pixels high");
                ImageIO.write(bufferedimage, "PNG", new ByteBufOutputStream(bytebuf));
                ByteBuffer bytebuffer = Base64.getEncoder().encode(bytebuf.nioBuffer());
                response.setFavicon("data:image/png;base64," + StandardCharsets.UTF_8.decode(bytebuffer));
            } catch (Exception exception) {
                LogManager.getLogger().error("Couldn't load server icon", exception);
            } finally {
                bytebuf.release();
            }
        }
    }

    private String getPlayerList() {
        final GameProfile[] agameprofile = new GameProfile[Math.min(server.getCurrentPlayerCount(), 12)];
        int j = MathHelper.nextInt(new Random(), 0, server.getCurrentPlayerCount() - agameprofile.length);

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
