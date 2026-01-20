package ca.lwi.hyframes.ui;

import ca.lwi.hyframes.manager.ItemFrameManager;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
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
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ItemFrameGUI extends InteractiveCustomUIPage<ItemFrameGUI.ItemFrameEventData> {

    private final ItemFrameManager itemFrameManager;
    private final Ref<ChunkStore> blockEntityRef;
    private final Map<PlayerRef, BlockPosition> activeEditSessions = new HashMap<>();

    public ItemFrameGUI(@Nonnull PlayerRef playerRef, ItemFrameManager itemFrameManager, @Nullable Ref<ChunkStore> blockEntityRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, ItemFrameEventData.CODEC);
        this.itemFrameManager = itemFrameManager;
        this.blockEntityRef = blockEntityRef;
    }

    public static class ItemFrameEventData {
        public String action;
        public ItemStack[] items;

        public static final BuilderCodec<ItemFrameEventData> CODEC = BuilderCodec
                .builder(ItemFrameEventData.class, ItemFrameEventData::new)
                .append(new KeyedCodec<>("Action", Codec.STRING),
                        (ItemFrameEventData o, String v) -> o.action = v,
                        (ItemFrameEventData o) -> o.action)
                .add()
                .append(new KeyedCodec<>("@Items", ArrayCodec.ofBuilderCodec(ItemStack.CODEC, ItemStack[]::new)),
                        (ItemFrameEventData o, ItemStack[] v) -> o.items = v,
                        (ItemFrameEventData o) -> o.items)
                .add()
                .build();
    }

    @Override
    public void build(@Nonnull Ref<EntityStore> ref,
                      @Nonnull UICommandBuilder cmd,
                      @Nonnull UIEventBuilder evt,
                      @Nonnull Store<EntityStore> store) {
        // Construire l'UI en utilisant des composants Hytale existants
        // Charger un composant de base depuis les assets Hytale
        cmd.append("InGame/Pages/Inventory/CharacterPanel.ui");

        // Lier l'événement de sauvegarde
        evt.addEventBinding(
                CustomUIEventBindingType.Activating,
                "#SaveButton",
                new EventData()
                        .append("Action", "Save")
                        .append("@Items", "#Slots.Items")
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
            BlockPosition framePosition = activeEditSessions.get(playerRef);
            if (framePosition != null && data.items != null) {
                // Sauvegarder les items dans le manager
                itemFrameManager.setItems(framePosition, Arrays.asList(data.items));

                playerRef.sendMessage(Message.raw("Item Frame updated successfully!"));
            }

            // Nettoyer la session
            activeEditSessions.remove(playerRef);
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
        activeEditSessions.put(playerRef, blockPosition);

        // Créer la page
        ItemFrameGUI page = new ItemFrameGUI(playerRef, itemFrameManager, blockEntityRef);

        // Ouvrir la page pour le joueur
        player.getPageManager().openCustomPage(ref, store, page);
    }
}