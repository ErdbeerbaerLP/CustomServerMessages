package de.erdbeerbaerlp.customServerMessages.mixin;

import de.erdbeerbaerlp.customServerMessages.Configuration;
import de.erdbeerbaerlp.customServerMessages.CustomMessageMod;
import de.erdbeerbaerlp.customServerMessages.CustomStatusPacketListenerImpl;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import net.minecraft.network.protocol.login.ClientboundLoginDisconnectPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.network.ServerHandshakePacketListenerImpl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerHandshakePacketListenerImpl.class)
public class MixinServerHandshakePacketListenerImpl {
    @Final
    @Shadow
    private static Component IGNORE_STATUS_REASON;
    @Shadow
    @Final
    private MinecraftServer server;
    @Final
    @Shadow
    private Connection connection;


    @Inject(method = "handleIntention", at = @At("HEAD"), cancellable = true)
    public void handleIntention(ClientIntentionPacket packet, CallbackInfo ci) {
        if (packet.getIntention() != ConnectionProtocol.STATUS && CustomMessageMod.started) return;
        ci.cancel();
        switch (packet.getIntention()) {
            case LOGIN:
                this.connection.setProtocol(ConnectionProtocol.LOGIN);
                final MutableComponent component = Component.literal(Configuration.instance().messages.serverStartingKickMessage);
                if (Configuration.instance().general.logStartDisconnects)
                    CustomMessageMod.LOGGER.info("Disconnecting Player (server is still starting): {}", component.getContents());
                connection.send(new ClientboundLoginDisconnectPacket(component));
                connection.disconnect(component);
                break;
            case STATUS:
                if (((DedicatedServer) server).getProperties().enableStatus) {
                    this.connection.setProtocol(ConnectionProtocol.STATUS);
                    this.connection.setListener(new CustomStatusPacketListenerImpl(server, this.connection));
                } else {
                    this.connection.disconnect(IGNORE_STATUS_REASON);
                }
                break;
            default:
                throw new UnsupportedOperationException("Invalid intention " + packet.getIntention());
        }

    }
}
