package net.idothehax.invissues.client.mixin;

import net.idothehax.invissues.BounceState;
import net.idothehax.invissues.MutableSlot;
import net.idothehax.invissues.registry.ModComponents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.WeakHashMap;

@Mixin(HandledScreen.class)
public abstract class SlotBounceMixin extends Screen {

    @Shadow protected int x;
    @Shadow protected int y;
    @Shadow @Final protected ScreenHandler handler;

    private static final WeakHashMap<Slot, BounceState> BOUNCE_STATES = new WeakHashMap<>();

    protected SlotBounceMixin(net.minecraft.text.Text title) {
        super(title);
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void onRenderDVD(net.minecraft.client.gui.DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || this.handler == null) return;

        var data = ModComponents.SENTIENT_DATA.get(client.player);
        int mood = data.getMood();
        int gameState = data.getMemoryGameState();
        int sortTimer = data.getSortTimer();

        if (mood < 20 && gameState == 0 && sortTimer <= 0) {
            for (Slot slot : this.handler.slots) {
                BounceState state = BOUNCE_STATES.computeIfAbsent(slot, k -> new BounceState());

                if (!state.initialized) {
                    state.origX = slot.x;
                    state.origY = slot.y;
                    state.currentX = slot.x;
                    state.currentY = slot.y;

                    float speedX = 0.5f + client.player.getRandom().nextFloat();
                    float speedY = 0.5f + client.player.getRandom().nextFloat();

                    state.vx = client.player.getRandom().nextBoolean() ? speedX : -speedX;
                    state.vy = client.player.getRandom().nextBoolean() ? speedY : -speedY;
                    state.initialized = true;
                }

                float screenAbsoluteX = this.x + state.currentX;
                float screenAbsoluteY = this.y + state.currentY;

                if (screenAbsoluteX <= 0) {
                    state.currentX = -this.x;
                    state.vx = Math.abs(state.vx);
                } else if (screenAbsoluteX + 16 >= this.width) {
                    state.currentX = this.width - 16 - this.x;
                    state.vx = -Math.abs(state.vx);
                }

                if (screenAbsoluteY <= 0) {
                    state.currentY = -this.y;
                    state.vy = Math.abs(state.vy);
                } else if (screenAbsoluteY + 16 >= this.height) {
                    state.currentY = this.height - 16 - this.y;
                    state.vy = -Math.abs(state.vy);
                }

                state.currentX += state.vx;
                state.currentY += state.vy;

                if (slot instanceof MutableSlot mutableSlot) {
                    mutableSlot.inventoryIssues$setX((int) state.currentX);
                    mutableSlot.inventoryIssues$setY((int) state.currentY);
                }
            }
        } else {
            for (Slot slot : this.handler.slots) {
                BounceState state = BOUNCE_STATES.get(slot);
                if (state != null && state.initialized) {
                    if (slot instanceof MutableSlot mutableSlot) {
                        mutableSlot.inventoryIssues$setX(state.origX);
                        mutableSlot.inventoryIssues$setY(state.origY);
                    }
                    state.initialized = false;
                }
            }
        }
    }

    // --- DRAW NEON OUTLINES AROUND BOUNCING SLOTS ---
    @Inject(method = "render", at = @At("TAIL"))
    private void onRenderDVDOutlines(net.minecraft.client.gui.DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || this.handler == null) return;

        var data = ModComponents.SENTIENT_DATA.get(client.player);
        int mood = data.getMood();
        int gameState = data.getMemoryGameState();
        int sortTimer = data.getSortTimer();

        // Only draw the borders if the bouncing phase is actually active
        if (mood < 20 && gameState == 0 && sortTimer <= 0) {
            for (Slot slot : this.handler.slots) {
                // Calculate the absolute screen coordinates of the slot
                int absoluteX = this.x + slot.x;
                int absoluteY = this.y + slot.y;

                // Draw a glowing magenta border around the 16x16 slot
                // We use x-1 and y-1, and 18x18 size to perfectly wrap the 16x16 item icon
                context.drawBorder(absoluteX - 1, absoluteY - 1, 18, 18, 0xFFFF00FF);

                // Optional: Draw a super faint transparent box inside the slot so it's even more visible
                context.fill(absoluteX, absoluteY, absoluteX + 16, absoluteY + 16, 0x30FF00FF);
            }
        }
    }
}