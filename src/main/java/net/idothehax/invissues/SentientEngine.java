package net.idothehax.invissues;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.idothehax.invissues.component.SentientComponent;
import net.idothehax.invissues.registry.ModComponents;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
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
                SortChallengeManager.tick(player);

                // Grab the CCA component from the player
                SentientComponent brain = ModComponents.SENTIENT_DATA.get(player);
                int mood = brain.getMood();

                // -----------------------------------------------------
                // THE TRIP & THE DEATH (Mood < 10)
                // We check every 20 ticks (1 second) so we don't spam the server
                // -----------------------------------------------------
                if (server.getTicks() % 20 == 0) {

                    // 1. THE TRIP (Mood between 0 and 9)
                    if (mood < 10) {
                        // Apply Nausea for 15 seconds (keeps the screen constantly wobbling)
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 300, 0, false, false, false));

                        // Randomly play creepy ambient cave noises directly into their ears
                        if (player.getRandom().nextFloat() < 0.15f) {
                            player.playSound(SoundEvents.AMBIENT_CAVE.value(), SoundCategory.AMBIENT, 1.0f, 0.5f);
                        }
                    }

                    // 2. THE DEATH (Mood == 0)
                    if (mood <= 0) {
                        // Add Darkness to make it absolutely terrifying
                        player.addStatusEffect(new StatusEffectInstance(StatusEffects.DARKNESS, 60, 0, false, false, false));

                        // Deal 1 Heart (2.0f) of Magic Damage every 2 seconds (so we check % 40)
                        if (server.getTicks() % 40 == 0) {
                            // .magic() bypasses normal armor
                            player.damage(player.getDamageSources().magic(), 2.0f);

                            // Occasional creepy chat message
                            if (player.getRandom().nextFloat() < 0.3f) {
                                player.sendMessage(Text.literal("Your gear is consuming your life force...")
                                        .formatted(Formatting.DARK_RED, Formatting.ITALIC), true);
                            }
                        }
                    }
                }

                if (brain.getCooldown() > 0) {
                    brain.decrementCooldown();
                    continue; // Skip to the next player
                }

                if (mood <= 0 && player.isDead()) {
                    brain.setMood(50); // Reset the mood after death so they have a chance to recover and play again
                }

                // Memory Game Check - If the player has an active memory game and is looking at their normal inventory, punish them for trying to escape
                if (brain.getMemoryGameState() > 0) {
                    // If they closed the UI by hitting ESC, currentScreenHandler reverts to playerScreenHandler
                    if (player.currentScreenHandler == player.playerScreenHandler) {
                        brain.setMemoryGameState(0);
                        brain.modifyMood(-40);
                        ActionDispatcher.executeTantrumDrop(player);
                        player.sendMessage(Text.literal("COWARD. YOU CANNOT ESCAPE THE GAME.").formatted(Formatting.DARK_RED, Formatting.BOLD), false);
                    }
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

                if (brain.getMood() < 60 && player.getRandom().nextFloat() < 0.01f) { // ~1% chance per tick while annoyed
                    ActionDispatcher.executePhantomItem(player);
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