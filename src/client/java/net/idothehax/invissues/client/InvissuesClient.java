package net.idothehax.invissues.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.idothehax.invissues.registry.ModComponents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;

public class InvissuesClient implements ClientModInitializer {

    // This points directly to the vanilla GUI texture file that holds the hearts, hunger, and XP bar
    private static final Identifier ICONS = new Identifier("minecraft", "textures/gui/icons.png");

    @Override
    public void onInitializeClient() {
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null) return;

            int mood = ModComponents.SENTIENT_DATA.get(client.player).getMood();

            int screenWidth = client.getWindow().getScaledWidth();
            int screenHeight = client.getWindow().getScaledHeight();

            // The exact width and height of the vanilla XP bar frame
            int barWidth = 182;
            int barHeight = 5;

            int x = (screenWidth / 2) - 91;
            int y = screenHeight - 29; // Centered beautifully in our new gap!

            // 1. Draw the empty Vanilla XP bar frame
            // This gives us that classic Minecraft gray border
            drawContext.drawTexture(ICONS, x, y, 0, 64, barWidth, barHeight);

            // 2. Draw the vibrant inner fill
            // The inside of the bar is 180 pixels wide and 3 pixels tall
            int innerWidth = 180;
            int fillWidth = (int) ((mood / 100.0f) * innerWidth);

            if (fillWidth > 0) {
                float fraction = mood / 100.0f;

                // Re-using the smooth hex color interpolator for true colors!
                // 0xFF4A0080 = Rich Dark Purple
                // 0xFFD896FF = Bright Pastel Purple
                int currentColor = interpolateColor(0xFF4A0080, 0xFFD896FF, fraction);

                // We draw the raw color fill exactly inside the 1-pixel borders (x + 1, y + 1)
                drawContext.fill(x + 1, y + 1, x + 1 + fillWidth, y + 4, currentColor);
            }

            // --- THE "TRIP" SCREEN TINT (Add this inside HudRenderCallback, below the bar code) ---
            if (mood < 10) {
                // Pulse the alpha (transparency) using a sine wave so it throbs smoothly
                float alpha = 0.15f + 0.25f * (float) ((Math.sin(System.currentTimeMillis() / 150.0) + 1.0) / 2.0);

                // If mood hits 0, the flashing becomes erratic, dark, and violent
                if (mood <= 0) {
                    alpha = 0.3f + 0.5f * client.player.getRandom().nextFloat();
                }

                int alphaInt = (int) (alpha * 255);
                int purpleColor = (alphaInt << 24) | 0x800080; // Hex 800080 is pure purple

                // Draw the tint over the ENTIRE screen
                drawContext.fill(0, 0, screenWidth, screenHeight, purpleColor);
            }
        });
    }

    /**
     * Smoothly blends between two ARGB hex colors.
     */
    private int interpolateColor(int color1, int color2, float fraction) {
        int a1 = (color1 >> 24) & 0xff;
        int r1 = (color1 >> 16) & 0xff;
        int g1 = (color1 >> 8) & 0xff;
        int b1 = color1 & 0xff;

        int a2 = (color2 >> 24) & 0xff;
        int r2 = (color2 >> 16) & 0xff;
        int g2 = (color2 >> 8) & 0xff;
        int b2 = color2 & 0xff;

        int a = (int) (a1 + (a2 - a1) * fraction);
        int r = (int) (r1 + (r2 - r1) * fraction);
        int g = (int) (g1 + (g2 - g1) * fraction);
        int b = (int) (b1 + (b2 - b1) * fraction);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}