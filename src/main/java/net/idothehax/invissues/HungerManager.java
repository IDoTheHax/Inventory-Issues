package net.idothehax.invissues;

import net.idothehax.invissues.registry.ModComponents;
import net.minecraft.advancement.Advancement;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class HungerManager {

    // --- THE DYNAMIC CRAVING SYSTEM ---
    public enum Craving {
        // Tier 0 (Always Unlocked)
        DIRT("minecraft:dirt", "Dirt", false, 0, "SURFACE"),
        GRASS("minecraft:grass_block", "a Grass Block", false, 0, "SURFACE"),
        LOGS("minecraft:logs", "ANY Log", true, 0, "SURFACE"),
        PLANKS("minecraft:planks", "ANY Wood Planks", true, 0, "SURFACE"),

        // Tier 1 (Unlocked after getting Stone)
        STONE("minecraft:stone_crafting_materials", "ANY Stone", true, 1, "UNDERGROUND"),
        COAL("minecraft:coal", "Coal", false, 1, "UNDERGROUND"),

        // Tier 2 (Unlocked after getting Iron)
        IRON("minecraft:iron_ingot", "an Iron Ingot", false, 2, "UNDERGROUND"),
        COPPER("minecraft:copper_ingot", "a Copper Ingot", false, 2, "UNDERGROUND"),
        RAW_IRON("minecraft:raw_iron", "Raw Iron", false, 2, "UNDERGROUND"),

        // Tier 3 (Unlocked after reaching the Nether)
        NETHERRACK("minecraft:netherrack", "Netherrack", false, 3, "NETHER"),
        GOLD("minecraft:gold_ingot", "a Gold Ingot", false, 3, "UNDERGROUND"),
        QUARTZ("minecraft:quartz", "Nether Quartz", false, 3, "NETHER"),

        // Tier 4 (Unlocked after Diamond Advancement)
        DIAMOND("minecraft:diamond", "a Diamond", false, 4, "DEEP"),
        OBSIDIAN("minecraft:obsidian", "Obsidian", false, 4, "DEEP"),

        // Tier 5 (End Game)
        ENDER_PEARL("minecraft:ender_pearl", "an Ender Pearl", false, 5, "UNIVERSAL");

        private final String registryId;
        private final String displayName;
        private final boolean isTag;
        private final int tier;
        private final String category; // SURFACE, UNDERGROUND, DEEP, NETHER, UNIVERSAL

        Craving(String registryId, String displayName, boolean isTag, int tier, String category) {
            this.registryId = registryId;
            this.displayName = displayName;
            this.isTag = isTag;
            this.tier = tier;
            this.category = category;
        }

        public boolean isSatisfiedBy(ItemStack stack) {
            if (stack.isEmpty()) return false;
            if (isTag) {
                TagKey<net.minecraft.item.Item> tagKey = TagKey.of(RegistryKeys.ITEM, new Identifier(registryId));
                return stack.isIn(tagKey);
            } else {
                return Registries.ITEM.getId(stack.getItem()).toString().equals(registryId);
            }
        }
    }

    /**
     * Calculates the player's progression tier based on vanilla advancements.
     */
    private static int getPlayerTier(ServerPlayerEntity player) {
        int tier = 0;
        var loader = player.getServer().getAdvancementLoader();
        var tracker = player.getAdvancementTracker();

        Advancement stoneAge = loader.get(new Identifier("minecraft:story/mine_stone"));
        if (stoneAge != null && tracker.getProgress(stoneAge).isDone()) tier = 1;

        Advancement ironAge = loader.get(new Identifier("minecraft:story/smelt_iron"));
        if (ironAge != null && tracker.getProgress(ironAge).isDone()) tier = 2;

        Advancement nether = loader.get(new Identifier("minecraft:story/enter_the_nether"));
        if (nether != null && tracker.getProgress(nether).isDone()) tier = 3;

        // Tier 4: Diamonds
        Advancement diamonds = loader.get(new Identifier("minecraft:story/mine_diamond"));
        if (diamonds != null && tracker.getProgress(diamonds).isDone()) tier = 4;

        // Tier 5: The End
        Advancement theEnd = loader.get(new Identifier("minecraft:story/enter_the_end"));
        if (theEnd != null && tracker.getProgress(theEnd).isDone()) tier = 5;

        return tier;
    }

    /**
     * Rolls a random valid item based on progression and starts the timer.
     */
    public static void triggerHunger(ServerPlayerEntity player) {
        int playerTier = getPlayerTier(player);
        var data = ModComponents.SENTIENT_DATA.get(player);

        // Determine player location for the Inconvenience Factor
        double yLevel = player.getY();
        boolean isUnderground = yLevel < 50 && yLevel >= 0;
        boolean isDeep = yLevel < 0;
        boolean isSurface = yLevel >= 50;
        boolean inNether = player.getWorld().getRegistryKey() == net.minecraft.world.World.NETHER;

        List<Craving> validCravings = new ArrayList<>();

        for (Craving c : Craving.values()) {
            if (c.tier <= playerTier) {
                // If the inventory is somewhat annoyed (Mood < 60), it gets malicious about location
                if (data.getMood() < 60) {
                    // Filter out easy items based on location
                    if (isSurface && c.category.equals("SURFACE")) continue;
                    if (isUnderground && c.category.equals("UNDERGROUND")) continue;
                    if (isDeep && (c.category.equals("DEEP") || c.category.equals("UNDERGROUND"))) continue;
                    if (inNether && c.category.equals("NETHER")) continue;
                }
                validCravings.add(c);
            }
        }

        // Failsafe in case we filtered out everything
        if (validCravings.isEmpty()) validCravings.add(Craving.DIRT);

        Craving selectedCraving = validCravings.get(player.getRandom().nextInt(validCravings.size()));

        data.setHungerItem(selectedCraving.name());
        data.setHungerTimer(600); // 30 seconds

        player.sendMessage(Text.literal("FEED ME: ")
                .append(Text.literal(selectedCraving.displayName).formatted(Formatting.RED, Formatting.BOLD))
                .append(Text.literal(" (Put it in your Offhand! 30 seconds!)").formatted(Formatting.GRAY)), false);

        player.playSound(SoundEvents.ENTITY_ZOMBIE_AMBIENT, SoundCategory.PLAYERS, 1.0f, 0.5f);
    }

    /**
     * Checks the player's offhand every tick to see if they fed the inventory.
     */
    public static void tickHunger(ServerPlayerEntity player) {
        var data = ModComponents.SENTIENT_DATA.get(player);

        if (data.getHungerTimer() <= 0 || data.getHungerItem().isEmpty()) {
            return;
        }

        data.decrementHungerTimer();

        // Parse the enum from the saved string
        Craving currentCraving;
        try {
            currentCraving = Craving.valueOf(data.getHungerItem());
        } catch (IllegalArgumentException e) {
            data.setHungerItem(""); // Failsafe in case data gets corrupted
            return;
        }

        ItemStack offhandStack = player.getOffHandStack();

        // Did they feed it?
        if (currentCraving.isSatisfiedBy(offhandStack)) {
            offhandStack.decrement(1); // MUNCH!

            data.setHungerItem(""); // Clear hunger
            data.setHungerTimer(0);
            data.modifyMood(15); // Good boy inventory

            player.sendMessage(Text.literal("The Inventory crunches the item happily.").formatted(Formatting.GREEN, Formatting.ITALIC), true);
            player.playSound(SoundEvents.ENTITY_GENERIC_EAT, SoundCategory.PLAYERS, 1.0f, 1.0f);
            return;
        }

        // FAILURE! Timer ran out
        if (data.getHungerTimer() == 0) {
            data.setHungerItem("");
            data.modifyMood(-25);

            player.sendMessage(Text.literal("The Inventory starved... and took its anger out by taxing your items!").formatted(Formatting.DARK_RED, Formatting.BOLD), false);
            player.playSound(SoundEvents.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.PLAYERS, 1.0f, 0.8f);

            punishStarvation(player);
        }
    }

    /**
     * Deletes a random stack of items from the player's main inventory.
     */
    private static void punishStarvation(ServerPlayerEntity player) {
        var inv = player.getInventory();
        for (int i = 0; i < 15; i++) { // Try up to 15 times to find a non-empty slot
            int slot = player.getRandom().nextInt(36);
            if (!inv.getStack(slot).isEmpty()) {
                ItemStack destroyed = inv.getStack(slot);
                Invissues.LOGGER.info("Hunger punishment: Destroyed {} from {}", destroyed.getItem().getName().getString(), player.getName().getString());

                inv.setStack(slot, ItemStack.EMPTY);
                player.currentScreenHandler.sendContentUpdates();
                break;
            }
        }
    }
}