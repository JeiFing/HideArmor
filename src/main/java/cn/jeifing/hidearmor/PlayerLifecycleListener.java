package cn.jeifing.hidearmor;

import io.papermc.paper.event.entity.EntityEquipmentChangedEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

final class PlayerLifecycleListener implements Listener {
    private final ArmorVisibilityManager visibilityManager;

    PlayerLifecycleListener(ArmorVisibilityManager visibilityManager) {
        this.visibilityManager = visibilityManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoin(PlayerJoinEvent event) {
        visibilityManager.track(event.getPlayer());
        visibilityManager.queueFullRefresh(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        visibilityManager.track(event.getPlayer());
        visibilityManager.queueFullRefresh(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChangedWorld(PlayerChangedWorldEvent event) {
        visibilityManager.track(event.getPlayer());
        visibilityManager.queueFullRefresh(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEquipmentChanged(EntityEquipmentChangedEvent event) {
        if (event.getEntity() instanceof Player player
                && visibilityManager.hasHiddenSlots(player.getUniqueId())
                && event.getEquipmentChanges().keySet().stream()
                .anyMatch(org.bukkit.inventory.EquipmentSlot::isArmor)) {
            // Paper fires this after the authoritative equipment state changes.
            // Vanilla only forwards that update to trackers, not to the player
            // whose client predicted a shift-click/SWAP/right-click equip.
            // Only refresh the self entity here: a full container refresh would
            // advance the menu state id and can disturb Tweakeroo's three-click
            // swap sequence.
            visibilityManager.queueSelfEquipmentRefresh(player);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent event) {
        visibilityManager.untrack(event.getPlayer());
    }
}
