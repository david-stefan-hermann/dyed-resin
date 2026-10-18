package dyedresin;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;

/**
 * Dev only ({@code -Ddyedresin.dev.scene=<name>}): five seconds after joining (the scene of {@link DevScene} is built
 * and drawn by then), saves screenshots/&lt;name&gt;.png and quits.
 */
public final class DevSceneClient implements ClientModInitializer {
    private static int ticks;

    @Override
    public void onInitializeClient() {
        String name = System.getProperty("dyedresin.dev.scene");
        if (name == null) {
            return;
        }
        ClientTickEvents.END_CLIENT_TICK.register(minecraft -> {
            if (minecraft.level == null || minecraft.player == null) {
                return;
            }
            ticks++;
            if (ticks == 90) {
                minecraft.gui.toastManager().clear();
            }
            if (ticks == 100) {
                Screenshot.grab(minecraft.gameDirectory, name + ".png", minecraft.gameRenderer.mainRenderTarget(), 1,
                        message -> {
                        });
            }
            if (ticks == 120) {
                minecraft.stop();
            }
        });
    }
}
