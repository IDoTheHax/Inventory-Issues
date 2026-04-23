package net.idothehax.invissues.client.mixin;

import net.idothehax.invissues.MutableSlot;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Slot.class)
public class SlotMixin implements MutableSlot {

    // @Mutable strips the 'final' modifier from these variables!
    @Shadow @Final @Mutable public int x;
    @Shadow @Final @Mutable public int y;

    @Override
    public void inventoryIssues$setX(int newX) {
        this.x = newX;
    }

    @Override
    public void inventoryIssues$setY(int newY) {
        this.y = newY;
    }
}