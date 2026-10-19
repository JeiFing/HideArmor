package cn.jeifing.hidearmor;

import com.github.retrooper.packetevents.PacketEvents;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Objects;

public final class HideArmorPlugin extends JavaPlugin {
    private HiddenStateStore stateStore;
    private ArmorVisibilityManager visibilityManager;
    private ArmorPacketListener packetListener;

    @Override
    public void onEnable() {
        stateStore = new HiddenStateStore(this);
        stateStore.load();

        visibilityManager = new ArmorVisibilityManager(this, stateStore);
        packetListener = new ArmorPacketListener(visibilityManager);
        PacketEvents.getAPI().getEventManager().registerListener(packetListener);

        ArmorCommand command = new ArmorCommand(visibilityManager);
        PluginCommand hideCommand = command("hide");
        hideCommand.setExecutor(command);
        hideCommand.setTabCompleter(command);
        Bukkit.getPluginManager().registerEvents(new PlayerLifecycleListener(visibilityManager), this);

        for (Player player : Bukkit.getOnlinePlayers()) {
            visibilityManager.queueFullRefresh(player);
        }

        getLogger().info("Made by JeiFing");
        getLogger().info("HideArmor " + getPluginMeta().getVersion() + " enabled with "
                + stateStore.customizedCount() + " persisted visibility setting(s).");
    }

    @Override
    public void onDisable() {
        if (visibilityManager != null) {
            visibilityManager.shutdown();
        }
        if (packetListener != null && PacketEvents.getAPI() != null) {
            PacketEvents.getAPI().getEventManager().unregisterListener(packetListener);
        }
        if (stateStore != null) {
            stateStore.flush();
        }
    }

    private PluginCommand command(String name) {
        return Objects.requireNonNull(getCommand(name), "Missing command in plugin.yml: " + name);
    }
}
