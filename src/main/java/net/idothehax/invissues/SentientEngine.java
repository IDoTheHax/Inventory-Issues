package net.idothehax.invissues;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.idothehax.invissues.component.SentientComponent;
import net.idothehax.invissues.registry.ModComponents;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class SentientEngine {

    public static void register() {
        // Run at the end of every server tick
        ServerTickEvents.END_SERVER_TICK.register(server -> {

            // Loop through all online players
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {

                if (!player.isAlive() || player.isSpectator()) continue;

                HungerManager.tickHunger(player);

                // Grab the CCA component from the player
                SentientComponent brain = ModComponents.SENTIENT_DATA.get(player);

                if (brain.getCooldown() > 0) {
                    brain.decrementCooldown();
                    continue; // Skip to the next player
                }

                // TRIGGER: Item Rejection
                if (brain.getMood() < 25) {
                    ItemStack mainHand = player.getMainHandStack();
                    // If holding a weapon/tool and the inventory is furious
                    if (mainHand.getItem() instanceof net.minecraft.item.ToolItem || mainHand.getItem() instanceof net.minecraft.item.SwordItem) {
                        if (player.getRandom().nextFloat() < 0.05f) { // 5% chance per tick to reject
                            player.sendMessage(Text.literal("The inventory refuses to let you use that!").formatted(Formatting.RED), true);
                            ActionDispatcher.executeScramble(player, false); // Instant scramble to hide the tool
                            player.playSound(SoundEvents.ENTITY_ITEM_BREAK, 1.0f, 0.5f);
                        }
                    }
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