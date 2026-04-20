package net.idothehax.invissues.mixin;

import net.idothehax.invissues.ActionDispatcher;
import net.idothehax.invissues.registry.ModComponents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public class BlockInteractionMixin {

    // Injecting into interactBlock which contains the HitResult we need
    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void onBlockInteract(Entity entity, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        PlayerEntity player = (PlayerEntity) (Object) this;

        // Ensure we are on server and using main hand
        if (!player.getWorld().isClient && hand == Hand.MAIN_HAND && player instanceof ServerPlayerEntity serverPlayer) {
            int mood = ModComponents.SENTIENT_DATA.get(serverPlayer).getMood();

            // 30% chance to hijack if mood is low
            if (mood < 25 && serverPlayer.getRandom().nextFloat() < 0.3f) {
                // If the player is looking at a block, use its position
                ActionDispatcher.executeInterfaceRoulette(serverPlayer, serverPlayer.getBlockPos());
                cir.setReturnValue(ActionResult.SUCCESS);
            }
        }
    }
}