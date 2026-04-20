package net.idothehax.invissues;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.idothehax.invissues.registry.ModComponents;

public class ModCommands {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            registerMoodCommand(dispatcher);
        });
    }

    private static void registerMoodCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("invmood")
                .requires(source -> source.hasPermissionLevel(2)) // Requires OP/cheats enabled
                .then(CommandManager.argument("mood", IntegerArgumentType.integer(0, 100))
                        .executes(context -> {
                            // Get the player who ran the command
                            ServerPlayerEntity player = context.getSource().getPlayer();
                            if (player == null) return 0;

                            // Get the number they typed
                            int newMood = IntegerArgumentType.getInteger(context, "mood");

                            // Update the CCA data
                            ModComponents.SENTIENT_DATA.get(player).setMood(newMood);

                            // Send a message to the player
                            context.getSource().sendFeedback(() -> Text.literal("Inventory mood forcefully set to: " + newMood), false);
                            Invissues.LOGGER.info("Set {}'s inventory mood to {}", player.getName().getString(), newMood);

                            return 1;
                        })
                )
        );
    }
}
