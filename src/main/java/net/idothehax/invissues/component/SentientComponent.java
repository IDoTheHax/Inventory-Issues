package net.idothehax.invissues.component;

import dev.onyxstudios.cca.api.v3.component.Component;
import dev.onyxstudios.cca.api.v3.component.sync.AutoSyncedComponent;

public interface SentientComponent extends Component, AutoSyncedComponent {
    int getMood();
    void setMood(int mood);
    void modifyMood(int amount);

    int getCooldown();
    void setCooldown(int ticks);
    void decrementCooldown();

    String getHungerItem();
    void setHungerItem(String itemId);

    int getHungerTimer();
    void setHungerTimer(int ticks);
    void decrementHungerTimer();

    int getSortTimer();
    void setSortTimer(int ticks);

    boolean isInventoryLocked();
    void setInventoryLocked(boolean locked);

    int targetSlot = -1;
    int getTargetSlot();
    void setTargetSlot(int slot);

    // Memory game Interface
    int getMemoryGameState(); // 0 = Off, 1 = Shuffling (No Clicking), 2 = Guessing Phase
    void setMemoryGameState(int state);

    int getMemoryTargetSlot();
    void setMemoryTargetSlot(int slot);
}