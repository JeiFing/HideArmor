package cn.jeifing.hidearmor;

import com.github.retrooper.packetevents.protocol.player.EquipmentSlot;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArmorVisibilitySettingTest {
    @Test
    void parsesAliasesAndFourBinaryDigits() {
        assertSame(ArmorVisibilitySetting.ALL_HIDDEN,
                ArmorVisibilitySetting.fromCommandArgument("all"));
        assertSame(ArmorVisibilitySetting.ALL_HIDDEN,
                ArmorVisibilitySetting.fromCommandArgument("ALL"));
        assertSame(ArmorVisibilitySetting.ALL_VISIBLE,
                ArmorVisibilitySetting.fromCommandArgument("none"));
        assertSame(ArmorVisibilitySetting.ALL_VISIBLE,
                ArmorVisibilitySetting.fromCommandArgument("1111"));
        assertEquals("0100", ArmorVisibilitySetting.fromCommandArgument("0100").code());
        assertNull(ArmorVisibilitySetting.fromCommandArgument("101"));
        assertNull(ArmorVisibilitySetting.fromCommandArgument("10000"));
        assertNull(ArmorVisibilitySetting.fromCommandArgument("abcd"));
        assertNull(ArmorVisibilitySetting.fromCommandArgument(null));
    }

    @Test
    void mapsBitsToHelmetChestLeggingsAndBoots() {
        ArmorVisibilitySetting setting = ArmorVisibilitySetting.fromCode("0100");
        assertTrue(setting.isHidden(EquipmentSlot.HELMET));
        assertFalse(setting.isHidden(EquipmentSlot.CHEST_PLATE));
        assertTrue(setting.isHidden(EquipmentSlot.LEGGINGS));
        assertTrue(setting.isHidden(EquipmentSlot.BOOTS));
        assertFalse(setting.isHidden(EquipmentSlot.MAIN_HAND));
    }

    @Test
    void rendersEverySuccessMessageInFixedSlotOrder() {
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("0000", "已成功设置：隐藏所有盔甲（不含鞘翅）");
        expected.put("0001", "已成功设置：隐藏头盔、胸甲（不含鞘翅）、护腿；显示靴子");
        expected.put("0010", "已成功设置：隐藏头盔、胸甲（不含鞘翅）、靴子；显示护腿");
        expected.put("0011", "已成功设置：隐藏头盔、胸甲（不含鞘翅）；显示护腿、靴子");
        expected.put("0100", "已成功设置：隐藏头盔、护腿、靴子；显示胸甲");
        expected.put("0101", "已成功设置：隐藏头盔、护腿；显示胸甲、靴子");
        expected.put("0110", "已成功设置：隐藏头盔、靴子；显示胸甲、护腿");
        expected.put("0111", "已成功设置：隐藏头盔；显示胸甲、护腿、靴子");
        expected.put("1000", "已成功设置：隐藏胸甲（不含鞘翅）、护腿、靴子；显示头盔");
        expected.put("1001", "已成功设置：隐藏胸甲（不含鞘翅）、护腿；显示头盔、靴子");
        expected.put("1010", "已成功设置：隐藏胸甲（不含鞘翅）、靴子；显示头盔、护腿");
        expected.put("1011", "已成功设置：隐藏胸甲（不含鞘翅）；显示头盔、护腿、靴子");
        expected.put("1100", "已成功设置：隐藏护腿、靴子；显示头盔、胸甲");
        expected.put("1101", "已成功设置：隐藏护腿；显示头盔、胸甲、靴子");
        expected.put("1110", "已成功设置：隐藏靴子；显示头盔、胸甲、护腿");
        expected.put("1111", "已成功设置：显示所有盔甲");

        expected.forEach((code, message) -> assertEquals(
                message,
                ArmorVisibilitySetting.fromCode(code).successText(),
                code
        ));
    }
}
