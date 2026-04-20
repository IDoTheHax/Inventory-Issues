package net.idothehax.invissues;

import net.idothehax.invissues.registry.ModComponents;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.entity.player.PlayerInventory;

public class ActionDispatcher {

    public static void triggerAction(ServerPlayerEntity player, int mood) {
        int roll = player.getRandom().nextInt(100);

        Invissues.LOGGER.info("{}'s inventory is acting up. Mood: {}, Roll: {}", player.getName().getString(), mood, roll);

        if (mood <= 30) {
            // --- HIGH CHAOS (Hates you) ---
            if (roll < 33) {
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
    private static void executeScramble(ServerPlayerEntity player, boolean violent) {
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
}