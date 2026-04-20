package net.idothehax.invissues;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Invissues implements ModInitializer {
    public static final String MOD_ID = "invissues";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        // Registry registration
        ModCommands.register();
        SentientEngine.register();

        // TRIGGER: Killing a Mob
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, entity, killedEntity) -> {
            if (entity instanceof ServerPlayerEntity player) {
                MoodManager.onKillMob(player, killedEntity);
            }
        });

        // TRIGGER: Player Death
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (entity instanceof ServerPlayerEntity player) {
                MoodManager.onDeath(player);
            }
        });

        LOGGER.info("Inventory Issues is waking up...");
    }
}