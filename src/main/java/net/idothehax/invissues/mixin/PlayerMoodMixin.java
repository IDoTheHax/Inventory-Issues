package net.idothehax.invissues.mixin;

import net.idothehax.invissues.ActionDispatcher;
import net.idothehax.invissues.MoodManager;
import net.idothehax.invissues.registry.ModComponents;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public class PlayerMoodMixin {

    @Inject(method = "applyDamage", at = @At("HEAD"))
    private void onTakeDamage(DamageSource source, float amount, CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;

        if (player instanceof ServerPlayerEntity serverPlayer) {
            // 1. Regular Mood Drop logic
            MoodManager.onDamaged(serverPlayer, source, amount);

            // 2. THE SABOTAGE (Option C)
            // Check if mood is low and health is critical (< 3 hearts)
            if (serverPlayer.getHealth() < 6.0f && ModComponents.SENTIENT_DATA.get(serverPlayer).getMood() < 40) {
                // 50% chance to sabotage when you're already struggling
                if (serverPlayer.getRandom().nextFloat() < 0.5f) {
                    ActionDispatcher.executeLowHealthSabotage(serverPlayer);
                }
            }
        }
    }

    @Inject(method = "eatFood", at = @At("HEAD"))
    private void onEatFood(World world, ItemStack stack, CallbackInfoReturnable<ItemStack> cir) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (player instanceof ServerPlayerEntity serverPlayer) {
            MoodManager.onEat(serverPlayer);
        }
    }

    @Inject(method = "wakeUp(ZZ)V", at = @At("HEAD"))
    private void onWakeUp(boolean skipSleepTimer, boolean updateSleepingPlayers, CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        // Only reward them if they actually slept through the night (skipSleepTimer is usually true if the night passes)
        if (player instanceof ServerPlayerEntity serverPlayer && skipSleepTimer) {
            MoodManager.onSleep(serverPlayer);
        }
    }
}