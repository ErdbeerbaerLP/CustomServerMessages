package de.erdbeerbaerlp.customServerMessages.mixin;

import de.erdbeerbaerlp.customServerMessages.CustomServerMessagesMod;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.play.ServerPlayNetHandler;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin used to detect player timeouts
 */
@Mixin(value = ServerPlayNetHandler.class, priority = 1001)
public abstract class MixinServerPlayNetHandler {

    @Shadow
    public ServerPlayerEntity player;

    @Inject(method = "disconnect", at = @At("HEAD"))
    private void onDisconnect(final ITextComponent textComponent, CallbackInfo ci) {
        if (textComponent.equals(new TranslationTextComponent("disconnect.timeout")))
            CustomServerMessagesMod.playersTimedOut.add(this.player.getName().getUnformattedComponentText());
    }


}