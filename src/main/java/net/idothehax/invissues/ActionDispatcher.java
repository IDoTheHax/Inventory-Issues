package net.idothehax.invissues;

import net.idothehax.invissues.registry.ModComponents;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

public class ActionDispatcher {

    public static void triggerAction(ServerPlayerEntity player, int mood) {
        var data = ModComponents.SENTIENT_DATA.get(player);

        // SAFETY LOCK 1: Do not trigger anything new if a minigame is actively running
        if (data.getMemoryGameState() > 0 || data.getSortTimer() > 0) {
            return;
        }

        // SAFETY LOCK 2: Do not trigger organized minigames (Memory/Sort) if in the DVD/Horror phase
        if (mood < 20) {
            // The inventory is too angry to play games. Just throw an item instead.
            ActionDispatcher.executeTantrumDrop(player);
            return;
        }

        int roll = player.getRandom().nextInt(100);

        if (mood <= 30) {
            // --- HIGH CHAOS ---
            Invissues.LOGGER.info("High chaos mode, roll: {}", roll);
            // We prioritize the unique games over the basic scramble
            if (roll < 20) {
                Invissues.LOGGER.info("Triggering memory game for {}", player.getName().getString());
                executeMemoryGame(player);
            } else if (roll < 40) {
                if (ModComponents.SENTIENT_DATA.get(player).getSortTimer() <= 0) {
                    SortChallengeManager.startChallenge(player);
                } else {
                    executeTantrumDrop(player); // Failsafe
                }
            } else if (roll < 60) {
                executeArmorStrip(player);
            } else {
                executeScramble(player, true);
            }
        } else if (mood <= 70) {
            // --- ANNOYED / NEUTRAL ---
            if (roll < 40) {
                executeScramble(player, false); // Gentle scramble
            } else if (roll < 80) {
                // Check if it's already hungry to prevent spamming the message
                if (ModComponents.SENTIENT_DATA.get(player).getHungerTimer() <= 0) {
                    HungerManager.triggerHunger(player);
                }
            } else {
                Invissues.LOGGER.info("Inventory grumbled but did nothing.");
            }
        } else {
            // --- HIGH TRUST (Helpful) ---
            if (roll < 30) {
                // TODO: executeOrganize(player);
                Invissues.LOGGER.info("Inventory is feeling helpful (Organization coming soon).");
            }
        }
    }

    // --- BEHAVIOR LOGIC ---

    /**
     * Swaps items between slots in the main inventory and hotbar (0-35).
     * @param violent If true, performs 6-10 swaps. If false, performs 3-5 swaps.
     */
    public static void executeScramble(ServerPlayerEntity player, boolean violent) {
        PlayerInventory inv = player.getInventory();

        // Gentle: 3-5 swaps. Violent: 6-10 swaps for absolute chaos.
        int swaps = violent ? (6 + player.getRandom().nextInt(5)) : (3 + player.getRandom().nextInt(3));

        // 0 to 35 covers both the hotbar (0-8) and the main inventory (9-35).
        int maxSlot = 36;

        for (int i = 0; i < swaps; i++) {
            int slot1 = player.getRandom().nextInt(maxSlot);
            int slot2 = player.getRandom().nextInt(maxSlot);

            if (slot1 == slot2) continue;

            // Perform the swap
            ItemStack temp = inv.getStack(slot1);
            inv.setStack(slot1, inv.getStack(slot2));
            inv.setStack(slot2, temp);
        }

        // Force the server to sync the new inventory layout to the client
        player.currentScreenHandler.sendContentUpdates();
        Invissues.LOGGER.info("Scrambled {}'s inventory. Violent mode: {}", player.getName().getString(), violent);
    }

    /**
     * Rips off the player's armor and offhand, tossing them into the main inventory.
     */
    private static void executeArmorStrip(ServerPlayerEntity player) {
        PlayerInventory inv = player.getInventory();
        boolean strippedSomething = false;

        // Slots 36-39 are Armor, 40 is Offhand
        for (int i = 36; i <= 40; i++) {
            ItemStack equipment = inv.getStack(i);

            if (!equipment.isEmpty()) {
                // Remove the armor from the player's body
                inv.setStack(i, ItemStack.EMPTY);

                // Try to put it in a normal inventory slot (0-35)
                if (!inv.insertStack(equipment)) {
                    // If the inventory is completely full, drop it on the ground!
                    player.dropItem(equipment, true, true);
                }
                strippedSomething = true;
            }
        }

        if (strippedSomething) {
            player.currentScreenHandler.sendContentUpdates();
            Invissues.LOGGER.info("Stripped armor from {}!", player.getName().getString());
        }
    }

    /**
     * Grabs a random item from the player's inventory and throws it on the ground.
     */
    public static void executeTantrumDrop(ServerPlayerEntity player) {
        PlayerInventory inv = player.getInventory();

        // Find a random non-empty slot in the hotbar/main inventory (0-35)
        for (int i = 0; i < 10; i++) {
            int targetSlot = player.getRandom().nextInt(36);
            ItemStack stack = inv.getStack(targetSlot);

            if (!stack.isEmpty()) {
                // Remove it from the inventory
                ItemStack thrownItem = inv.removeStack(targetSlot);

                // Drop it into the world (true = retain ownership, true = throw randomly)
                player.dropItem(thrownItem, true, true);

                // Sync to client
                player.currentScreenHandler.sendContentUpdates();
                Invissues.LOGGER.info("Threw {} out of {}'s inventory!", thrownItem.getItem().getName().getString(), player.getName().getString());
                break;
            }
        }
    }

    /**
     * The "Dick Move": Swaps essential gear for junk when the player is dying.
     */
    public static void executeLowHealthSabotage(ServerPlayerEntity player) {
        PlayerInventory inv = player.getInventory();

        // Target the Chestplate (38) or Main Hand (selectedSlot)
        int targetSlot = player.getRandom().nextBoolean() ? 38 : player.getInventory().selectedSlot;
        ItemStack currentGear = inv.getStack(targetSlot);

        if (currentGear.isEmpty()) return;

        // Find some "junk" in the inventory to swap it with
        int junkSlot = -1;
        for (int i = 0; i < 36; i++) {
            ItemStack s = inv.getStack(i);
            if (!s.isEmpty() && (s.getItem().equals(Items.DIRT) || s.getItem().equals(Items.ROTTEN_FLESH) || s.getItem().equals(Items.POISONOUS_POTATO))) {
                junkSlot = i;
                break;
            }
        }

        // If no junk found, just swap with a random empty-ish slot
        if (junkSlot == -1) junkSlot = player.getRandom().nextInt(36);

        // Perform the swap
        ItemStack junk = inv.getStack(junkSlot);
        inv.setStack(targetSlot, junk);
        inv.setStack(junkSlot, currentGear);

        player.currentScreenHandler.sendContentUpdates();
        player.sendMessage(Text.literal("Oops... your hands slipped!").formatted(Formatting.ITALIC, Formatting.DARK_GRAY), true);
        player.playSound(SoundEvents.ENTITY_WITCH_CELEBRATE, SoundCategory.PLAYERS, 1.0f, 1.2f);
    }

    /**
     * Sends a fake packet to the client to display a valuable item.
     */
    public static void executePhantomItem(ServerPlayerEntity player) {
        // Pick a fake item
        ItemStack fakeStack = new ItemStack(player.getRandom().nextBoolean() ? Items.DIAMOND_BLOCK : Items.TOTEM_OF_UNDYING, 64);

        // Find an empty slot
        int fakeSlot = -1;
        for (int i = 0; i < 36; i++) {
            if (player.getInventory().getStack(i).isEmpty()) {
                fakeSlot = i;
                break;
            }
        }

        if (fakeSlot != -1) {
            // Send the lie to the client
            // Slot IDs in the InventoryScreen start at 9 for the top-left of the main inv
            // Converting PlayerInventory index to ScreenHandler index:
            int screenSlot = (fakeSlot < 9) ? fakeSlot + 36 : fakeSlot;

            player.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket(
                    player.currentScreenHandler.syncId,
                    player.currentScreenHandler.nextRevision(),
                    screenSlot,
                    fakeStack
            ));

            Invissues.LOGGER.info("Gaslighting {} with fake items in slot {}", player.getName().getString(), screenSlot);
        }
    }

    /**
     * Trigger interface roulette where opening a block with GUI will give a random GUI that is NOT the intended GUI.
     */
    public static void executeInterfaceRoulette(ServerPlayerEntity player, BlockPos pos) {
        int roll = player.getRandom().nextInt(3);

        // We play a "glitch" sound to signal the inventory is messing with them
        player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BIT.value(), SoundCategory.PLAYERS, 1.0f, 0.5f);

        switch (roll) {
            case 0 -> // Force open a Crafting Table screen even if they clicked a Chest
                    player.openHandledScreen(new SimpleNamedScreenHandlerFactory((syncId, inv, p) ->
                            new CraftingScreenHandler(syncId, inv, ScreenHandlerContext.create(player.getWorld(), pos)),
                            Text.literal("Inventory's Crafting Table").formatted(Formatting.DARK_PURPLE)));

            case 1 -> // Force open their own Ender Chest
                    player.openHandledScreen(new SimpleNamedScreenHandlerFactory((syncId, inv, p) ->
                            GenericContainerScreenHandler.createGeneric9x3(syncId, inv, player.getEnderChestInventory()),
                            Text.literal("Inventory's Secret Stash")));

            case 2 -> // Force open a Beacon screen (confusing because it has no effects)
                    player.openHandledScreen(new SimpleNamedScreenHandlerFactory((syncId, inv, p) ->
                            new BeaconScreenHandler(syncId, inv),
                            Text.literal("???")));
        }

        player.sendMessage(Text.literal("The inventory didn't want you to open that.").formatted(Formatting.ITALIC, Formatting.GRAY), true);
    }

    /**
     * Triggers the memory game where the inventory briefly shows an item, shuffles it,
     * then the player must click the correct slot to win.
     */
    public static void executeMemoryGame(ServerPlayerEntity player) {
        Invissues.LOGGER.info("executeMemoryGame called for player: {}", player.getName().getString());

        var data = ModComponents.SENTIENT_DATA.get(player);

        // --- PHASE 0: THE WARNING ---
        player.sendMessage(Text.literal("⚠ MEMORY PROTOCOL IMMINENT ⚠").formatted(Formatting.DARK_RED, Formatting.BOLD), true);
        player.sendMessage(Text.literal("PREPARE YOURSELF. 5 SECONDS.").formatted(Formatting.RED), false);
        player.playSound(SoundEvents.ENTITY_WITHER_AMBIENT, SoundCategory.PLAYERS, 1.0f, 1.0f);

        Invissues.LOGGER.info("Memory game warning sent, scheduling phase 1...");

        // Wait 5 seconds before starting
        TaskScheduler.schedule(100, () -> {
            Invissues.LOGGER.info("Phase 1 executing - checking player state...");
            if (player.isDisconnected() || !player.isAlive()) {
                Invissues.LOGGER.warn("Player disconnected or dead, aborting memory game");
                return;
            }

            Invissues.LOGGER.info("Setting up memory game board...");

            // --- PHASE 1: SETUP ---
            data.setMemoryGameState(1); // Lock the game (state 1 = shuffling)

            // Create the game board with glass panes
            SimpleInventory gameBoard = new SimpleInventory(27);
            ItemStack glass = new ItemStack(net.minecraft.item.Items.GRAY_STAINED_GLASS_PANE);
            glass.setCustomName(Text.literal("???").formatted(Formatting.GRAY));

            for (int i = 0; i < 27; i++) {
                gameBoard.setStack(i, glass.copy());
            }

            // Pick the target item (what they're looking for)
            ItemStack targetItem = player.getMainHandStack().copy();
            if (targetItem.isEmpty() || targetItem.getItem() == net.minecraft.item.Items.GRAY_STAINED_GLASS_PANE) {
                targetItem = new ItemStack(net.minecraft.item.Items.DIAMOND);
            }
            targetItem.setCustomName(Text.literal("FIND ME!").formatted(Formatting.GOLD, Formatting.BOLD));

            // Pick a random winning slot
            int winningSlot = player.getRandom().nextInt(27);
            data.setMemoryTargetSlot(winningSlot);

            final ItemStack finalTarget = targetItem;

            // Open the custom screen handler
            player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                    (syncId, inv, p) -> new MemoryGameScreenHandler(syncId, inv, gameBoard),
                    Text.literal("MEMORY PROTOCOL").formatted(Formatting.DARK_PURPLE, Formatting.BOLD)
            ));

            Invissues.LOGGER.info("Memory game screen opened for {}", player.getName().getString());

            // --- PHASE 2: INITIAL REVEAL (Show the item for 2 seconds) ---
            TaskScheduler.schedule(20, () -> {
                Invissues.LOGGER.info("Phase 2 executing - revealing item...");
                if (data.getMemoryGameState() == 0) {
                    Invissues.LOGGER.warn("Game state is 0, aborting reveal");
                    return;
                }

                gameBoard.setStack(winningSlot, finalTarget);
                player.currentScreenHandler.sendContentUpdates();

                player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), SoundCategory.PLAYERS, 1.0f, 2.0f);
                player.sendMessage(Text.literal("MEMORIZE THIS LOCATION!").formatted(Formatting.GOLD, Formatting.BOLD), true);
            });

            // --- PHASE 3: SHUFFLE ANIMATION (5 shuffles over 2.5 seconds) ---
            for (int shuffleIndex = 0; shuffleIndex < 5; shuffleIndex++) {
                final int index = shuffleIndex;
                TaskScheduler.schedule(60 + (index * 10), () -> {
                    if (data.getMemoryGameState() == 0) return;

                    // Clear all slots
                    for (int j = 0; j < 27; j++) {
                        gameBoard.setStack(j, glass.copy());
                    }

                    // Show the item in a random slot (fake shuffle)
                    int fakeSlot = player.getRandom().nextInt(27);
                    gameBoard.setStack(fakeSlot, finalTarget.copy());

                    player.currentScreenHandler.sendContentUpdates();
                    player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(), SoundCategory.PLAYERS, 0.7f, 1.5f + (index * 0.15f));
                });
            }

            // --- CRITICAL: Reset item to correct slot after shuffle animation ---
            TaskScheduler.schedule(115, () -> {
                if (data.getMemoryGameState() == 0) return;

                // Clear everything and put item back in the REAL winning slot
                for (int j = 0; j < 27; j++) {
                    gameBoard.setStack(j, glass.copy());
                }
                gameBoard.setStack(winningSlot, finalTarget.copy());
                player.currentScreenHandler.sendContentUpdates();

                Invissues.LOGGER.info("Item reset to winning slot {} before hiding", winningSlot);
            });

            // --- PHASE 4: HIDE AND GUESS (After shuffling, hide everything) ---
            TaskScheduler.schedule(120, () -> {
                if (data.getMemoryGameState() == 0) return;

                // Hide everything with question mark glass
                ItemStack questionGlass = new ItemStack(net.minecraft.item.Items.GRAY_STAINED_GLASS_PANE);
                questionGlass.setCustomName(Text.literal("?").formatted(Formatting.YELLOW));

                for (int j = 0; j < 27; j++) {
                    gameBoard.setStack(j, questionGlass.copy());
                }

                data.setMemoryGameState(2); // State 2 = Guessing phase (clicks are now active)
                player.currentScreenHandler.sendContentUpdates();

                player.playSound(SoundEvents.ENTITY_ENDERMAN_STARE, SoundCategory.PLAYERS, 1.0f, 0.5f);
                player.sendMessage(Text.literal("CLICK THE CORRECT SLOT!").formatted(Formatting.RED, Formatting.BOLD), true);
                player.sendMessage(Text.literal("(You have 10 seconds)").formatted(Formatting.GRAY), false);
            });

            // --- PHASE 5: TIMEOUT (If they don't click within 10 seconds, they lose) ---
            TaskScheduler.schedule(320, () -> {
                if (data.getMemoryGameState() != 2) return; // Already finished

                // Time's up!
                data.setMemoryGameState(0);
                data.modifyMood(-8);

                player.closeHandledScreen();
                player.sendMessage(Text.literal("⏱ TIME'S UP! -8 MOOD").formatted(Formatting.DARK_RED, Formatting.BOLD), false);
                player.playSound(SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.PLAYERS, 1.0f, 0.5f);

                ActionDispatcher.executeScramble(player, true);
                Invissues.LOGGER.info("{} timed out on the memory game", player.getName().getString());
            });
        });
    }
}