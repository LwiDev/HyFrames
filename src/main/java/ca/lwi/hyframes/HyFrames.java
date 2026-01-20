package ca.lwi.hyframes;

import ca.lwi.hyframes.events.BlockEvents;
import ca.lwi.hyframes.manager.ItemFrameManager;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.task.TaskRegistration;
import javax.annotation.Nonnull;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class HyFrames extends JavaPlugin {

    private ItemFrameManager itemFrameManager;
    private TaskRegistration tickTaskRegistration;

    public HyFrames(@Nonnull JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        // Initialiser le manager
        itemFrameManager = new ItemFrameManager();

        // Enregistrer les événements
        new BlockEvents(getEventRegistry(), itemFrameManager, null);

        // Démarrer le tick system pour la rotation (toutes les 1 tick = 33ms)
        startTickSystem();

        getLogger().info("HyFrames plugin enabled!");
    }

    private void startTickSystem() {
        // Exécuter itemFrameManager.tick() toutes les 1 tick (30 TPS)
        long tickIntervalMs = 33; // 1000ms / 30 ticks = ~33ms par tick

        @SuppressWarnings("unchecked")
        ScheduledFuture<Void> tickTask = (ScheduledFuture<Void>)
                HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(
                        () -> {
                            try {
                                itemFrameManager.tick();
                            } catch (Exception e) {
                                getLogger().error("Error in tick system", e);
                            }
                        },
                        0,              // Démarrer immédiatement
                        tickIntervalMs, // Intervalle
                        TimeUnit.MILLISECONDS
                );

        // Enregistrer pour auto-cleanup au shutdown
        tickTaskRegistration = getTaskRegistry().registerTask(tickTask);

        getLogger().info("Tick system started (30 TPS)");
    }

    @Override
    protected void tearDown() {
        // Le TaskRegistry annule automatiquement toutes les tâches enregistrées
        getLogger().info("HyFrames plugin disabled!");
    }

    public ItemFrameManager getItemFrameManager() {
        return itemFrameManager;
    }
}