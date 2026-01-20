package ca.lwi.hyframes.events;

import ca.lwi.hyframes.manager.ItemFrameManager;
import ca.lwi.hyframes.ui.ItemFrameGUI;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.UseBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.logging.Level;

public class BlockEvents {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final String ITEM_FRAME_BLOCK_ID = "LwiDev:HyFrames/Item_Frame";
    private final ItemFrameManager itemFrameManager;
    private final ItemFrameGUI itemFrameGUI;

    public BlockEvents(EventRegistry eventRegistry, ItemFrameManager itemFrameManager, ItemFrameGUI itemFrameGUI) {
        this.itemFrameManager = itemFrameManager;
        this.itemFrameGUI = itemFrameGUI;
        eventRegistry.registerGlobal(UseBlockEvent.Pre.class, this::onUseBlock);
    }

    private void onUseBlock(UseBlockEvent.Pre event) {
        LOGGER.at(Level.INFO).log("DEBUG: UseBlockEvent.Pre triggered!");

        // Récupérer le contexte d'interaction
        var context = event.getContext();
        Ref<EntityStore> playerRef = context.getEntity();
        var commandBuffer = context.getCommandBuffer();

        // Récupérer le Player et le Store
        var store = commandBuffer.getStore();
        Player player = commandBuffer.getComponent(playerRef, Player.getComponentType());
        if (player == null) return;

        // Récupérer le type de block depuis l'événement
        var blockType = event.getBlockType();
        if (blockType == null) return;

        // DEBUG: Logger l'ID du bloc cliqué
        String blockId = blockType.getId();
        LOGGER.at(Level.INFO).log("DEBUG: Block clicked: " + blockId);

        // Récupérer le PlayerRef pour envoyer un message de debug
        PlayerRef pRef = commandBuffer.getComponent(playerRef, PlayerRef.getComponentType());
        if (pRef != null) {
            pRef.sendMessage(Message.raw("§eBlock clicked: " + blockId));
        }

        // Vérifier si c'est un Item Frame
        if (!blockId.equals(ITEM_FRAME_BLOCK_ID)) {
            return;
        }

        // Récupérer la position du bloc
        BlockPosition blockPosition = context.getTargetBlock();
        if (blockPosition == null) return;

        // Annuler l'événement pour empêcher le comportement par défaut
        event.setCancelled(true);

        // Enregistrer le frame s'il ne l'est pas déjà
        if (!itemFrameManager.isFrameRegistered(blockPosition)) {
            itemFrameManager.registerFrame(blockPosition);
        }

        // Envoyer un message + ouvrir l'UI
        if (pRef != null) {
            pRef.sendMessage(Message.raw("§aOpening Item Frame Editor..."));
            itemFrameGUI.openEditGUI(pRef, blockPosition, player, store, playerRef);
        }
    }
}
