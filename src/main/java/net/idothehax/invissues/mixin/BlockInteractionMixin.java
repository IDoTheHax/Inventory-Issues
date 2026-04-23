package net.idothehax.invissues.mixin;

import net.idothehax.invissues.ActionDispatcher;
import net.idothehax.invissues.registry.ModComponents;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerInteractionManager.class)
public class BlockInteractionMixin {

    @Shadow @Final protected ServerPlayerEntity player;

    @Inject(method = "interactBlock", at = @At("HEAD"), cancellable = true)
    private void onInteractBlock(ServerPlayerEntity player, World world, ItemStack stack, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir) {
        if (!world.isClient && hand == Hand.MAIN_HAND) {
            // --- THE FIX: Only trigger on actual containers/interactive blocks ---
            var blockState = world.getBlockState(hitResult.getBlockPos());

            // If the block isn't a container (like a chest) and isn't interactive (like a lever)
            // we skip the roulette so it doesn't feel buggy.
            if (blockState.getBlock() instanceof net.minecraft.block.BlockWithEntity ||
                    blockState.getBlock() instanceof net.minecraft.block.CraftingTableBlock ||
                    blockState.getBlock() instanceof net.minecraft.block.AnvilBlock) {

                int mood = ModComponents.SENTIENT_DATA.get(player).getMood();

                if (mood < 30 && player.getRandom().nextFloat() < 0.4f) {
                    ActionDispatcher.executeInterfaceRoulette(player, hitResult.getBlockPos());
                    cir.setReturnValue(ActionResult.SUCCESS);
                }
            }
        }
    }
}