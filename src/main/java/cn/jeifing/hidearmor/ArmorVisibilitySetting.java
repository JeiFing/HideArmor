package cn.jeifing.hidearmor;

import com.github.retrooper.packetevents.protocol.player.EquipmentSlot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

record ArmorVisibilitySetting(String code) {
    static final ArmorVisibilitySetting ALL_HIDDEN = new ArmorVisibilitySetting("0000");
    static final ArmorVisibilitySetting ALL_VISIBLE = new ArmorVisibilitySetting("1111");

    ArmorVisibilitySetting {
        if (code == null || !code.matches("[01]{4}")) {
            throw new IllegalArgumentException("Visibility code must contain exactly four binary digits");
        }
    }

    static ArmorVisibilitySetting fromCommandArgument(String argument) {
        if (argument == null) {
            return null;
        }

        return switch (argument.toLowerCase(Locale.ROOT)) {
            case "all" -> ALL_HIDDEN;
            case "none" -> ALL_VISIBLE;
            default -> fromCode(argument);
        };
    }

    static ArmorVisibilitySetting fromCode(String value) {
        if (value == null || !value.matches("[01]{4}")) {
            return null;
        }
        if (ALL_HIDDEN.code.equals(value)) {
            return ALL_HIDDEN;
        }
        if (ALL_VISIBLE.code.equals(value)) {
            return ALL_VISIBLE;
        }
        return new ArmorVisibilitySetting(value);
    }

    boolean isHidden(EquipmentSlot slot) {
        int index = slotIndex(slot);
        return index >= 0 && code.charAt(index) == '0';
    }

    boolean hasHiddenSlots() {
        return !equals(ALL_VISIBLE);
    }

    boolean isAllHidden() {
        return equals(ALL_HIDDEN);
    }

    boolean isAllVisible() {
        return equals(ALL_VISIBLE);
    }

    String successText() {
        if (isAllHidden()) {
            return "已成功设置：隐藏所有盔甲（不含鞘翅）";
        }
        if (isAllVisible()) {
            return "已成功设置：显示所有盔甲";
        }

        List<String> hidden = new ArrayList<>(4);
        List<String> visible = new ArrayList<>(4);
        addSlotDescription(EquipmentSlot.HELMET, "头盔", hidden, visible);
        addSlotDescription(EquipmentSlot.CHEST_PLATE, "胸甲", hidden, visible);
        addSlotDescription(EquipmentSlot.LEGGINGS, "护腿", hidden, visible);
        addSlotDescription(EquipmentSlot.BOOTS, "靴子", hidden, visible);
        return "已成功设置：隐藏" + String.join("、", hidden)
                + "；显示" + String.join("、", visible);
    }

    private void addSlotDescription(
            EquipmentSlot slot,
            String name,
            List<String> hidden,
            List<String> visible
    ) {
        if (isHidden(slot)) {
            hidden.add(slot == EquipmentSlot.CHEST_PLATE ? name + "（不含鞘翅）" : name);
        } else {
            visible.add(name);
        }
    }

    private static int slotIndex(EquipmentSlot slot) {
        if (slot == null) {
            return -1;
        }
        return switch (slot) {
            case HELMET -> 0;
            case CHEST_PLATE -> 1;
            case LEGGINGS -> 2;
            case BOOTS -> 3;
            default -> -1;
        };
    }
}
