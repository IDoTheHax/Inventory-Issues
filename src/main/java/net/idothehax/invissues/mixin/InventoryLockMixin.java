package net.idothehax.invissues.mixin;

import net.idothehax.invissues.ActionDispatcher;
import net.idothehax.invissues.registry.ModComponents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenHandler.class)
public class InventoryLockMixin {

    @Inject(method = "onSlotClick", at = @At("HEAD"), cancellable = true)
    private void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity playerEntity, CallbackInfo ci) {
        if (!(playerEntity instanceof ServerPlayerEntity player)) return;
        var data = ModComponents.SENTIENT_DATA.get(player);

        // STATE 1: Shuffling Animation (No touching anything)
        if (data.getMemoryGameState() == 1) {
            ci.cancel();
            return;
        }

        // STATE 2: Guessing Phase
        if (data.getMemoryGameState() == 2) {
            ci.cancel(); // Always cancel so they don't pick up the glass/items

            // Did they click the top board? (Slots 0-26 in a 9x3 generic container)
            if (slotIndex >= 0 && slotIndex < 27) {
                if (slotIndex == data.getMemoryTargetSlot()) {
                    // WIN
                    player.sendMessage(Text.literal("Correct. You live another day.").formatted(Formatting.GREEN), true);
                    data.modifyMood(25);
                } else {
                    // LOSE
                    player.sendMessage(Text.literal("WRONG. SUFFER.").formatted(Formatting.DARK_RED, Formatting.BOLD), false);
                    data.modifyMood(-30);
                    ActionDispatcher.executeTantrumDrop(player);
                }

                // End game and close UI
                data.setMemoryGameState(0);
                player.closeHandledScreen();
            }
            return;
        }

        // Standard Lock Fallback (for other routines)
        if (data.isInventoryLocked()) {
            ci.cancel();
            player.currentScreenHandler.sendContentUpdates();
        }
    }
}