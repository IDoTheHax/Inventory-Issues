# Inventory Issues (My Inventory Hates Me)

A Fabric 1.20.1 Minecraft mod where your inventory is a sentient entity that actively judges your gameplay. Treat it well, and it might help you. Treat it poorly, and it will make your life a living nightmare.

## Core Concept
Your inventory has a hidden **Mood Meter** ranging from 0 (Pure Hatred) to 100 (Maximum Trust), defaulting to 50. Every 10 to 20 seconds, the "Sentience Engine" rolls the dice to decide whether to mess with you, help you, or do nothing based on its current mood.

### The Mood System
Your actions directly impact how your inventory feels about you. The `MoodManager` handles cooldowns so you can't cheese the system.
* **Positive Triggers (Raises Mood):** Eating food, sleeping through the night, crafting items, and killing hostile monsters.
* **Negative Triggers (Lowers Mood):** Taking damage (especially environmental like fire/fall damage), taking massive hits (8+ damage), and dying.

### Behaviors & Punishments
Depending on the current mood, the inventory will trigger different events:
* **High Chaos (Mood < 30):** * *Tantrums:* Actively throws random items out of your inventory onto the ground.
    * *Armor Strip:* Violently rips off your equipped armor and offhand, throwing them into your main inventory (or on the ground if full).
    * *Violent Scramble:* Swaps multiple items across your hotbar and main inventory.
* **Annoyed (Mood 31-70):** * *Gentle Scramble:* Mildly reorganizes your main inventory just to confuse you (leaves hotbar and armor safe).
* **Helpful (Mood > 70):** * *Planned:* Auto-sorting, emergency item saving.

## Commands
* `/invmood <0-100>`: Forcefully set the inventory's mood for testing purposes. (Requires OP/Permission Level 2).

## Upcoming Features (In Development)
* **The Hunger System:** The inventory will occasionally demand specific items via a chat/UI message. Feed it, or face the consequences.
* **Cursed Slots:** Certain slots will permanently glow red and slowly corrupt or destroy items placed inside them.
* **Phantom Items:** Fake valuable items (like diamonds or totems) that gaslight the player and disappear when clicked.

## Dependencies
* Minecraft 1.20.1
* Fabric Loader
* Fabric API
* Cardinal Components API (CCA)