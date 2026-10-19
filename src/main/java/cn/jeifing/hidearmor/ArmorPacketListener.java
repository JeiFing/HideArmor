package cn.jeifing.hidearmor;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.Equipment;
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientCreativeInventoryAction;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetCursorItem;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetPlayerInventory;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowItems;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class ArmorPacketListener extends PacketListenerAbstract {
    private final ArmorVisibilityManager visibilityManager;

    ArmorPacketListener(ArmorVisibilityManager visibilityManager) {
        super(PacketListenerPriority.HIGHEST);
        this.visibilityManager = visibilityManager;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() != PacketType.Play.Client.CREATIVE_INVENTORY_ACTION) {
            return;
        }

        WrapperPlayClientCreativeInventoryAction wrapper =
                new WrapperPlayClientCreativeInventoryAction(event);
        EquipmentMasker.ItemMaskResult result =
                EquipmentMasker.restoreCreativeItem(wrapper.getItemStack());
        if (result.changed()) {
            wrapper.setItemStack(result.item());
            event.markForReEncode(true);
        }
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.ENTITY_EQUIPMENT) {
            maskEquipmentPacket(event);
            return;
        }

        UUID playerId = event.getUser().getUUID();
        if (playerId == null) {
            return;
        }
        ArmorVisibilitySetting setting = visibilityManager.setting(playerId);
        if (!setting.hasHiddenSlots()) {
            return;
        }

        if (visibilityManager.shouldUseModelLessView(event.getUser())) {
            maskSelfInventoryPacket(event, setting);
        } else {
            scheduleSelfInventoryCorrection(event, setting);
        }
    }

    private void maskEquipmentPacket(PacketSendEvent event) {
        WrapperPlayServerEntityEquipment wrapper = new WrapperPlayServerEntityEquipment(event);
        UUID viewerId = event.getUser().getUUID();
        UUID targetId;
        if (wrapper.getEntityId() == event.getUser().getEntityId()) {
            targetId = viewerId;
        } else {
            targetId = visibilityManager.playerIdForEntity(wrapper.getEntityId());
        }

        if (targetId == null) {
            return;
        }
        ArmorVisibilitySetting setting = visibilityManager.setting(targetId);
        if (!setting.hasHiddenSlots()) {
            return;
        }

        EquipmentMasker.MaskMode mode = targetId.equals(viewerId)
                && visibilityManager.shouldUseModelLessView(event.getUser())
                ? EquipmentMasker.MaskMode.MODEL_LESS
                : EquipmentMasker.MaskMode.AIR;
        EquipmentMasker.MaskResult result =
                EquipmentMasker.mask(wrapper.getEquipment(), mode, setting);
        if (result.changed()) {
            wrapper.setEquipment(result.equipment());
            event.markForReEncode(true);
        }
    }

    private static void maskSelfInventoryPacket(
            PacketSendEvent event,
            ArmorVisibilitySetting setting
    ) {
        if (event.getPacketType() == PacketType.Play.Server.SET_PLAYER_INVENTORY) {
            WrapperPlayServerSetPlayerInventory wrapper = new WrapperPlayServerSetPlayerInventory(event);
            EquipmentSlot actualSlot = playerInventorySlot(wrapper.getSlot());
            EquipmentMasker.ItemMaskResult result = actualSlot == null
                    ? EquipmentMasker.maskInventoryItem(wrapper.getStack(), setting)
                    : EquipmentMasker.maskInventoryItem(wrapper.getStack(), actualSlot, setting);
            if (result.changed()) {
                wrapper.setStack(result.item());
                event.markForReEncode(true);
            }
            return;
        }

        if (event.getPacketType() == PacketType.Play.Server.SET_SLOT) {
            WrapperPlayServerSetSlot wrapper = new WrapperPlayServerSetSlot(event);
            EquipmentSlot actualSlot = wrapper.getWindowId() == 0
                    ? menuArmorSlot(wrapper.getSlot())
                    : null;
            EquipmentMasker.ItemMaskResult result = actualSlot == null
                    ? EquipmentMasker.maskInventoryItem(wrapper.getItem(), setting)
                    : EquipmentMasker.maskInventoryItem(wrapper.getItem(), actualSlot, setting);
            if (result.changed()) {
                wrapper.setItem(result.item());
                event.markForReEncode(true);
            }
            return;
        }

        if (event.getPacketType() == PacketType.Play.Server.SET_CURSOR_ITEM) {
            WrapperPlayServerSetCursorItem wrapper = new WrapperPlayServerSetCursorItem(event);
            EquipmentMasker.ItemMaskResult result =
                    EquipmentMasker.maskInventoryItem(wrapper.getStack(), setting);
            if (result.changed()) {
                wrapper.setStack(result.item());
                event.markForReEncode(true);
            }
            return;
        }

        if (event.getPacketType() == PacketType.Play.Server.WINDOW_ITEMS) {
            WrapperPlayServerWindowItems wrapper = new WrapperPlayServerWindowItems(event);
            List<ItemStack> source = wrapper.getItems();
            List<ItemStack> masked = new ArrayList<>(source.size());
            boolean changed = false;
            for (int index = 0; index < source.size(); index++) {
                ItemStack item = source.get(index);
                EquipmentSlot actualSlot = wrapper.getWindowId() == 0
                        ? menuArmorSlot(index)
                        : null;
                EquipmentMasker.ItemMaskResult result = actualSlot == null
                        ? EquipmentMasker.maskInventoryItem(item, setting)
                        : EquipmentMasker.maskInventoryItem(item, actualSlot, setting);
                masked.add(result.changed() ? result.item() : item);
                changed |= result.changed();
            }

            ItemStack carried = wrapper.getCarriedItem().orElse(null);
            EquipmentMasker.ItemMaskResult carriedResult =
                    EquipmentMasker.maskInventoryItem(carried, setting);
            if (carriedResult.changed()) {
                wrapper.setCarriedItem(carriedResult.item());
                changed = true;
            }

            if (changed) {
                wrapper.setItems(List.copyOf(masked));
                event.markForReEncode(true);
            }
        }
    }

    private void scheduleSelfInventoryCorrection(
            PacketSendEvent event,
            ArmorVisibilitySetting setting
    ) {
        if (event.getPacketType() != PacketType.Play.Server.SET_PLAYER_INVENTORY
                && event.getPacketType() != PacketType.Play.Server.SET_SLOT
                && event.getPacketType() != PacketType.Play.Server.WINDOW_ITEMS) {
            return;
        }

        UUID playerId = event.getUser().getUUID();
        List<Equipment> correction = inventoryCorrection(event, setting);
        if (correction.isEmpty()) {
            return;
        }

        User user = event.getUser();
        int entityId = user.getEntityId();
        long generation = visibilityManager.generation(playerId);
        List<Equipment> detachedCorrection = List.copyOf(correction);
        event.getTasksAfterSend().add(() -> {
            if (!visibilityManager.isCurrentMasking(playerId, generation)) {
                return;
            }
            user.sendPacket(new WrapperPlayServerEntityEquipment(entityId, detachedCorrection));
        });
    }

    private static List<Equipment> inventoryCorrection(
            PacketSendEvent event,
            ArmorVisibilitySetting setting
    ) {
        if (event.getPacketType() == PacketType.Play.Server.SET_PLAYER_INVENTORY) {
            WrapperPlayServerSetPlayerInventory wrapper = new WrapperPlayServerSetPlayerInventory(event);
            EquipmentSlot equipmentSlot = playerInventorySlot(wrapper.getSlot());
            return equipmentSlot == null
                    ? List.of()
                    : List.of(EquipmentMasker.maskSingle(
                            equipmentSlot,
                            wrapper.getStack(),
                            EquipmentMasker.MaskMode.AIR,
                            setting
                    ));
        }

        if (event.getPacketType() == PacketType.Play.Server.SET_SLOT) {
            WrapperPlayServerSetSlot wrapper = new WrapperPlayServerSetSlot(event);
            if (wrapper.getWindowId() != 0) {
                return List.of();
            }
            EquipmentSlot equipmentSlot = menuArmorSlot(wrapper.getSlot());
            return equipmentSlot == null
                    ? List.of()
                    : List.of(EquipmentMasker.maskSingle(
                            equipmentSlot,
                            wrapper.getItem(),
                            EquipmentMasker.MaskMode.AIR,
                            setting
                    ));
        }

        if (event.getPacketType() == PacketType.Play.Server.WINDOW_ITEMS) {
            WrapperPlayServerWindowItems wrapper = new WrapperPlayServerWindowItems(event);
            if (wrapper.getWindowId() != 0 || wrapper.getItems().size() <= 8) {
                return List.of();
            }

            List<ItemStack> items = wrapper.getItems();
            List<Equipment> correction = new ArrayList<>(4);
            correction.add(EquipmentMasker.maskSingle(
                    EquipmentSlot.BOOTS, items.get(8), EquipmentMasker.MaskMode.AIR, setting));
            correction.add(EquipmentMasker.maskSingle(
                    EquipmentSlot.LEGGINGS, items.get(7), EquipmentMasker.MaskMode.AIR, setting));
            correction.add(EquipmentMasker.maskSingle(
                    EquipmentSlot.CHEST_PLATE, items.get(6), EquipmentMasker.MaskMode.AIR, setting));
            correction.add(EquipmentMasker.maskSingle(
                    EquipmentSlot.HELMET, items.get(5), EquipmentMasker.MaskMode.AIR, setting));
            return correction;
        }

        return List.of();
    }

    static EquipmentSlot menuArmorSlot(int slot) {
        return switch (slot) {
            case 5 -> EquipmentSlot.HELMET;
            case 6 -> EquipmentSlot.CHEST_PLATE;
            case 7 -> EquipmentSlot.LEGGINGS;
            case 8 -> EquipmentSlot.BOOTS;
            default -> null;
        };
    }

    static EquipmentSlot playerInventorySlot(int slot) {
        return switch (slot) {
            case 36 -> EquipmentSlot.BOOTS;
            case 37 -> EquipmentSlot.LEGGINGS;
            case 38 -> EquipmentSlot.CHEST_PLATE;
            case 39 -> EquipmentSlot.HELMET;
            default -> null;
        };
    }
}
