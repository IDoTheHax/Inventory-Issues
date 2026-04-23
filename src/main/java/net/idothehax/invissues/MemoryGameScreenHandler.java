package net.idothehax.invissues;

import net.idothehax.invissues.registry.ModComponents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class MemoryGameScreenHandler extends ScreenHandler {
    private final Inventory inventory;
    private final PlayerEntity player;

    public MemoryGameScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(27));
    }

    public MemoryGameScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory) {
        super(ScreenHandlerType.GENERIC_9X3, syncId);
        this.inventory = inventory;
        this.player = playerInventory.player;

        Invissues.LOGGER.info("MemoryGameScreenHandler created for {}", player.getName().getString());

        checkSize(inventory, 27);
        inventory.onOpen(playerInventory.player);

        // Add the 27 memory game slots (3 rows of 9)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new MemoryGameSlot(inventory, col + row * 9, 8 + col * 18, 18 + row * 18));
            }
        }

        // Add player inventory (but we'll block interactions with it)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Add player hotbar
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        // Only handle clicks on the game board (slots 0-26)
        if (slotIndex < 0 || slotIndex >= 27) {
            return; // Ignore clicks outside the game board
        }

        if (!(player instanceof ServerPlayerEntity serverPlayer)) {
            return;
        }

        var data = ModComponents.SENTIENT_DATA.get(serverPlayer);

        // Only allow clicks during the guessing phase
        if (data.getMemoryGameState() != 2) {
            return;
        }

        // Check if they clicked the correct slot
        int targetSlot = data.getMemoryTargetSlot();

        if (slotIndex == targetSlot) {
            // WINNER!
            data.setMemoryGameState(0); // End the game
            data.modifyMood(10); // Reward for winning

            serverPlayer.closeHandledScreen();
            serverPlayer.sendMessage(Text.literal("✓ CORRECT! +10 MOOD").formatted(Formatting.GREEN, Formatting.BOLD), false);
            serverPlayer.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 1.0f, 1.0f);

            Invissues.LOGGER.info("{} won the memory game!", serverPlayer.getName().getString());
        } else {
            // WRONG!
            data.setMemoryGameState(0); // End the game
            data.modifyMood(-5); // Penalty for losing

            serverPlayer.closeHandledScreen();
            serverPlayer.sendMessage(Text.literal("✗ WRONG! It was in slot " + (targetSlot + 1) + ". -5 MOOD").formatted(Formatting.RED, Formatting.BOLD), false);
            serverPlayer.playSound(SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.PLAYERS, 1.0f, 0.8f);

            // Scramble their inventory as punishment
            ActionDispatcher.executeScramble(serverPlayer, true);

            Invissues.LOGGER.info("{} failed the memory game. Target was slot {}, they clicked {}",
                    serverPlayer.getName().getString(), targetSlot, slotIndex);
        }
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        // Completely block shift-clicking
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);

        // If they close the screen during the game, they lose
        if (player instanceof ServerPlayerEntity serverPlayer) {
            var data = ModComponents.SENTIENT_DATA.get(serverPlayer);
            if (data.getMemoryGameState() > 0) {
                data.setMemoryGameState(0);
                data.modifyMood(-10); // Harsh penalty for rage-quitting
                serverPlayer.sendMessage(Text.literal("You closed the memory game! -10 MOOD").formatted(Formatting.DARK_RED), false);
                ActionDispatcher.executeScramble(serverPlayer, true);
            }
        }
    }

    // Custom slot that blocks all item removal
    private static class MemoryGameSlot extends Slot {
        public MemoryGameSlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            return false; // Can't put items in
        }

        @Override
        public boolean canTakeItems(PlayerEntity playerEntity) {
            return false; // Can't take items out
        }

        @Override
        public boolean canTakePartial(PlayerEntity player) {
            return false;
        }
    }
}