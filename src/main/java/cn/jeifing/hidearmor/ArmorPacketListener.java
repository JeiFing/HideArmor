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

import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnPlayer;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities;
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes;
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
                visibilityManager.isBedrock(event.getUser())
                        ? BedrockArmorView.restore(wrapper.getItemStack())
                        : EquipmentMasker.restoreCreativeItem(wrapper.getItemStack());
        if (result.changed()) {
            wrapper.setItemStack(result.item());
            event.markForReEncode(true);
        }
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        UUID viewer=event.getUser().getUUID();
        if(viewer!=null){
            if(event.getPacketType()==PacketType.Play.Server.PLAYER_INFO_UPDATE){
                var packet=new WrapperPlayServerPlayerInfoUpdate(event);
                if(packet.getActions().contains(WrapperPlayServerPlayerInfoUpdate.Action.ADD_PLAYER))for(var entry:packet.getEntries()){
                    var profile=entry.getGameProfile();
                    if(profile!=null)visibilityManager.poseClones.profile(viewer,entry.getProfileId(),profile.getName());
                }
            }else if(event.getPacketType()==PacketType.Play.Server.SPAWN_ENTITY){
                var packet=new WrapperPlayServerSpawnEntity(event);
                if(packet.getEntityType()==EntityTypes.PLAYER)packet.getUUID().ifPresent(id->visibilityManager.poseClones.spawn(viewer,packet.getEntityId(),id));
            }else if(event.getPacketType()==PacketType.Play.Server.SPAWN_PLAYER){
                var packet=new WrapperPlayServerSpawnPlayer(event);visibilityManager.poseClones.spawn(viewer,packet.getEntityId(),packet.getUUID());
            }else if(event.getPacketType()==PacketType.Play.Server.DESTROY_ENTITIES){
                visibilityManager.poseClones.destroy(viewer,new WrapperPlayServerDestroyEntities(event).getEntityIds());
            }else if(event.getPacketType()==PacketType.Play.Server.RESPAWN){visibilityManager.poseClones.viewerLeft(viewer);}
        }
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

        if (visibilityManager.shouldUseModelLessView(event.getUser()) || visibilityManager.isBedrock(event.getUser())) {
            maskSelfInventoryPacket(event, setting, visibilityManager.isBedrock(event.getUser()));
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

        if(targetId==null)targetId=visibilityManager.poseClones.owner(viewerId,wrapper.getEntityId());
        if (targetId == null) {
            return;
        }
        ArmorVisibilitySetting setting = visibilityManager.setting(targetId);
        if (!setting.hasHiddenSlots()) {
            return;
        }

        EquipmentMasker.MaskMode mode = wrapper.getEntityId()==event.getUser().getEntityId() && targetId.equals(viewerId) && visibilityManager.isBedrock(event.getUser())
                ? EquipmentMasker.MaskMode.BEDROCK_MODEL
                : wrapper.getEntityId()==event.getUser().getEntityId() && targetId.equals(viewerId)
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
            ArmorVisibilitySetting setting, boolean bedrock
    ) {
        if (event.getPacketType() == PacketType.Play.Server.SET_PLAYER_INVENTORY) {
            WrapperPlayServerSetPlayerInventory wrapper = new WrapperPlayServerSetPlayerInventory(event);
            EquipmentSlot actualSlot = playerInventorySlot(wrapper.getSlot());
            EquipmentMasker.ItemMaskResult result = actualSlot == null
                    ? maskInventoryView(bedrock, wrapper.getStack(), setting)
                    : maskInventoryView(bedrock, wrapper.getStack(), actualSlot, setting);
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
                    ? maskInventoryView(bedrock, wrapper.getItem(), setting)
                    : maskInventoryView(bedrock, wrapper.getItem(), actualSlot, setting);
            if (result.changed()) {
                wrapper.setItem(result.item());
                event.markForReEncode(true);
            }
            return;
        }

        if (event.getPacketType() == PacketType.Play.Server.SET_CURSOR_ITEM) {
            WrapperPlayServerSetCursorItem wrapper = new WrapperPlayServerSetCursorItem(event);
            EquipmentMasker.ItemMaskResult result =
                    maskInventoryView(bedrock, wrapper.getStack(), setting);
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
                        ? maskInventoryView(bedrock, item, setting)
                        : maskInventoryView(bedrock, item, actualSlot, setting);
                masked.add(result.changed() ? result.item() : item);
                changed |= result.changed();
            }

            ItemStack carried = wrapper.getCarriedItem().orElse(null);
            EquipmentMasker.ItemMaskResult carriedResult =
                    maskInventoryView(bedrock, carried, setting);
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

    private static EquipmentMasker.ItemMaskResult maskInventoryView(boolean bedrock, ItemStack item, ArmorVisibilitySetting setting) {
        return bedrock ? BedrockArmorView.mask(item, setting) : EquipmentMasker.maskInventoryItem(item, setting);
    }
    private static EquipmentMasker.ItemMaskResult maskInventoryView(boolean bedrock, ItemStack item, EquipmentSlot slot, ArmorVisibilitySetting setting) {
        return bedrock ? BedrockArmorView.mask(item, slot, setting) : EquipmentMasker.maskInventoryItem(item, slot, setting);
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
