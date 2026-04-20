package net.idothehax.invissues;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Invissues implements ModInitializer {
    public static final String MOD_ID = "invissues";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        // Register our commands
        ModCommands.register();

        // Start the Sentient Engine tick loop
        SentientEngine.register();

        LOGGER.info("Inventory Issues is waking up...");
    }
}