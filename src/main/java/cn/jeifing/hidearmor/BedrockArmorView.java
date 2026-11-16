package cn.jeifing.hidearmor;

import com.github.retrooper.packetevents.protocol.component.ComponentTypes;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot;
import com.github.retrooper.packetevents.resources.ResourceLocation;

/** Per-recipient models for the documented Geyser v2 custom-item mapping API. */
final class BedrockArmorView {
    private static final String PREFIX = "hidearmor:hidden_";
    private BedrockArmorView() { }

    static boolean supported(ItemStack item) {
        if (item == null || item.isEmpty()) return false;
        String type = item.getType().getName().toString();
        return type.matches("minecraft:(?:(?:leather|chainmail|copper|iron|golden|diamond|netherite)_(?:helmet|chestplate|leggings|boots)|turtle_helmet)");
    }

    static EquipmentMasker.ItemMaskResult mask(ItemStack item, EquipmentSlot slot,
                                               ArmorVisibilitySetting setting) {
        if (!EquipmentMasker.shouldHide(slot, item, setting) || !supported(item))
            return new EquipmentMasker.ItemMaskResult(item, false);
        var modelValue = item.getComponent(ComponentTypes.ITEM_MODEL).orElse(null);
        ResourceLocation model = modelValue == null ? null : modelValue.getModelLocation();
        String expected = item.getType().getName().toString();
        String hidden = PREFIX + expected.substring("minecraft:".length());
        // A custom chestplate (for example a music backpack) retains its own model.
        if (model == null || !model.toString().equals(expected) && !model.toString().equals(hidden))
            return new EquipmentMasker.ItemMaskResult(item, false);
        ItemStack view = item.copy();
        view.setComponent(ComponentTypes.ITEM_MODEL, new com.github.retrooper.packetevents.protocol.component.builtin.item.ItemModel(new ResourceLocation(hidden)));
        return new EquipmentMasker.ItemMaskResult(view, true);
    }

    static EquipmentMasker.ItemMaskResult mask(ItemStack item, ArmorVisibilitySetting setting) {
        if (item == null || item.isEmpty()) return new EquipmentMasker.ItemMaskResult(item, false);
        var equipped = item.getComponent(ComponentTypes.EQUIPPABLE).orElse(null);
        return equipped == null ? new EquipmentMasker.ItemMaskResult(item, false)
                : mask(item, equipped.getSlot(), setting);
    }

    static EquipmentMasker.ItemMaskResult restore(ItemStack item) {
        if (!supported(item)) return new EquipmentMasker.ItemMaskResult(item, false);
        var modelValue = item.getComponent(ComponentTypes.ITEM_MODEL).orElse(null);
        ResourceLocation model = modelValue == null ? null : modelValue.getModelLocation();
        String expected = PREFIX + item.getType().getName().toString().substring("minecraft:".length());
        if (model == null || !model.toString().equals(expected))
            return new EquipmentMasker.ItemMaskResult(item, false);
        ItemStack restored = item.copy();
        restored.setComponent(ComponentTypes.ITEM_MODEL, new com.github.retrooper.packetevents.protocol.component.builtin.item.ItemModel(item.getType().getName()));
        return new EquipmentMasker.ItemMaskResult(restored, true);
    }
}
