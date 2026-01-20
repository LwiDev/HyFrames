package ca.lwi.hyframes.ui;

import ca.lwi.hyframes.manager.ItemFrameManager;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.interface_.Page;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemFrameGUI extends InteractiveCustomUIPage<ItemFrameGUI.ItemFrameEventData> {

    private final ItemFrameManager itemFrameManager;
    private final Map<String, BlockPosition> activeEditSessions = new HashMap<>();

    public ItemFrameGUI(@Nonnull PlayerRef playerRef, ItemFrameManager itemFrameManager) {
        super(playerRef, CustomPageLifetime.CanDismiss, ItemFrameEventData.CODEC);
        this.itemFrameManager = itemFrameManager;
    }

    public static class ItemFrameEventData {
        public String action;
        public List<ItemStack> items;

        public static final BuilderCodec<ItemFrameEventData> CODEC = BuilderCodec
                .builder(ItemFrameEventData.class, ItemFrameEventData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING),
                        (ItemFrameEventData o, String v) -> o.action = v,
                        (ItemFrameEventData o) -> o.action)
                .add()
                .append(new KeyedCodec<>("@Items", Codec.listOf(ItemStack.CODEC)),
                        (ItemFrameEventData o, List<ItemStack> v) -> o.items = v,
                        (ItemFrameEventData o) -> o.items)
                .add()
                .build();
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
                     @Nonnull UICommandBuilder cmd,
                     @Nonnull UIEventBuilder evt,
                     @Nonnull Store<EntityStore> store) {
        cmd.append("Common/UI/Custom/Pages/ItemFrameUI.json");

        // Lier l'événement de sauvegarde
        evt.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#SaveButton",
                new EventData()
                        .append("Action", "Save")
                        .append("@Items", "#ItemSlots.Items")
        );
    }

    @Override
    public void handleDataEvent(@Nonnull Ref<EntityStore> ref,
                               @Nonnull Store<EntityStore> store,
                               @Nonnull ItemFrameEventData data) {
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) return;

        if ("Save".equals(data.action)) {
            // Récupérer la position du frame depuis la session
            BlockPosition framePosition = activeEditSessions.get(player.getName());
            if (framePosition != null && data.items != null) {
                // Sauvegarder les items dans le manager
                itemFrameManager.setItems(framePosition, data.items);

                playerRef.sendMessage(Message.raw("Item Frame updated successfully!"));
            }

            // Nettoyer la session
            activeEditSessions.remove(player.getName());
        }

        // Fermer la page
        player.getPageManager().setPage(ref, store, Page.None);
    }

    /**
     * Ouvre l'interface d'édition pour un joueur
     */
    public void openEditGUI(@Nonnull PlayerRef playerRef, @Nonnull BlockPosition blockPosition,
                           @Nonnull Player player, @Nonnull Store<EntityStore> store,
                           @Nonnull Ref<EntityStore> ref) {
        // Stocker la session active
        activeEditSessions.put(player.getName(), blockPosition);

        // Créer la page
        ItemFrameGUI page = new ItemFrameGUI(playerRef, itemFrameManager);

        // Ouvrir la page pour le joueur
        player.getPageManager().openCustomPage(ref, store, page);
    }
}
