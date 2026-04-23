package net.idothehax.invissues.client.mixin;

import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DeathScreen.class)
public abstract class DeathScreenMixin extends Screen {

    @Shadow private Text scoreText;
    @Shadow @Final private Text message; // This holds the death cause

    protected DeathScreenMixin(Text title) {
        super(title);
    }

    // --- SCRAMBLE THE BUTTONS AND SCORE ---
    @Inject(method = "init", at = @At("TAIL"))
    private void onInitObfuscate(CallbackInfo ci) {
        // Did the inventory kill them?
        if (this.message != null && this.message.getString().contains("consumed by their own inventory")) {

            // 1. Obfuscate the Score text
            if (this.scoreText != null) {
                this.scoreText = Text.literal("SCORE: NULL").formatted(Formatting.OBFUSCATED, Formatting.DARK_PURPLE);
            }

            // 2. Obfuscate all clickable buttons (Respawn, Title Screen)
            for (net.minecraft.client.gui.Element element : this.children()) {
                if (element instanceof ButtonWidget button) {
                    // Takes whatever the button says ("Respawn") and turns it into scrambled purple text
                    button.setMessage(Text.literal(button.getMessage().getString())
                            .formatted(Formatting.OBFUSCATED, Formatting.DARK_PURPLE, Formatting.BOLD));
                }
            }
        }
    }

    // --- REPLACE THE VANILLA TITLE NATIVELY ---
    @ModifyArg(
            method = "render",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawCenteredTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;III)V"),
            index = 1
    )
    private Text obfuscateTitleText(Text originalText) {
        // Check if the inventory killed us
        if (this.message != null && this.message.getString().contains("consumed by their own inventory")) {

            // If the text it's trying to draw is the main screen title ("You died!")
            if (originalText.equals(this.title)) {
                // Swap it with corrupted text before it hits the screen!
                return Text.literal("FATAL SYSTEM ERROR").formatted(Formatting.OBFUSCATED, Formatting.DARK_RED, Formatting.BOLD);
            }
        }
        return originalText; // Otherwise, draw normal text (like the death cause)
    }
}