package net.idothehax.invissues.component;

import dev.onyxstudios.cca.api.v3.component.sync.AutoSyncedComponent;
import net.idothehax.invissues.registry.ModComponents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;

public class SentientComponentImpl implements SentientComponent {
    private int mood = 50; // Starts neutral
    private int cooldown = 200; // 10 seconds before first action
    private final PlayerEntity provider;

    public SentientComponentImpl(PlayerEntity provider) {
        this.provider = provider;
    }

    @Override
    public int getMood() { return this.mood; }

    @Override
    public void setMood(int mood) {
        // Clamp the mood between 0 (Pure Hate) and 100 (Max Trust)
        this.mood = Math.max(0, Math.min(100, mood));
        ModComponents.SENTIENT_DATA.sync(this.provider); // Sync to client
    }

    @Override
    public void modifyMood(int amount) {
        this.setMood(this.mood + amount);
    }

    @Override
    public int getCooldown() { return this.cooldown; }

    @Override
    public void setCooldown(int ticks) { this.cooldown = ticks; }

    @Override
    public void decrementCooldown() {
        if (this.cooldown > 0) this.cooldown--;
    }

    // Save data when logging out
    @Override
    public void readFromNbt(NbtCompound tag) {
        if (tag.contains("SentientMood")) {
            this.mood = tag.getInt("SentientMood");
            this.cooldown = tag.getInt("SentientCooldown");
        }
    }

    // Load data when logging in
    @Override
    public void writeToNbt(NbtCompound tag) {
        tag.putInt("SentientMood", this.mood);
        tag.putInt("SentientCooldown", this.cooldown);
    }
}