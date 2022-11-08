package de.erdbeerbaerlp.customServerMessages.mixin;

import de.erdbeerbaerlp.customServerMessages.Configuration;
import de.erdbeerbaerlp.customServerMessages.CustomMessageMod;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.network.protocol.login.ClientboundLoginDisconnectPacket;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Connection.class)
public abstract class MixinConnection {
    private static Packet<?> modifyPacket(Packet<?> packetIn) {
        if (packetIn instanceof final ClientboundSystemChatPacket msg) {
            final Component com = msg.content();
            if (com instanceof final MutableComponent c) {
                if (c.getContents() instanceof final TranslatableContents tct) {
                    if (tct.getArgs().length > 0)
                        if (tct.getArgs()[0] instanceof final Component cmp) {
                            final String player = cmp.getString();
                            final boolean timeout = CustomMessageMod.playerTimeouts.contains(player);
                            if (timeout) CustomMessageMod.playerTimeouts.remove(player);
                            if (tct.getKey().startsWith("multiplayer.player.left")) {
                                final MutableComponent leaveMsg = Component.literal((timeout ? Configuration.instance().messages.timeoutLeaveMessage : Configuration.instance().messages.leaveMessage).replace("%player%", player));
                                leaveMsg.setStyle(c.getStyle());
                                return new ClientboundSystemChatPacket(leaveMsg, msg.overlay());
                            }
                            if (tct.getKey().startsWith("multiplayer.player.joined")) {
                                final MutableComponent joinMsg = Component.literal(Configuration.instance().messages.joinMessage.replace("%player%", player));
                                joinMsg.setStyle(c.getStyle());
                                return new ClientboundSystemChatPacket(joinMsg, msg.overlay());
                            }
                        }

                }
            }
        }
        if (packetIn instanceof final ClientboundLoginDisconnectPacket p) {
            if (p.getReason().getContents() instanceof final TranslatableContents tct) {
                if (tct.getKey().startsWith("multiplayer.disconnect.incompatible")) {
                    return new ClientboundLoginDisconnectPacket(Component.literal(Configuration.instance().messages.outdatedServerKick));
                }
                if (tct.getKey().startsWith("multiplayer.disconnect.outdated_client")) {
                    return new ClientboundLoginDisconnectPacket(Component.literal(Configuration.instance().messages.outdatedClientKick));
                }
            }

        }
        if (packetIn instanceof final ClientboundDisconnectPacket p) {
            if (p.getReason().getContents() instanceof final TranslatableContents tct) {
                if (tct.getKey().startsWith("multiplayer.disconnect.server_shutdown")) {
                    return new ClientboundDisconnectPacket(Component.literal(Configuration.instance().messages.serverStoppedMessage));
                }
                if (tct.getKey().startsWith("disconnect.spam")) {
                    return new ClientboundDisconnectPacket(Component.literal(Configuration.instance().messages.spamKick));
                }
                if (tct.getKey().startsWith("multiplayer.disconnect.idling")) {
                    return new ClientboundDisconnectPacket(Component.literal(Configuration.instance().messages.idleTimeoutKick));
                }
            }
        }
        return packetIn;
    }

    @Shadow
    protected abstract void sendPacket(Packet<?> p_129521_, @Nullable PacketSendListener p_243246_);

    @Redirect(method = "send(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/Connection;sendPacket(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketSendListener;)V"))
    private void send(Connection instance, Packet<?> packetIn, PacketSendListener futureListeners) {
        packetIn = modifyPacket(packetIn);
        sendPacket(packetIn, futureListeners);
    }
}
