package ca.lwi.hyframes.manager;

import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestionnaire central pour tous les Item Frames
 * Stocke les items de chaque frame et gère la rotation automatique
 */
public class ItemFrameManager {

    private static final int MAX_ITEMS = 5;
    private static final int ROTATION_TICKS = 60; // 3 secondes

    // Données de chaque Item Frame
    private static class ItemFrameData {
        final List<ItemStack> items = new ArrayList<>();
        int currentIndex = 0;
        int tickCounter = 0;

        ItemFrameData() {
            for (int i = 0; i < MAX_ITEMS; i++) {
                items.add(null);
            }
        }

        boolean hasItems() {
            return items.stream().anyMatch(Objects::nonNull);
        }

        @Nullable
        ItemStack getCurrentItem() {
            return items.get(currentIndex);
        }

        void rotateToNext() {
            int nextIndex = (currentIndex + 1) % MAX_ITEMS;
            for (int i = 0; i < MAX_ITEMS; i++) {
                if (items.get(nextIndex) != null) {
                    currentIndex = nextIndex;
                    return;
                }
                nextIndex = (nextIndex + 1) % MAX_ITEMS;
            }
        }
    }

    private final Map<BlockPosition, ItemFrameData> frames = new ConcurrentHashMap<>();

    /**
     * Enregistre un Item Frame dans le système
     */
    public void registerFrame(@Nonnull BlockPosition position) {
        frames.putIfAbsent(position, new ItemFrameData());
    }

    /**
     * Retire un Item Frame du système
     */
    public void unregisterFrame(@Nonnull BlockPosition position) {
        frames.remove(position);
    }

    /**
     * Vérifie si un Item Frame est enregistré
     */
    public boolean isFrameRegistered(@Nonnull BlockPosition position) {
        return frames.containsKey(position);
    }

    /**
     * Définit un item à un slot spécifique
     */
    public void setItem(@Nonnull BlockPosition position, int slot, @Nullable ItemStack item) {
        ItemFrameData data = frames.get(position);
        if (data != null && slot >= 0 && slot < MAX_ITEMS) {
            data.items.set(slot, item);
            if (!data.hasItems() || slot == data.currentIndex) {
                data.currentIndex = slot;
            }
        }
    }

    /**
     * Obtient un item d'un slot spécifique
     */
    @Nullable
    public ItemStack getItem(@Nonnull BlockPosition position, int slot) {
        ItemFrameData data = frames.get(position);
        if (data != null && slot >= 0 && slot < MAX_ITEMS) {
            return data.items.get(slot);
        }
        return null;
    }

    /**
     * Obtient tous les items d'un frame
     */
    @Nonnull
    public List<ItemStack> getItems(@Nonnull BlockPosition position) {
        ItemFrameData data = frames.get(position);
        if (data != null) {
            return new ArrayList<>(data.items);
        }
        return new ArrayList<>();
    }

    /**
     * Définit tous les items d'un frame
     */
    public void setItems(@Nonnull BlockPosition position, @Nonnull List<ItemStack> items) {
        ItemFrameData data = frames.get(position);
        if (data != null) {
            data.items.clear();
            for (int i = 0; i < MAX_ITEMS; i++) {
                if (i < items.size()) {
                    data.items.add(items.get(i));
                } else {
                    data.items.add(null);
                }
            }
            // Trouver le premier slot non-vide
            for (int i = 0; i < MAX_ITEMS; i++) {
                if (data.items.get(i) != null) {
                    data.currentIndex = i;
                    return;
                }
            }
        }
    }

    /**
     * Obtient l'item actuellement affiché
     */
    @Nullable
    public ItemStack getCurrentItem(@Nonnull BlockPosition position) {
        ItemFrameData data = frames.get(position);
        return data != null ? data.getCurrentItem() : null;
    }

    /**
     * Obtient l'index de l'item actuellement affiché
     */
    public int getCurrentIndex(@Nonnull BlockPosition position) {
        ItemFrameData data = frames.get(position);
        return data != null ? data.currentIndex : 0;
    }

    /**
     * Vide tous les items d'un frame
     */
    public void clearItems(@Nonnull BlockPosition position) {
        ItemFrameData data = frames.get(position);
        if (data != null) {
            for (int i = 0; i < MAX_ITEMS; i++) {
                data.items.set(i, null);
            }
            data.currentIndex = 0;
            data.tickCounter = 0;
        }
    }

    /**
     * Met à jour tous les frames (appelé chaque tick du serveur)
     */
    public void tick() {
        frames.forEach((position, data) -> {
            if (data.hasItems()) {
                data.tickCounter++;
                if (data.tickCounter >= ROTATION_TICKS) {
                    data.tickCounter = 0;
                    data.rotateToNext();
                    // TODO: Mettre à jour l'affichage du block
                }
            }
        });
    }

    /**
     * Obtient le nombre de frames enregistrés
     */
    public int getFrameCount() {
        return frames.size();
    }
}
