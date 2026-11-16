package cn.jeifing.hidearmor;

import dev.geco.gsit.api.GSitAPI;
import dev.geco.gsit.api.event.*;
import org.bukkit.event.*;
import org.bukkit.entity.Player;

/** Optional public GSit API integration; no NMS fields or GSit modifications. */
final class GSitPoseHook implements Listener {
    private final HideArmorPlugin plugin;
    private final ArmorVisibilityManager manager;
    GSitPoseHook(HideArmorPlugin plugin,ArmorVisibilityManager manager) {
        this.plugin=plugin;this.manager=manager;
    }
    @EventHandler(priority=EventPriority.MONITOR,ignoreCancelled=true)
    public void preparing(PrePlayerPoseEvent event) {
        Player p=event.getPlayer();manager.poseClones.begin(p.getName(),p.getUniqueId());
        // Creation can still fail after the uncancelled pre-event.
        p.getScheduler().runDelayed(plugin,t->{
            if(!p.isOnline()||!GSitAPI.isPlayerPosing(p))manager.poseClones.end(p.getUniqueId());
        },()->manager.poseClones.end(p.getUniqueId()),2L);
    }
    @EventHandler(priority=EventPriority.MONITOR)
    public void posed(PlayerPoseEvent event) {
        manager.poseClones.begin(event.getPlayer().getName(),event.getPlayer().getUniqueId());
        manager.queueFullRefresh(event.getPlayer());
    }
    @EventHandler(priority=EventPriority.MONITOR)
    public void stopped(PlayerStopPoseEvent event) {
        manager.poseClones.end(event.getPlayer().getUniqueId());
        manager.queueFullRefresh(event.getPlayer());
    }
    void track(Player p) {
        if(GSitAPI.isPlayerPosing(p))manager.poseClones.begin(p.getName(),p.getUniqueId());
    }
}
