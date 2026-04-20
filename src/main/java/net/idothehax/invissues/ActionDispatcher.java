package net.idothehax.invissues;

import net.idothehax.invissues.registry.ModComponents;
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
        int roll = player.getRandom().nextInt(100);

        Invissues.LOGGER.info("{}'s inventory is acting up. Mood: {}, Roll: {}", player.getName().getString(), mood, roll);

        if (mood <= 30) {
            // --- HIGH CHAOS (Hates you) ---
            if (roll < 10) { // 10% chance for the Memory Game
                executeMemoryGame(player);
            } else if (roll < 15) { // 15% chance when mood is low to start the sort protocol
                if (ModComponents.SENTIENT_DATA.get(player).getSortTimer() <= 0) {
                    SortChallengeManager.startChallenge(player);
                }
            } else if (roll < 33) {
                executeTantrumDrop(player);
            } else if (roll < 66) {
                executeArmorStrip(player);
            } else {
                executeScramble(player, true); // Violent scramble (many swaps)
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
    private static void executeTantrumDrop(ServerPlayerEntity player) {
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
     * Trigger interface roulette where opening a block with GUI will give a random GUI that is NOT the intented GUI.
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
     * Triggers the memory game where the inventory briefly shows fake items, then scrambles. The player has to remember where their items are!
     */
    public static void executeMemoryGame(ServerPlayerEntity player) {
        var server = player.getServer();
        if (server == null) return;

        // 1. Start the countdown with obfuscated "Memory" text
        player.sendMessage(Text.literal("--- [")
                .append(Text.literal("MEMORY").formatted(Formatting.OBFUSCATED))
                .append(Text.literal("] PROTOCOL: 3 ---")).formatted(Formatting.DARK_PURPLE), true);
        player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BIT.value(), SoundCategory.PLAYERS, 1.0f, 0.5f);

        // 2. Schedule "2" (after 1 second / 20 ticks)
        server.execute(() -> {
            scheduleTask(server, 20, () -> {
                player.sendMessage(Text.literal("--- [")
                        .append(Text.literal("MEMORY").formatted(Formatting.OBFUSCATED))
                        .append(Text.literal("] PROTOCOL: 2 ---")).formatted(Formatting.DARK_PURPLE), true);
                player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BIT.value(), SoundCategory.PLAYERS, 1.0f, 0.7f);
            });

            // 3. Schedule "1" (after 2 seconds / 40 ticks)
            scheduleTask(server, 40, () -> {
                player.sendMessage(Text.literal("--- [")
                        .append(Text.literal("MEMORY").formatted(Formatting.OBFUSCATED))
                        .append(Text.literal("] PROTOCOL: 1 ---")).formatted(Formatting.DARK_PURPLE), true);
                player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BIT.value(), SoundCategory.PLAYERS, 1.0f, 1.0f);
            });

            // 4. THE BLINDING (after 3 seconds / 60 ticks)
            scheduleTask(server, 60, () -> {
                // Hide everything
                for (int i = 0; i < 46; i++) {
                    player.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket(
                            player.currentScreenHandler.syncId,
                            player.currentScreenHandler.nextRevision(),
                            i,
                            new ItemStack(Items.BARRIER)
                    ));
                }

                // Perform the secret swap
                executeScramble(player, false);

                player.playSound(SoundEvents.ENTITY_ILLUSIONER_MIRROR_MOVE, SoundCategory.PLAYERS, 1.0f, 0.5f);
                player.sendMessage(Text.literal("WHERE IS YOUR GEAR NOW?").formatted(Formatting.OBFUSCATED, Formatting.RED), true);
            });

            // 5. THE REVEAL (after 8 seconds / 160 ticks total)
            scheduleTask(server, 160, () -> {
                player.currentScreenHandler.sendContentUpdates();
                player.playSound(SoundEvents.ENTITY_ENDERMAN_STARE, SoundCategory.PLAYERS, 1.0f, 0.5f);
                player.sendMessage(Text.literal("Truth restored... for now.").formatted(Formatting.GRAY, Formatting.ITALIC), false);
            });
        });
    }

    /**
     * Helper to handle delayed tasks without freezing the server.
     */
    private static void scheduleTask(net.minecraft.server.MinecraftServer server, int delayTicks, Runnable task) {
        long executeTime = server.getTicks() + delayTicks;
        server.execute(() -> {
            // This is a simple way to wait for a specific tick
            if (server.getTicks() < executeTime) {
                scheduleTask(server, 0, task); // Re-queue if not time yet
            } else {
                task.run();
            }
        });
    }
}