package de.erdbeerbaerlp.customServerMessages.mixin;

import de.erdbeerbaerlp.customServerMessages.Configuration;
import de.erdbeerbaerlp.customServerMessages.CustomMessageMod;
import net.minecraft.server.Main;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Mixin(Main.class)
public class MixinMain {
    @Inject(method ="main", at=@At(value = "INVOKE", target="Lnet/minecraftforge/server/loading/ServerModLoader;load()V", remap = false),remap = false)
    private static void bootstrap(String[] p_129699_, CallbackInfo ci){
        CustomMessageMod.serverLaunched = Instant.now();
        CustomMessageMod.earlyStart();
        Thread r = new Thread(() -> {
            if (Configuration.instance().dev.autoReloadConfig != 0)
                while (true) {
                    if (Configuration.instance().dev.autoReloadConfig != 0) {
                        try {
                            Thread.sleep(TimeUnit.SECONDS.toMillis(Configuration.instance().dev.autoReloadConfig));
                            Configuration.instance().loadConfig();
                        } catch (InterruptedException e) {
                            System.err.println("Error auto-reloading config: InterruptedException");
                        }
                    }
                }
        });
        r.setDaemon(true);
        r.setPriority(Thread.MAX_PRIORITY);
        r.start();
        if(Configuration.instance().dev.delayServerStart) {
            try {
                TimeUnit.SECONDS.sleep(9999);
            } catch (InterruptedException e) {
                System.err.println("Got interrupted while delaying server");
            }
        }
    }
}
