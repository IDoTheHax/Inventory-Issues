package net.idothehax.invissues;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.idothehax.invissues.component.SentientComponent;
import net.idothehax.invissues.registry.ModComponents;
import net.minecraft.server.network.ServerPlayerEntity;

public class SentientEngine {

    public static void register() {
        // Run at the end of every server tick
        ServerTickEvents.END_SERVER_TICK.register(server -> {

            // Loop through all online players
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {

                if (!player.isAlive() || player.isSpectator()) continue;

                // Grab the CCA component from the player
                SentientComponent brain = ModComponents.SENTIENT_DATA.get(player);

                if (brain.getCooldown() > 0) {
                    brain.decrementCooldown();
                    continue; // Skip to the next player
                }

                // Time to act! Reset timer (random interval between 10 and 20 seconds)
                int newTimer = 200 + player.getRandom().nextInt(200);
                brain.setCooldown(newTimer);

                // Trigger the behavior using the saved mood
                ActionDispatcher.triggerAction(player, brain.getMood());
            }
        });
    }
}