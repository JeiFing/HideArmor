package cn.jeifing.hidearmor;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.Equipment;
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

final class ArmorVisibilityManager {
    private final HideArmorPlugin plugin;
    private final HiddenStateStore stateStore;
    private final BedrockPlayerDetector bedrockPlayerDetector;
    private final ConcurrentHashMap<Integer, UUID> playerIdsByEntityId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, Integer> entityIdsByPlayerId = new ConcurrentHashMap<>();
    private final Set<UUID> queuedFullRefreshes = ConcurrentHashMap.newKeySet();
    private final Set<UUID> queuedSelfEquipmentRefreshes = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<UUID, Long> generations = new ConcurrentHashMap<>();
    private final AtomicLong generationCounter = new AtomicLong();
    private final AtomicBoolean enabled = new AtomicBoolean(true);

    ArmorVisibilityManager(HideArmorPlugin plugin, HiddenStateStore stateStore) {
        this.plugin = plugin;
        this.stateStore = stateStore;
        this.bedrockPlayerDetector = new BedrockPlayerDetector(plugin.getLogger());
    }

    ArmorVisibilitySetting setting(UUID playerId) {
        return stateStore.setting(playerId);
    }

    boolean hasHiddenSlots(UUID playerId) {
        return stateStore.hasHiddenSlots(playerId);
    }

    UUID playerIdForEntity(int entityId) {
        return playerIdsByEntityId.get(entityId);
    }

    long generation(UUID playerId) {
        return generations.getOrDefault(playerId, 0L);
    }

    boolean isCurrentMasking(UUID playerId, long expectedGeneration) {
        return enabled.get()
                && stateStore.hasHiddenSlots(playerId)
                && generation(playerId) == expectedGeneration;
    }

    boolean shouldUseModelLessView(User user) {
        if (user == null || bedrockPlayerDetector.isBedrock(user.getUUID())) {
            return false;
        }

        ClientVersion version = user.getClientVersion();
        return version != null
                && version != ClientVersion.UNKNOWN
                && version.getProtocolVersion() >= ClientVersion.V_1_21_5.getProtocolVersion();
    }

    void setVisibility(Player player, ArmorVisibilitySetting setting) {
        track(player);
        generations.put(player.getUniqueId(), generationCounter.incrementAndGet());
        stateStore.setVisibility(player.getUniqueId(), setting);
        player.updateInventory();
        sendFullSnapshot(player, setting);
    }

    void track(Player player) {
        UUID playerId = player.getUniqueId();
        int entityId = player.getEntityId();
        bedrockPlayerDetector.track(playerId);
        generations.computeIfAbsent(playerId, ignored -> generationCounter.incrementAndGet());

        Integer previousEntityId = entityIdsByPlayerId.put(playerId, entityId);
        if (previousEntityId != null && previousEntityId != entityId) {
            playerIdsByEntityId.remove(previousEntityId, playerId);
        }

        UUID displacedPlayerId = playerIdsByEntityId.put(entityId, playerId);
        if (displacedPlayerId != null && !displacedPlayerId.equals(playerId)) {
            entityIdsByPlayerId.remove(displacedPlayerId, entityId);
        }
    }

    void untrack(Player player) {
        UUID playerId = player.getUniqueId();
        Integer entityId = entityIdsByPlayerId.remove(playerId);
        if (entityId != null) {
            playerIdsByEntityId.remove(entityId, playerId);
        }
        queuedFullRefreshes.remove(playerId);
        queuedSelfEquipmentRefreshes.remove(playerId);
        generations.remove(playerId);
        bedrockPlayerDetector.untrack(playerId);
    }

    void queueFullRefresh(Player player) {
        UUID playerId = player.getUniqueId();
        if (!queuedFullRefreshes.add(playerId)) {
            return;
        }

        player.getScheduler().runDelayed(plugin, task -> {
            queuedFullRefreshes.remove(playerId);
            if (!player.isOnline()) {
                return;
            }
            track(player);
            ArmorVisibilitySetting setting = stateStore.setting(playerId);
            if (setting.hasHiddenSlots()) {
                player.updateInventory();
                sendFullSnapshot(player, setting);
            }
        }, () -> queuedFullRefreshes.remove(playerId), 1L);
    }

    void queueSelfEquipmentRefresh(Player player) {
        UUID playerId = player.getUniqueId();
        if (!queuedSelfEquipmentRefreshes.add(playerId)) {
            return;
        }

        player.getScheduler().runDelayed(plugin, task -> {
            queuedSelfEquipmentRefreshes.remove(playerId);
            if (!player.isOnline()) {
                return;
            }
            ArmorVisibilitySetting setting = stateStore.setting(playerId);
            if (!setting.hasHiddenSlots()) {
                return;
            }
            track(player);
            sendToViewer(player, player, setting);
        }, () -> queuedSelfEquipmentRefreshes.remove(playerId), 1L);
    }

    void shutdown() {
        enabled.set(false);
        queuedFullRefreshes.clear();
        queuedSelfEquipmentRefreshes.clear();
        playerIdsByEntityId.clear();
        entityIdsByPlayerId.clear();
        generations.clear();
        bedrockPlayerDetector.clear();
    }

    private void sendFullSnapshot(Player subject, ArmorVisibilitySetting setting) {
        Set<Player> viewers = new HashSet<>(subject.getTrackedBy());
        viewers.add(subject);
        for (Player viewer : viewers) {
            sendToViewer(viewer, subject, setting);
        }
    }

    private void sendToViewer(
            Player viewer,
            Player subject,
            ArmorVisibilitySetting setting
    ) {
        var user = PacketEvents.getAPI().getPlayerManager().getUser(viewer);
        if (user != null) {
            EquipmentMasker.MaskMode mode = viewer.getUniqueId().equals(subject.getUniqueId())
                    && shouldUseModelLessView(user)
                    ? EquipmentMasker.MaskMode.MODEL_LESS
                    : EquipmentMasker.MaskMode.AIR;
            List<Equipment> equipment = buildEquipment(subject, setting, mode);
            WrapperPlayServerEntityEquipment packet =
                    new WrapperPlayServerEntityEquipment(subject.getEntityId(), equipment);
            user.sendPacket(packet);
        }
    }

    private static List<Equipment> buildEquipment(
            Player subject,
            ArmorVisibilitySetting setting,
            EquipmentMasker.MaskMode mode
    ) {
        PlayerInventory inventory = subject.getInventory();
        List<Equipment> equipment = new ArrayList<>(4);

        equipment.add(new Equipment(EquipmentSlot.BOOTS, toPacketItem(inventory.getBoots())));
        equipment.add(new Equipment(EquipmentSlot.LEGGINGS, toPacketItem(inventory.getLeggings())));
        equipment.add(new Equipment(EquipmentSlot.CHEST_PLATE, toPacketItem(inventory.getChestplate())));
        equipment.add(new Equipment(EquipmentSlot.HELMET, toPacketItem(inventory.getHelmet())));
        List<Equipment> realEquipment = List.copyOf(equipment);
        return setting.hasHiddenSlots()
                ? EquipmentMasker.mask(realEquipment, mode, setting).equipment()
                : realEquipment;
    }

    private static ItemStack toPacketItem(org.bukkit.inventory.ItemStack bukkitItem) {
        if (bukkitItem == null || bukkitItem.getType().isAir()) {
            return ItemStack.EMPTY;
        }
        return SpigotConversionUtil.fromBukkitItemStack(bukkitItem);
    }
}
