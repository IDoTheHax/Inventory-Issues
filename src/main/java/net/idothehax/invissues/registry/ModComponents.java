package net.idothehax.invissues.registry;

import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.ComponentRegistry;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentFactoryRegistry;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentInitializer;
import dev.onyxstudios.cca.api.v3.entity.RespawnCopyStrategy;
import net.idothehax.invissues.Invissues;
import net.idothehax.invissues.component.SentientComponent;
import net.idothehax.invissues.component.SentientComponentImpl;
import net.minecraft.util.Identifier;

public class ModComponents implements EntityComponentInitializer {

    // Updated to use your actual mod ID: "invissues"
    public static final ComponentKey<SentientComponent> SENTIENT_DATA =
            ComponentRegistry.getOrCreate(Identifier.of(Invissues.MOD_ID, "sentient_data"), SentientComponent.class);

    // Call this from your main mod class to force the static block to run early!
    public static void init() {
    }

    @Override
    public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
        registry.registerForPlayers(
                SENTIENT_DATA,
                SentientComponentImpl::new,
                RespawnCopyStrategy.ALWAYS_COPY
        );
    }
}