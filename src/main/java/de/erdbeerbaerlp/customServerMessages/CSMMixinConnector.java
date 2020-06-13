package de.erdbeerbaerlp.customServerMessages;

import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.connect.IMixinConnector;

public class CSMMixinConnector implements IMixinConnector {
    @Override
    public void connect() {
        Mixins.addConfiguration("mixins.customservermsgs.json");
    }
}
