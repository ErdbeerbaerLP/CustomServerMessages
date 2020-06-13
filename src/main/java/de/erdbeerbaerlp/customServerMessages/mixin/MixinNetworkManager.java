package de.erdbeerbaerlp.customServerMessages.mixin;

import de.erdbeerbaerlp.customServerMessages.CustomMessagesConfig;
import de.erdbeerbaerlp.customServerMessages.CustomServerMessagesMod;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import net.minecraft.network.IPacket;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SChatPacket;
import net.minecraft.network.play.server.SDisconnectPacket;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import javax.annotation.Nullable;

@Mixin(NetworkManager.class)
public abstract class MixinNetworkManager {

    private static IPacket<?> modifyPacket(IPacket<?> packetIn) {
        if (packetIn instanceof SChatPacket) {
            final SChatPacket msg = (SChatPacket) packetIn;
            final ITextComponent com = ObfuscationReflectionHelper.getPrivateValue(SChatPacket.class, msg, "field_148919_a");
            if (com instanceof TranslationTextComponent) {
                final TranslationTextComponent tct = (TranslationTextComponent) com.setStyle(new Style());
                final String player = tct.getFormattedText().split(" ")[0];
                boolean timeout = CustomServerMessagesMod.playersTimedOut.contains(player);
                if (timeout) CustomServerMessagesMod.playersTimedOut.remove(player);
                if (tct.getKey().startsWith("multiplayer.player.left")) {
                    final StringTextComponent leaveMsg = new StringTextComponent((timeout ? CustomMessagesConfig.instance().messages.timeoutLeaveMessage : CustomMessagesConfig.instance().messages.leaveMessage).replace("%player%", player));
                    leaveMsg.setStyle(tct.getStyle());
                    return new SChatPacket(leaveMsg, msg.getType());
                }
                if (tct.getKey().startsWith("multiplayer.player.joined")) {
                    final StringTextComponent joinMsg = new StringTextComponent(CustomMessagesConfig.instance().messages.joinMessage.replace("%player%", player));
                    joinMsg.setStyle(tct.getStyle());
                    return new SChatPacket(joinMsg, msg.getType());
                }
            }
        }
        if (packetIn instanceof net.minecraft.network.login.server.SDisconnectLoginPacket) {
            final net.minecraft.network.login.server.SDisconnectLoginPacket p = (net.minecraft.network.login.server.SDisconnectLoginPacket) packetIn;
            if (ObfuscationReflectionHelper.getPrivateValue(net.minecraft.network.login.server.SDisconnectLoginPacket.class, p, "field_149605_a") instanceof TranslationTextComponent) {
                final TranslationTextComponent tct = ObfuscationReflectionHelper.getPrivateValue(net.minecraft.network.login.server.SDisconnectLoginPacket.class, p, "field_149605_a");
                if (tct.getKey().startsWith("multiplayer.disconnect.outdated_server")) {
                    return new net.minecraft.network.login.server.SDisconnectLoginPacket(new StringTextComponent(CustomMessagesConfig.instance().messages.outdatedServerKick));
                }
                if (tct.getKey().startsWith("multiplayer.disconnect.outdated_client")) {
                    return new net.minecraft.network.login.server.SDisconnectLoginPacket(new StringTextComponent(CustomMessagesConfig.instance().messages.outdatedClientKick));
                }
            }

        }
        if (packetIn instanceof net.minecraft.network.play.server.SDisconnectPacket) {
            final net.minecraft.network.play.server.SDisconnectPacket p = (net.minecraft.network.play.server.SDisconnectPacket) packetIn;
            if (ObfuscationReflectionHelper.getPrivateValue(net.minecraft.network.play.server.SDisconnectPacket.class, p, "field_149167_a") instanceof TranslationTextComponent) {
                final TranslationTextComponent tct = ObfuscationReflectionHelper.getPrivateValue(net.minecraft.network.play.server.SDisconnectPacket.class, p, "field_149167_a");
                if (tct.getKey().startsWith("multiplayer.disconnect.server_shutdown")) {
                    return new net.minecraft.network.play.server.SDisconnectPacket(new StringTextComponent(CustomMessagesConfig.instance().messages.serverStoppedMessage));
                }
                if (tct.getKey().startsWith("disconnect.spam")) {
                    return new SDisconnectPacket(new StringTextComponent(CustomMessagesConfig.instance().messages.spamKick));
                }
                if (tct.getKey().startsWith("multiplayer.disconnect.idling")) {
                    return new SDisconnectPacket(new StringTextComponent(CustomMessagesConfig.instance().messages.idleTimeoutKick));
                }
            }
        }
        return packetIn;
    }

    @Shadow
    protected abstract void dispatchPacket(IPacket<?> inPacket, @Nullable GenericFutureListener<? extends Future<? super Void>> futureListeners);

    @Redirect(method = "sendPacket(Lnet/minecraft/network/IPacket;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/NetworkManager;sendPacket(Lnet/minecraft/network/IPacket;Lio/netty/util/concurrent/GenericFutureListener;)V"))
    private void send(NetworkManager networkManager, IPacket<?> packetIn, GenericFutureListener<? extends Future<? super Void>> p_201058_2_) {
        packetIn = modifyPacket(packetIn);
        dispatchPacket(packetIn, p_201058_2_);
    }

    @Redirect(method = "sendPacket(Lnet/minecraft/network/IPacket;Lio/netty/util/concurrent/GenericFutureListener;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/NetworkManager;dispatchPacket(Lnet/minecraft/network/IPacket;Lio/netty/util/concurrent/GenericFutureListener;)V"))
    private void send2(NetworkManager networkManager, IPacket<?> packetIn, GenericFutureListener<? extends Future<? super Void>> futureListeners) {
        packetIn = modifyPacket(packetIn);
        dispatchPacket(packetIn, futureListeners);
    }
}
