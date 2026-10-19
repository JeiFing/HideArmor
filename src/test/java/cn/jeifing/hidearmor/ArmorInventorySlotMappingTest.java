package cn.jeifing.hidearmor;

import com.github.retrooper.packetevents.protocol.player.EquipmentSlot;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ArmorInventorySlotMappingTest {
    @Test
    void mapsWindowZeroArmorSlots() {
        assertEquals(EquipmentSlot.HELMET, ArmorPacketListener.menuArmorSlot(5));
        assertEquals(EquipmentSlot.CHEST_PLATE, ArmorPacketListener.menuArmorSlot(6));
        assertEquals(EquipmentSlot.LEGGINGS, ArmorPacketListener.menuArmorSlot(7));
        assertEquals(EquipmentSlot.BOOTS, ArmorPacketListener.menuArmorSlot(8));
        assertNull(ArmorPacketListener.menuArmorSlot(4));
        assertNull(ArmorPacketListener.menuArmorSlot(9));
    }

    @Test
    void mapsSetPlayerInventoryArmorSlots() {
        assertEquals(EquipmentSlot.BOOTS, ArmorPacketListener.playerInventorySlot(36));
        assertEquals(EquipmentSlot.LEGGINGS, ArmorPacketListener.playerInventorySlot(37));
        assertEquals(EquipmentSlot.CHEST_PLATE, ArmorPacketListener.playerInventorySlot(38));
        assertEquals(EquipmentSlot.HELMET, ArmorPacketListener.playerInventorySlot(39));
        assertNull(ArmorPacketListener.playerInventorySlot(35));
        assertNull(ArmorPacketListener.playerInventorySlot(40));
    }
}
