package net.idothehax.invissues.client.mixin;

import net.idothehax.invissues.registry.ModComponents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InventoryScreen.class)
public class InventoryGlitchMixin {

    // --- SHAKE THE SCREEN ---
    @Inject(method = "render", at = @At("HEAD"))
    private void shakeInventoryHead(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        int mood = ModComponents.SENTIENT_DATA.get(client.player).getMood();
        if (mood < 10) {
            context.getMatrices().push(); // Save the normal rendering state

            // Calculate a violent random shift
            float intensity = (mood <= 0) ? 20f : 6f; // Shakes way harder at 0 mood
            float shiftX = (client.player.getRandom().nextFloat() - 0.5f) * intensity;
            float shiftY = (client.player.getRandom().nextFloat() - 0.5f) * intensity;

            // Move the entire GUI matrix
            context.getMatrices().translate(shiftX, shiftY, 0);
        }
    }

    // --- FLASH CORRUPTED PURPLE BOXES ---
    @Inject(method = "render", at = @At("RETURN"))
    private void flashInventoryReturn(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        int mood = ModComponents.SENTIENT_DATA.get(client.player).getMood();
        if (mood < 10) {
            context.getMatrices().pop(); // Restore the rendering state to normal

            // 40% chance every single frame to draw a glitch artifact
            if (client.player.getRandom().nextFloat() < 0.4f) {
                int screenWidth = client.getWindow().getScaledWidth();
                int screenHeight = client.getWindow().getScaledHeight();

                // Generate random coordinates for a "corrupted" rectangle
                int rx = client.player.getRandom().nextInt(screenWidth);
                int ry = client.player.getRandom().nextInt(screenHeight);
                int rw = client.player.getRandom().nextInt(screenWidth / 3) + 20;
                int rh = client.player.getRandom().nextInt(50) + 10;

                // Bright, semi-transparent magenta/purple overlay
                context.fill(rx, ry, rx + rw, ry + rh, 0x60B000B0);
            }
        }
    }
}