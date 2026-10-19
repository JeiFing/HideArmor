package cn.jeifing.hidearmor;

import com.github.retrooper.packetevents.protocol.component.ComponentTypes;
import com.github.retrooper.packetevents.protocol.component.ComponentType;
import com.github.retrooper.packetevents.protocol.component.PatchableComponentMap;
import com.github.retrooper.packetevents.protocol.component.builtin.item.ItemEquippable;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.nbt.NBT;
import com.github.retrooper.packetevents.protocol.nbt.NBTByte;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.nbt.NBTString;
import com.github.retrooper.packetevents.protocol.player.Equipment;
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot;
import com.github.retrooper.packetevents.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

final class EquipmentMasker {
    private static final ResourceLocation HIDDEN_EQUIPMENT_ASSET =
            new ResourceLocation("minecraft:saddle");
    private static final String MARKER_KEY = "hidearmor:client_view_v1";
    private static final String FORMAT_KEY = "format";
    private static final String FORMAT_VALUE = "HideArmor/1";
    private static final String HAD_ASSET_KEY = "had_asset";
    private static final String ASSET_KEY = "asset";
    private static final String HAD_CUSTOM_DATA_KEY = "had_custom_data";
    private static final String HAD_PREVIOUS_MARKER_KEY = "had_previous_marker";
    private static final String PREVIOUS_MARKER_KEY = "previous_marker";
    private static final String HAD_TYPE_OVERRIDE_KEY = "had_type_override";
    private static final String ORIGINAL_TYPE_KEY = "original_type";

    private EquipmentMasker() {
    }

    static MaskResult mask(
            List<Equipment> source,
            MaskMode mode,
            ArmorVisibilitySetting setting
    ) {
        List<Equipment> masked = new ArrayList<>(source.size());
        boolean changed = false;

        for (Equipment equipment : source) {
            EquipmentSlot slot = equipment.getSlot();
            ItemStack original = equipment.getItem();
            ItemStack replacement = original;

            if (shouldHide(slot, original, setting)) {
                if (mode == MaskMode.MODEL_LESS) {
                    ItemMaskResult view = makeClientView(original, slot);
                    replacement = view.changed() ? view.item() : ItemStack.EMPTY;
                } else {
                    replacement = ItemStack.EMPTY;
                }
                changed = true;
            }

            masked.add(new Equipment(slot, replacement));
        }

        return new MaskResult(List.copyOf(masked), changed);
    }

    static Equipment maskSingle(
            EquipmentSlot slot,
            ItemStack original,
            MaskMode mode,
            ArmorVisibilitySetting setting
    ) {
        if (!shouldHide(slot, original, setting)) {
            return new Equipment(slot, copyOrEmpty(original));
        }
        if (mode == MaskMode.AIR) {
            return new Equipment(slot, ItemStack.EMPTY);
        }

        ItemMaskResult view = makeClientView(original, slot);
        return new Equipment(slot, view.changed() ? view.item() : ItemStack.EMPTY);
    }

    static ItemMaskResult maskInventoryItem(
            ItemStack original,
            ArmorVisibilitySetting setting
    ) {
        if (original == null || original.isEmpty()) {
            return new ItemMaskResult(original, false);
        }

        ItemEquippable equippable = original.getComponent(ComponentTypes.EQUIPPABLE).orElse(null);
        EquipmentSlot logicalSlot = equippable == null
                ? null
                : equippable.getSlot();
        if (logicalSlot == null) {
            return new ItemMaskResult(original, false);
        }
        return maskInventoryItem(original, logicalSlot, setting);
    }

    static ItemMaskResult maskInventoryItem(
            ItemStack original,
            EquipmentSlot actualSlot,
            ArmorVisibilitySetting setting
    ) {
        if (!shouldHide(actualSlot, original, setting)) {
            return new ItemMaskResult(original, false);
        }
        return makeClientView(original, actualSlot);
    }

    static ItemMaskResult restoreCreativeItem(ItemStack incoming) {
        if (incoming == null || incoming.isEmpty()) {
            return new ItemMaskResult(incoming, false);
        }

        NBTCompound customData = incoming.getComponent(ComponentTypes.CUSTOM_DATA).orElse(null);
        NBTCompound marker = marker(customData);
        ItemEquippable equippable = incoming.getComponent(ComponentTypes.EQUIPPABLE).orElse(null);
        if (marker == null
                || equippable == null
                || !HIDDEN_EQUIPMENT_ASSET.equals(equippable.getAssetId())) {
            return new ItemMaskResult(incoming, false);
        }

        ResourceLocation originalAsset = null;
        if (marker.getBooleanOr(HAD_ASSET_KEY, false)) {
            String asset = marker.getStringTagValueOrNull(ASSET_KEY);
            if (asset == null) {
                return new ItemMaskResult(incoming, false);
            }
            try {
                originalAsset = new ResourceLocation(asset);
            } catch (RuntimeException ignored) {
                return new ItemMaskResult(incoming, false);
            }
        }

        ItemType originalType = incoming.getType();
        boolean hadTypeOverride = marker.getBooleanOr(HAD_TYPE_OVERRIDE_KEY, false);
        if (hadTypeOverride) {
            String originalTypeName = marker.getStringTagValueOrNull(ORIGINAL_TYPE_KEY);
            originalType = originalTypeName == null ? null : ItemTypes.getByName(originalTypeName);
            if (originalType == null
                    || !isSkullItem(originalType)
                    || !incoming.is(ItemTypes.CARVED_PUMPKIN)) {
                return new ItemMaskResult(incoming, false);
            }
        }

        ItemStack restored = incoming.copy();
        restored.setComponent(ComponentTypes.EQUIPPABLE, withAsset(equippable, originalAsset));

        NBTCompound restoredCustomData = customData.copy();
        restoredCustomData.removeTag(MARKER_KEY);
        if (marker.getBooleanOr(HAD_PREVIOUS_MARKER_KEY, false)) {
            NBT previousMarker = marker.getTagOrNull(PREVIOUS_MARKER_KEY);
            if (previousMarker != null) {
                restoredCustomData.setTag(MARKER_KEY, previousMarker.copy());
            }
        }

        if (!marker.getBooleanOr(HAD_CUSTOM_DATA_KEY, false) && restoredCustomData.isEmpty()) {
            restored.unsetComponent(ComponentTypes.CUSTOM_DATA);
        } else {
            restored.setComponent(ComponentTypes.CUSTOM_DATA, restoredCustomData);
        }

        if (hadTypeOverride) {
            restored = retypePreservingComponents(restored, originalType);
        }
        return new ItemMaskResult(restored, true);
    }

    static boolean shouldHide(
            EquipmentSlot slot,
            ItemStack item,
            ArmorVisibilitySetting setting
    ) {
        if (!isPlayerArmorSlot(slot)
                || setting == null
                || !setting.isHidden(slot)
                || item == null
                || item.isEmpty()
                || isElytra(item)) {
            return false;
        }
        return true;
    }

    private static boolean isPlayerArmorSlot(EquipmentSlot slot) {
        return slot == EquipmentSlot.HELMET
                || slot == EquipmentSlot.CHEST_PLATE
                || slot == EquipmentSlot.LEGGINGS
                || slot == EquipmentSlot.BOOTS;
    }

    private static ItemMaskResult makeClientView(ItemStack original, EquipmentSlot expectedSlot) {
        if (original == null || original.isEmpty() || isElytra(original)) {
            return new ItemMaskResult(original, false);
        }

        ItemEquippable equippable = original.getComponent(ComponentTypes.EQUIPPABLE).orElse(null);
        if (equippable == null || equippable.getSlot() != expectedSlot) {
            return new ItemMaskResult(original, false);
        }

        NBTCompound originalCustomData = original.getComponent(ComponentTypes.CUSTOM_DATA).orElse(null);
        if (HIDDEN_EQUIPMENT_ASSET.equals(equippable.getAssetId()) && marker(originalCustomData) != null) {
            return new ItemMaskResult(original.copy(), true);
        }

        boolean needsTypeOverride = isSkullItem(original.getType());
        ItemStack view = needsTypeOverride
                ? retypePreservingComponents(original, ItemTypes.CARVED_PUMPKIN)
                : original.copy();
        view.setComponent(ComponentTypes.EQUIPPABLE, withAsset(equippable, HIDDEN_EQUIPMENT_ASSET));

        boolean hadCustomData = original.hasComponent(ComponentTypes.CUSTOM_DATA);
        NBTCompound customData = originalCustomData == null
                ? new NBTCompound()
                : originalCustomData.copy();
        NBT previousMarker = customData.getTagOrNull(MARKER_KEY);

        NBTCompound marker = new NBTCompound();
        marker.setTag(FORMAT_KEY, new NBTString(FORMAT_VALUE));
        marker.setTag(HAD_ASSET_KEY, new NBTByte(equippable.getAssetId() != null));
        if (equippable.getAssetId() != null) {
            marker.setTag(ASSET_KEY, new NBTString(equippable.getAssetId().toString()));
        }
        marker.setTag(HAD_CUSTOM_DATA_KEY, new NBTByte(hadCustomData));
        marker.setTag(HAD_PREVIOUS_MARKER_KEY, new NBTByte(previousMarker != null));
        if (previousMarker != null) {
            marker.setTag(PREVIOUS_MARKER_KEY, previousMarker.copy());
        }
        marker.setTag(HAD_TYPE_OVERRIDE_KEY, new NBTByte(needsTypeOverride));
        if (needsTypeOverride) {
            marker.setTag(ORIGINAL_TYPE_KEY, new NBTString(original.getType().getName().toString()));
        }
        customData.setTag(MARKER_KEY, marker);
        view.setComponent(ComponentTypes.CUSTOM_DATA, customData);
        return new ItemMaskResult(view, true);
    }

    private static NBTCompound marker(NBTCompound customData) {
        if (customData == null) {
            return null;
        }
        NBTCompound marker = customData.getCompoundTagOrNull(MARKER_KEY);
        if (marker == null
                || !FORMAT_VALUE.equals(marker.getStringTagValueOrNull(FORMAT_KEY))
                || marker.getTagOfTypeOrNull(HAD_ASSET_KEY, NBTByte.class) == null
                || marker.getTagOfTypeOrNull(HAD_CUSTOM_DATA_KEY, NBTByte.class) == null
                || marker.getTagOfTypeOrNull(HAD_PREVIOUS_MARKER_KEY, NBTByte.class) == null
                || marker.getTagOfTypeOrNull(HAD_TYPE_OVERRIDE_KEY, NBTByte.class) == null) {
            return null;
        }
        if (marker.getBooleanOr(HAD_ASSET_KEY, false)
                && marker.getStringTagOrNull(ASSET_KEY) == null) {
            return null;
        }
        if (marker.getBooleanOr(HAD_PREVIOUS_MARKER_KEY, false)
                && marker.getTagOrNull(PREVIOUS_MARKER_KEY) == null) {
            return null;
        }
        if (marker.getBooleanOr(HAD_TYPE_OVERRIDE_KEY, false)
                && marker.getStringTagOrNull(ORIGINAL_TYPE_KEY) == null) {
            return null;
        }
        return marker;
    }

    private static ItemEquippable withAsset(ItemEquippable source, ResourceLocation asset) {
        return new ItemEquippable(
                source.getSlot(),
                source.getEquipSound(),
                asset,
                source.getCameraOverlay(),
                source.getAllowedEntities(),
                source.isDispensable(),
                source.isSwappable(),
                source.isDamageOnHurt(),
                source.isEquipOnInteract(),
                source.isCanBeSheared(),
                source.getShearingSound()
        );
    }

    private static ItemStack retypePreservingComponents(ItemStack source, ItemType newType) {
        PatchableComponentMap components = new PatchableComponentMap(
                newType.getComponents(source.getVersion()),
                source.getRegistryHolder()
        );
        for (ComponentType<?> componentType : ComponentTypes.values()) {
            copyEffectiveComponent(source, components, componentType);
        }

        return ItemStack.builder()
                .type(newType)
                .amount(source.getAmount())
                .nbt(source.getNBT() == null ? null : source.getNBT().copy())
                .components(components)
                .legacyData(source.getLegacyData())
                .version(source.getVersion())
                .registryHolder(source.getRegistryHolder())
                .build();
    }

    private static <T> void copyEffectiveComponent(
            ItemStack source,
            PatchableComponentMap target,
            ComponentType<T> componentType
    ) {
        target.set(componentType, source.getComponent(componentType));
    }

    private static boolean isSkullItem(ItemType itemType) {
        return itemType == ItemTypes.PLAYER_HEAD
                || itemType == ItemTypes.ZOMBIE_HEAD
                || itemType == ItemTypes.CREEPER_HEAD
                || itemType == ItemTypes.DRAGON_HEAD
                || itemType == ItemTypes.PIGLIN_HEAD
                || itemType == ItemTypes.SKELETON_SKULL
                || itemType == ItemTypes.WITHER_SKELETON_SKULL;
    }

    private static ItemStack copyOrEmpty(ItemStack item) {
        return item == null || item.isEmpty() ? ItemStack.EMPTY : item.copy();
    }

    private static boolean isElytra(ItemStack item) {
        return item != null && !item.isEmpty() && item.is(ItemTypes.ELYTRA);
    }

    enum MaskMode {
        AIR,
        MODEL_LESS
    }

    record MaskResult(List<Equipment> equipment, boolean changed) {
    }

    record ItemMaskResult(ItemStack item, boolean changed) {
    }
}
