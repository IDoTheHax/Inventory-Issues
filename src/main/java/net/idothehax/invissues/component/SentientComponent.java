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
}