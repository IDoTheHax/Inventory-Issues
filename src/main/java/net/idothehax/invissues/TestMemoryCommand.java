package net.idothehax.invissues;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class TestMemoryCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("testmemory")
                .requires(source -> source.hasPermissionLevel(2)) // Requires OP
                .executes(TestMemoryCommand::execute));
    }

    private static int execute(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer();

        if (player == null) {
            context.getSource().sendError(Text.literal("Must be executed by a player"));
            return 0;
        }

        Invissues.LOGGER.info("TEST COMMAND: /testmemory executed by {}", player.getName().getString());
        context.getSource().sendFeedback(() -> Text.literal("§aTesting memory game..."), false);

        try {
            ActionDispatcher.executeMemoryGame(player);
            Invissues.LOGGER.info("TEST COMMAND: executeMemoryGame called successfully");
        } catch (Exception e) {
            Invissues.LOGGER.error("TEST COMMAND: Error executing memory game", e);
            context.getSource().sendError(Text.literal("Error: " + e.getMessage()));
            return 0;
        }

        return 1;
    }
}