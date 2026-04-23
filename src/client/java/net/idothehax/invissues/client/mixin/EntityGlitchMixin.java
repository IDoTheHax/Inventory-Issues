package net.idothehax.invissues.client.mixin;

import net.idothehax.invissues.registry.ModComponents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public class EntityGlitchMixin {

    // --- CORRUPT THE MATRIX ---
    @Inject(method = "render", at = @At("HEAD"))
    private <E extends Entity> void glitchRenderHead(E entity, double x, double y, double z, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        // Only target OUR player, and only when the inventory is open
        if (entity instanceof ClientPlayerEntity) {
            MinecraftClient client = MinecraftClient.getInstance();

            if (client.currentScreen instanceof InventoryScreen) {
                int mood = ModComponents.SENTIENT_DATA.get(client.player).getMood();

                if (mood < 10) {
                    matrices.push(); // Save the normal rendering state!

                    // The lower the mood, the more extreme the glitching
                    float intensity = (mood <= 0) ? 0.8f : 0.25f;

                    // 1. Spastic Twitching (Translating)
                    float tx = (client.player.getRandom().nextFloat() - 0.5f) * intensity;
                    float ty = (client.player.getRandom().nextFloat() - 0.5f) * intensity;
                    float tz = (client.player.getRandom().nextFloat() - 0.5f) * intensity;
                    matrices.translate(tx, ty, tz);

                    // 2. Horrific Stretching (Scaling the bones)
                    if (client.player.getRandom().nextFloat() < 0.6f) {
                        float sx = 1.0f + (client.player.getRandom().nextFloat() - 0.5f) * intensity * 2;
                        float sy = 1.0f + (client.player.getRandom().nextFloat() - 0.5f) * intensity * 3; // Heavy vertical stretch
                        float sz = 1.0f + (client.player.getRandom().nextFloat() - 0.5f) * intensity * 2;
                        matrices.scale(sx, sy, sz);
                    }

                    // 3. Unnatural Body Twisting (Rotation)
                    if (client.player.getRandom().nextFloat() < 0.4f) {
                        float rot = (client.player.getRandom().nextFloat() - 0.5f) * intensity * 90f; // Can snap up to 90 degrees!
                        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rot));
                        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rot * 0.5f));
                    }
                }
            }
        }
    }

    // --- RESTORE THE MATRIX ---
    @Inject(method = "render", at = @At("RETURN"))
    private <E extends Entity> void glitchRenderReturn(E entity, double x, double y, double z, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (entity instanceof ClientPlayerEntity) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.currentScreen instanceof InventoryScreen) {
                int mood = ModComponents.SENTIENT_DATA.get(client.player).getMood();
                if (mood < 10) {
                    matrices.pop(); // Important! Restore the state so we don't break Minecraft rendering.
                }
            }
        }
    }
}