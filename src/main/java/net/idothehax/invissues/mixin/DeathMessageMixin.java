package net.idothehax.invissues.mixin;

import net.idothehax.invissues.registry.ModComponents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageTracker;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(DamageTracker.class)
public class DeathMessageMixin {

    // Shadow allows us to access the entity that this DamageTracker belongs to
    @Shadow @Final private LivingEntity entity;

    @Inject(method = "getDeathMessage", at = @At("HEAD"), cancellable = true)
    private void onGetDeathMessage(CallbackInfoReturnable<Text> cir) {
        // Check if the entity dying is a Player on the server
        if (this.entity instanceof ServerPlayerEntity player) {

            // Check their inventory mood
            int mood = ModComponents.SENTIENT_DATA.get(player).getMood();

            // If they die while the inventory is at absolute zero, the inventory claims the kill.
            // Even if a zombie hits them for the final blow, if mood is 0, this message plays!
            if (mood <= 0) {
                String playerName = player.getName().getString();

                Text customDeathMessage = Text.literal(playerName + " was consumed by their own inventory.")
                        .formatted(Formatting.DARK_PURPLE, Formatting.ITALIC);

                cir.setReturnValue(customDeathMessage);
            }
        }
    }
}