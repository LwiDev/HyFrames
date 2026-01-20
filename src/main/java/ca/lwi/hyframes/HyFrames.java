package ca.lwi.hyframes;

import ca.lwi.hyframes.manager.ItemFrameManager;
import ca.lwi.hyframes.ui.ItemFrameGUI;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.OpenCustomUIInteraction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.task.TaskRegistration;
import javax.annotation.Nonnull;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class HyFrames extends JavaPlugin {

    private ItemFrameManager itemFrameManager;
    private TaskRegistration tickTaskRegistration;

    public HyFrames(@Nonnull JavaPluginInit init) {
        super(init);

        // Initialize the manager in constructor
        itemFrameManager = new ItemFrameManager();

        // Register the interaction in CONSTRUCTOR - this is critical for it to be available when assets load
        getLogger().at(Level.INFO).log("Registering Item Frame interaction codec...");

        OpenCustomUIInteraction.CustomPageSupplier supplier = (ref, componentAccessor, playerRef, context) -> {
            BlockPosition targetBlock = context.getTargetBlock();
            if (targetBlock == null) {
                getLogger().at(Level.WARNING).log("Target block is null!");
                return null;
            }

            getLogger().at(Level.INFO).log("CustomPageSupplier called for block at " + targetBlock);
            getLogger().at(Level.INFO).log("Opening ItemFrameGUI for player " + playerRef.getUsername());

            // Pour l'instant, on retourne un GUI simple sans blockEntityRef
            return new ItemFrameGUI(playerRef, itemFrameManager, null);
        };

        OpenCustomUIInteraction.registerCustomPageSupplier(
                this,
                ItemFrameGUI.class,
                "Item_Frame_Open",
                supplier
        );

        getLogger().at(Level.INFO).log("Item Frame interaction registered!");
    }

    @Override
    protected void setup() {
        // Start the tick system for rotation (every 1 tick = 33ms)
        startTickSystem();

        getLogger().at(Level.INFO).log("HyFrames plugin enabled!");
    }

    private void startTickSystem() {
        // Execute itemFrameManager.tick() every 1 tick (30 TPS)
        long tickIntervalMs = 33; // 1000ms / 30 ticks = ~33ms per tick

        @SuppressWarnings("unchecked")
        ScheduledFuture<Void> tickTask = (ScheduledFuture<Void>)
                HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(
                        () -> {
                            try {
                                itemFrameManager.tick();
                            } catch (Exception e) {
                                getLogger().at(Level.SEVERE).withCause(e).log("Error in tick system");
                            }
                        },
                        0,              // Start immediately
                        tickIntervalMs, // Interval
                        TimeUnit.MILLISECONDS
                );

        // Register for auto-cleanup on shutdown
        tickTaskRegistration = getTaskRegistry().registerTask(tickTask);

        getLogger().at(Level.INFO).log("Tick system started (30 TPS)");
    }

    @Override
    protected void shutdown() {
        // TaskRegistry automatically cancels all registered tasks
        getLogger().at(Level.INFO).log("HyFrames plugin disabled!");
    }

    public ItemFrameManager getItemFrameManager() {
        return itemFrameManager;
    }
}