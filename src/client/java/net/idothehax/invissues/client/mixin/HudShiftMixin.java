package net.idothehax.invissues.client.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.entity.JumpingMount;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public class HudShiftMixin {

    // Move everything UP by 12 pixels
    private static final float SHIFT = -8f;

    // --- 1. SHIFT HEARTS, FOOD, AND ARMOR ---
    @Inject(method = "renderStatusBars", at = @At("HEAD"))
    private void shiftStatusBarsUp(DrawContext context, CallbackInfo ci) {
        context.getMatrices().push();
        context.getMatrices().translate(0, SHIFT, 0);
    }

    @Inject(method = "renderStatusBars", at = @At("RETURN"))
    private void shiftStatusBarsDown(DrawContext context, CallbackInfo ci) {
        context.getMatrices().pop();
    }

    // --- 2. SHIFT XP BAR ---
    @Inject(method = "renderExperienceBar", at = @At("HEAD"))
    private void shiftExpBarUp(DrawContext context, int x, CallbackInfo ci) {
        context.getMatrices().push();
        context.getMatrices().translate(0, SHIFT, 0);
    }

    @Inject(method = "renderExperienceBar", at = @At("RETURN"))
    private void shiftExpBarDown(DrawContext context, int x, CallbackInfo ci) {
        context.getMatrices().pop();
    }

    // --- 3. SHIFT HORSE HEALTH (Just in case!) ---
    @Inject(method = "renderMountHealth", at = @At("HEAD"))
    private void shiftMountHealthUp(DrawContext context, CallbackInfo ci) {
        context.getMatrices().push();
        context.getMatrices().translate(0, SHIFT, 0);
    }

    @Inject(method = "renderMountHealth", at = @At("RETURN"))
    private void shiftMountHealthDown(DrawContext context, CallbackInfo ci) {
        context.getMatrices().pop();
    }

    // --- 4. SHIFT HORSE JUMP BAR ---
    @Inject(method = "renderMountJumpBar", at = @At("HEAD"))
    private void shiftJumpBarUp(JumpingMount mount, DrawContext context, int x, CallbackInfo ci) {
        context.getMatrices().push();
        context.getMatrices().translate(0, SHIFT, 0);
    }

    @Inject(method = "renderMountJumpBar", at = @At("RETURN"))
    private void shiftJumpBarDown(JumpingMount mount, DrawContext context, int x, CallbackInfo ci) {
        context.getMatrices().pop();
    }
}