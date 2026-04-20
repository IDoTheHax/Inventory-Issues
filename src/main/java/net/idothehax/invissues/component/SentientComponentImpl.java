package net.idothehax.invissues.component;

import dev.onyxstudios.cca.api.v3.component.sync.AutoSyncedComponent;
import net.idothehax.invissues.registry.ModComponents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;

public class SentientComponentImpl implements SentientComponent {
    private int mood = 50; // Starts neutral
    private int cooldown = 200; // 10 seconds before first action
    private final PlayerEntity provider;
    private String hungerItem = null;
    private int hungerTimer = 0;
    private int sortTimer = 0;

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

    // Add these methods
    @Override
    public String getHungerItem() { return this.hungerItem; }

    @Override
    public void setHungerItem(String itemId) {
        this.hungerItem = itemId;
        ModComponents.SENTIENT_DATA.sync(this.provider);
    }

    @Override
    public int getHungerTimer() { return this.hungerTimer; }

    @Override
    public void setHungerTimer(int ticks) { this.hungerTimer = ticks; }

    @Override
    public void decrementHungerTimer() {
        if (this.hungerTimer > 0) this.hungerTimer--;
    }

    @Override
    public int getSortTimer() { return this.sortTimer; }

    @Override
    public void setSortTimer(int ticks) { this.sortTimer = ticks; }

    // Update readFromNbt
    @Override
    public void readFromNbt(NbtCompound tag) {
        if (tag.contains("SentientMood")) {
            this.mood = tag.getInt("SentientMood");
            this.cooldown = tag.getInt("SentientCooldown");
            this.hungerItem = tag.getString("HungerItem");
            this.hungerTimer = tag.getInt("HungerTimer");
            this.sortTimer = tag.getInt("SortTimer");
        }
    }

    // Update writeToNbt
    @Override
    public void writeToNbt(NbtCompound tag) {
        tag.putInt("SentientMood", this.mood);
        tag.putInt("SentientCooldown", this.cooldown);
        tag.putString("HungerItem", this.hungerItem);
        tag.putInt("HungerTimer", this.hungerTimer);
        tag.putInt("SortTimer", this.sortTimer);
    }
}