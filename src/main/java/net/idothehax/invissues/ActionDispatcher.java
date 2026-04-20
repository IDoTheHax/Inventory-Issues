package net.idothehax.invissues;

import net.minecraft.server.network.ServerPlayerEntity;

import static net.idothehax.invissues.Invissues.LOGGER;

public class ActionDispatcher {

    public static void triggerAction(ServerPlayerEntity player, int mood) {
        // TODO: Add the actual inventory manipulation logic here

        // For testing: verify the engine is successfully firing
        LOGGER.info("[Sentient Inventory] " + player.getName().getString() + "'s inventory is thinking. Current mood: " + mood);

        int roll = player.getRandom().nextInt(100);

        if (mood < 50) {
            LOGGER.info("[Sentient Inventory] Executing hostile action!");
            // e.g., scrambleSlots(player);
        } else {
            LOGGER.info("[Sentient Inventory] Executing helpful/neutral action!");
            // e.g., organizeSlots(player);
        }
    }
}
