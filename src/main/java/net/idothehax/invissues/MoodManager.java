package net.idothehax.invissues;

import net.idothehax.invissues.registry.ModComponents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.WeakHashMap;

public class MoodManager {
    // Track timestamps for cooldowns
    private static final WeakHashMap<ServerPlayerEntity, Long> lastDamageTime = new WeakHashMap<>();
    private static final WeakHashMap<ServerPlayerEntity, Long> lastCraftTime = new WeakHashMap<>();

    // --- NEGATIVE TRIGGERS ---

    public static void onDamaged(ServerPlayerEntity player, DamageSource source, float amount) {
        long now = player.getWorld().getTime();
        long last = lastDamageTime.getOrDefault(player, 0L);

        // 1-second cooldown (20 ticks) so poison/fire doesn't instantly tank mood
        if (now - last < 20) return;
        lastDamageTime.put(player, now);

        int drop = 2; // Small base drop for taking a hit

        // Rule: Make rare/environmental events hit harder
        if (source.isOf(DamageTypes.FALL) || source.isOf(DamageTypes.LAVA) || source.isOf(DamageTypes.ON_FIRE) || source.isOf(DamageTypes.IN_FIRE)) {
            drop += 3;
        }

        // Rule: Big damage hits harder
        if (amount >= 8.0f) {
            drop += 5;
        }

        ModComponents.SENTIENT_DATA.get(player).modifyMood(-drop);
        Invissues.LOGGER.info("{} took {} damage. Mood dropped by {}", player.getName().getString(), amount, drop);
    }

    public static void onDeath(ServerPlayerEntity player) {
        ModComponents.SENTIENT_DATA.get(player).modifyMood(-20); // Massive penalty for dying
        Invissues.LOGGER.info("{} died! Mood plummeted by 20.", player.getName().getString());
    }

    // --- POSITIVE TRIGGERS ---

    public static void onEat(ServerPlayerEntity player) {
        ModComponents.SENTIENT_DATA.get(player).modifyMood(2);
        Invissues.LOGGER.info("{} ate food. Mood +2", player.getName().getString());
    }

    public static void onSleep(ServerPlayerEntity player) {
        ModComponents.SENTIENT_DATA.get(player).modifyMood(10); // Big recovery for a full night's rest
        Invissues.LOGGER.info("{} slept. Mood +10", player.getName().getString());
    }

    public static void onCraft(ServerPlayerEntity player) {
        long now = player.getWorld().getTime();
        long last = lastCraftTime.getOrDefault(player, 0L);

        // 2-second cooldown so shift-clicking a stack of wood into planks doesn't instantly max mood
        if (now - last < 40) return;
        lastCraftTime.put(player, now);

        ModComponents.SENTIENT_DATA.get(player).modifyMood(2);
        Invissues.LOGGER.info("{} crafted an item. Mood +2", player.getName().getString());
    }

    public static void onKillMob(ServerPlayerEntity player, Entity killedEntity) {
        // Only reward killing hostile monsters, not cows and sheep
        if (killedEntity instanceof HostileEntity) {
            ModComponents.SENTIENT_DATA.get(player).modifyMood(3);
            Invissues.LOGGER.info("{} killed a monster. Mood +3", player.getName().getString());
        }
    }
}