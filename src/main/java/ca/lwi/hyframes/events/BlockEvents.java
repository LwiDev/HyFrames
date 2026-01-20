package ca.lwi.hyframes.events;

import ca.lwi.hyframes.manager.ItemFrameManager;
import ca.lwi.hyframes.ui.ItemFrameGUI;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.event.EventRegistry;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.protocol.BlockType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.UseBlockEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public class BlockEvents {

    private static final String ITEM_FRAME_BLOCK_ID = "lwidev:hyframes/Item_Frame";
    private final ItemFrameManager itemFrameManager;
    private final ItemFrameGUI itemFrameGUI;

    public BlockEvents(EventRegistry eventRegistry, ItemFrameManager itemFrameManager, ItemFrameGUI itemFrameGUI) {
        this.itemFrameManager = itemFrameManager;
        this.itemFrameGUI = itemFrameGUI;
        eventRegistry.registerGlobal(UseBlockEvent.Pre.class, this::onUseBlock);
    }

    private void onUseBlock(UseBlockEvent.Pre event) {
        Ref<EntityStore> playerRef = event.getPlayerRef();
        Player player = event.getStore().getComponent(playerRef, Player.getComponentType());
        if (player == null) return;

        BlockPosition blockPosition = event.getPosition();
        World world = event.getWorld();

        // Récupérer le type de block
        BlockType blockType = world.getBlockType(blockPosition);
        if (blockType == null) return;

        // Vérifier si c'est un Item Frame
        if (!blockType.getItemId().equals(ITEM_FRAME_BLOCK_ID)) {
            return;
        }

        // Annuler l'événement pour empêcher le comportement par défaut
        event.setCancelled(true);

        // Enregistrer le frame s'il ne l'est pas déjà
        if (!itemFrameManager.isFrameRegistered(blockPosition)) {
            itemFrameManager.registerFrame(blockPosition);
        }

        // Ouvrir l'UI d'édition
        PlayerRef pRef = event.getStore().getComponent(playerRef, PlayerRef.getComponentType());
        if (pRef != null) {
            itemFrameGUI.openEditGUI(pRef, blockPosition, player, event.getStore(), playerRef);
        }
    }
}
