package net.idothehax.invissues;

import net.idothehax.invissues.registry.ModComponents;
import net.minecraft.item.BlockItem;
import net.minecraft.item.FoodComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolItem;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class SortChallengeManager {

    /**
     * Starts the 15-second heart attack.
     */
    public static void startChallenge(ServerPlayerEntity player) {
        ModComponents.SENTIENT_DATA.get(player).setSortTimer(300); // 15 seconds

        player.sendMessage(Text.literal("--- INVENTORY REORGANIZATION PROTOCOL ---").formatted(Formatting.GOLD, Formatting.BOLD), false);
        player.sendMessage(Text.literal("Left 3 Cols: ").append(Text.literal("BLOCKS").formatted(Formatting.RED)));
        player.sendMessage(Text.literal("Middle 3 Cols: ").append(Text.literal("TOOLS/ARMOR").formatted(Formatting.BLUE)));
        player.sendMessage(Text.literal("Right 3 Cols: ").append(Text.literal("MISC/FOOD").formatted(Formatting.GREEN)));
        player.sendMessage(Text.literal("YOU HAVE 15 SECONDS.").formatted(Formatting.DARK_RED, Formatting.UNDERLINE), false);

        player.playSound(SoundEvents.ENTITY_ELDER_GUARDIAN_CURSE, SoundCategory.PLAYERS, 1.0f, 1.0f);
    }

    /**
     * Ticks the timer and performs the purge at 0.
     */
    public static void tick(ServerPlayerEntity player) {
        var data = ModComponents.SENTIENT_DATA.get(player);
        int timer = data.getSortTimer();

        if (timer <= 0) return;

        data.setSortTimer(timer - 1);

        if (timer == 1) {
            performPurge(player);
        } else if (timer % 20 == 0 && timer <= 100) {
            // Beep every second for the last 5 seconds
            player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BIT.value(), SoundCategory.PLAYERS, 1.0f, 2.0f);
        }
    }

    private static void performPurge(ServerPlayerEntity player) {
        var inv = player.getInventory();
        int purgedCount = 0;

        for (int i = 0; i < 36; i++) {
            ItemStack stack = inv.getStack(i);
            if (stack.isEmpty()) continue;

            int column = i % 9;
            boolean isCorrect = false;

            if (column < 3) { // ZONE 1: BLOCKS
                isCorrect = stack.getItem() instanceof BlockItem;
            } else if (column < 6) { // ZONE 2: TOOLS/ARMOR
                isCorrect = stack.getItem() instanceof ToolItem || stack.getItem() instanceof net.minecraft.item.ArmorItem;
            } else { // ZONE 3: MISC/FOOD
                isCorrect = stack.getItem().isFood() || (! (stack.getItem() instanceof BlockItem) && ! (stack.getItem() instanceof ToolItem));
            }

            if (!isCorrect) {
                inv.setStack(i, ItemStack.EMPTY);
                purgedCount++;
            }
        }

        if (purgedCount > 0) {
            player.sendMessage(Text.literal("PROTOCOL COMPLETE. ").append(Text.literal(purgedCount + " items purged.").formatted(Formatting.RED)), false);
            player.playSound(SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 0.5f, 1.5f);
        } else {
            player.sendMessage(Text.literal("PROTOCOL COMPLETE. Inventory satisfies requirements.").formatted(Formatting.GREEN), false);
            ModComponents.SENTIENT_DATA.get(player).modifyMood(20);
            player.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1.0f, 1.0f);
        }
        player.currentScreenHandler.sendContentUpdates();
    }
}