package de.erdbeerbaerlp.customServerMessages.mixin;

import de.erdbeerbaerlp.customServerMessages.CustomMessages;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.SPacketChat;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.annotation.Nullable;

@Mixin(NetworkManager.class)
public abstract class MixinNetworkManager {

    private static Packet<?> modifyPacket(Packet<?> packetIn) {
        if (packetIn instanceof SPacketChat) {
            final SPacketChat msg = (SPacketChat) packetIn;
            final ITextComponent com = ObfuscationReflectionHelper.getPrivateValue(SPacketChat.class, msg, "chatComponent", "field_148919_a", "a");
            if (com instanceof TextComponentTranslation) {
                final TextComponentTranslation tct = (TextComponentTranslation) com;
                final String player = tct.getUnformattedText().split(" ")[0];
                boolean timeout = false;
                if (tct.getKey().startsWith("multiplayer.player.left")) {
                    final TextComponentString leaveMsg = new TextComponentString((timeout ? CustomMessages.LEAVE_MSG_TIMEOUT : CustomMessages.LEAVE_MSG).replace("%player%", player));
                    leaveMsg.setStyle(tct.getStyle());
                    return new SPacketChat(leaveMsg, msg.getType());
                }
                if (tct.getKey().startsWith("multiplayer.player.joined")) {
                    final TextComponentString joinMsg = new TextComponentString(CustomMessages.JOIN_MSG.replace("%player%", player));
                    joinMsg.setStyle(tct.getStyle());
                    return new SPacketChat(joinMsg, msg.getType());
                }
            }
        }
        if (packetIn instanceof net.minecraft.network.login.server.SPacketDisconnect) {
            final net.minecraft.network.login.server.SPacketDisconnect p = (net.minecraft.network.login.server.SPacketDisconnect) packetIn;
            if (ObfuscationReflectionHelper.getPrivateValue(net.minecraft.network.login.server.SPacketDisconnect.class, p, "reason", "a", "field_149605_a") instanceof TextComponentTranslation) {
                final TextComponentTranslation tct = ObfuscationReflectionHelper.getPrivateValue(net.minecraft.network.login.server.SPacketDisconnect.class, p, "reason", "a", "field_149605_a");
                System.out.println(tct.getKey());
                if (tct.getKey().startsWith("multiplayer.disconnect.outdated_server")) {
                    return new net.minecraft.network.login.server.SPacketDisconnect(new TextComponentString(CustomMessages.OUTDATED_SERVER));
                }
                if (tct.getKey().startsWith("multiplayer.disconnect.outdated_client")) {
                    return new net.minecraft.network.login.server.SPacketDisconnect(new TextComponentString(CustomMessages.OUTDATED_CLIENT));
                }
            }

        }
        if (packetIn instanceof net.minecraft.network.play.server.SPacketDisconnect) {
            final net.minecraft.network.play.server.SPacketDisconnect p = (net.minecraft.network.play.server.SPacketDisconnect) packetIn;
            if (ObfuscationReflectionHelper.getPrivateValue(net.minecraft.network.play.server.SPacketDisconnect.class, p, "reason", "field_149167_a", "a") instanceof TextComponentTranslation) {
                final TextComponentTranslation tct = ObfuscationReflectionHelper.getPrivateValue(net.minecraft.network.play.server.SPacketDisconnect.class, p, "reason", "field_149167_a", "a");
                System.out.println(tct.getKey());
                if (tct.getKey().startsWith("multiplayer.disconnect.server_shutdown")) {
                    return new net.minecraft.network.play.server.SPacketDisconnect(new TextComponentString(CustomMessages.STOP_MSG));
                }
            }
        }
        return packetIn;
    }

    @Shadow
    protected abstract void dispatchPacket(Packet<?> inPacket, @Nullable GenericFutureListener<? extends Future<? super Void>>[] futureListeners);

    @Redirect(method = "sendPacket(Lnet/minecraft/network/Packet;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/NetworkManager;dispatchPacket(Lnet/minecraft/network/Packet;[Lio/netty/util/concurrent/GenericFutureListener;)V"))
    private void send(NetworkManager networkManager, Packet<?> packetIn, GenericFutureListener<? extends Future<? super Void>>[] futureListeners) {
        packetIn = modifyPacket(packetIn);
        dispatchPacket(packetIn, futureListeners);
    }

    @Redirect(method = "sendPacket(Lnet/minecraft/network/Packet;Lio/netty/util/concurrent/GenericFutureListener;[Lio/netty/util/concurrent/GenericFutureListener;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/NetworkManager;dispatchPacket(Lnet/minecraft/network/Packet;[Lio/netty/util/concurrent/GenericFutureListener;)V"))
    private void send2(NetworkManager networkManager, Packet<?> packetIn, GenericFutureListener<? extends Future<? super Void>>[] futureListeners) {
        packetIn = modifyPacket(packetIn);
        dispatchPacket(packetIn, futureListeners);
    }
}
