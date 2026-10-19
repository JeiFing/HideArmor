package cn.jeifing.hidearmor;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.PacketEventsAPI;
import com.github.retrooper.packetevents.injector.ChannelInjector;
import com.github.retrooper.packetevents.manager.player.PlayerManager;
import com.github.retrooper.packetevents.manager.protocol.ProtocolManager;
import com.github.retrooper.packetevents.manager.server.ServerManager;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.netty.NettyManager;
import com.github.retrooper.packetevents.protocol.component.ComponentTypes;
import com.github.retrooper.packetevents.protocol.component.builtin.item.ItemEquippable;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.item.type.ItemType;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.nbt.NBTCompound;
import com.github.retrooper.packetevents.protocol.nbt.NBTString;
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot;
import com.github.retrooper.packetevents.resources.ResourceLocation;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EquipmentMaskerTest {
    private static PacketEventsAPI<?> previousApi;

    @BeforeAll
    static void installPacketEventsTestApi() {
        previousApi = PacketEvents.getAPI();
        PacketEvents.setAPI(new TestPacketEventsApi());
    }

    @AfterAll
    static void restorePacketEventsApi() {
        PacketEvents.setAPI(previousApi);
    }

    @Test
    void hidesAnyNonEmptyItemInSelectedArmorSlots() {
        assertTrue(EquipmentMasker.shouldHide(
                EquipmentSlot.HELMET, item(ItemTypes.IRON_HELMET),
                ArmorVisibilitySetting.ALL_HIDDEN));
        assertTrue(EquipmentMasker.shouldHide(
                EquipmentSlot.CHEST_PLATE, item(ItemTypes.STONE),
                ArmorVisibilitySetting.ALL_HIDDEN));
        assertTrue(EquipmentMasker.shouldHide(
                EquipmentSlot.LEGGINGS, item(ItemTypes.PLAYER_HEAD),
                ArmorVisibilitySetting.ALL_HIDDEN));
        assertTrue(EquipmentMasker.shouldHide(
                EquipmentSlot.BOOTS, item(ItemTypes.STICK),
                ArmorVisibilitySetting.ALL_HIDDEN));
    }

    @Test
    void keepsElytraVisible() {
        ItemStack elytra = item(ItemTypes.ELYTRA);
        assertFalse(EquipmentMasker.shouldHide(
                EquipmentSlot.CHEST_PLATE, elytra, ArmorVisibilitySetting.ALL_HIDDEN));
        EquipmentMasker.ItemMaskResult masked = EquipmentMasker.maskInventoryItem(
                elytra, EquipmentSlot.CHEST_PLATE, ArmorVisibilitySetting.ALL_HIDDEN);
        assertFalse(masked.changed());
        assertTrue(masked.item().is(ItemTypes.ELYTRA));
    }

    @Test
    void leavesHandsAndNonPlayerArmorSlotsUntouched() {
        ItemStack chestplate = item(ItemTypes.DIAMOND_CHESTPLATE);
        assertFalse(EquipmentMasker.shouldHide(
                EquipmentSlot.MAIN_HAND, chestplate, ArmorVisibilitySetting.ALL_HIDDEN));
        assertFalse(EquipmentMasker.shouldHide(
                EquipmentSlot.OFF_HAND, chestplate, ArmorVisibilitySetting.ALL_HIDDEN));
        assertFalse(EquipmentMasker.shouldHide(
                EquipmentSlot.BODY, chestplate, ArmorVisibilitySetting.ALL_HIDDEN));
        assertFalse(EquipmentMasker.shouldHide(
                EquipmentSlot.SADDLE, chestplate, ArmorVisibilitySetting.ALL_HIDDEN));
    }

    @Test
    void respectsPerSlotVisibilityMask() {
        ArmorVisibilitySetting setting = ArmorVisibilitySetting.fromCode("0100");
        ItemStack item = item(ItemTypes.STONE);

        assertTrue(EquipmentMasker.shouldHide(EquipmentSlot.HELMET, item, setting));
        assertFalse(EquipmentMasker.shouldHide(EquipmentSlot.CHEST_PLATE, item, setting));
        assertTrue(EquipmentMasker.shouldHide(EquipmentSlot.LEGGINGS, item, setting));
        assertTrue(EquipmentMasker.shouldHide(EquipmentSlot.BOOTS, item, setting));
    }

    @Test
    void allVisibleMaskLeavesEveryItemUntouched() {
        ItemStack chestplate = item(ItemTypes.DIAMOND_CHESTPLATE);
        assertFalse(EquipmentMasker.shouldHide(
                EquipmentSlot.CHEST_PLATE, chestplate, ArmorVisibilitySetting.ALL_VISIBLE));
        assertFalse(EquipmentMasker.maskInventoryItem(
                chestplate, ArmorVisibilitySetting.ALL_VISIBLE).changed());
    }

    @Test
    void ordinaryArmorUsesNonEmptySaddleViewAndRoundTripsExactly() {
        ItemStack original = item(ItemTypes.DIAMOND_CHESTPLATE);
        NBTCompound customData = new NBTCompound();
        customData.setTag("owner", new NBTString("OtherPlugin"));
        customData.setTag("hidearmor:client_view_v1", new NBTString("existing-user-value"));
        original.setComponent(ComponentTypes.CUSTOM_DATA, customData);
        ItemEquippable originalEquippable = requireEquippable(original);
        original.setComponent(ComponentTypes.EQUIPPABLE,
                withAsset(originalEquippable, new ResourceLocation("hidearmor:test_asset")));
        ItemStack untouchedCopy = original.copy();

        EquipmentMasker.ItemMaskResult masked = EquipmentMasker.maskInventoryItem(
                original, ArmorVisibilitySetting.ALL_HIDDEN);
        assertTrue(masked.changed());
        assertFalse(masked.item().isEmpty());
        assertTrue(masked.item().is(ItemTypes.DIAMOND_CHESTPLATE));
        assertEquals("minecraft:saddle", requireEquippable(masked.item()).getAssetId().toString());
        assertTrue(ItemStack.isSameItemSameComponents(original, untouchedCopy));

        EquipmentMasker.ItemMaskResult maskedAgain =
                EquipmentMasker.maskInventoryItem(
                        masked.item(), ArmorVisibilitySetting.ALL_HIDDEN);
        assertTrue(maskedAgain.changed());
        assertTrue(ItemStack.isSameItemSameComponents(masked.item(), maskedAgain.item()));

        EquipmentMasker.ItemMaskResult restored =
                EquipmentMasker.restoreCreativeItem(masked.item());
        assertTrue(restored.changed());
        assertEquivalent(original, restored.item());
        assertEquals("existing-user-value", restored.item()
                .getComponent(ComponentTypes.CUSTOM_DATA)
                .orElseThrow()
                .getStringTagValueOrNull("hidearmor:client_view_v1"));
    }

    @Test
    void restoresNullAndPreexistingSaddleAssets() {
        ItemStack nullAsset = item(ItemTypes.IRON_HELMET);
        nullAsset.setComponent(ComponentTypes.EQUIPPABLE,
                withAsset(requireEquippable(nullAsset), null));
        EquipmentMasker.ItemMaskResult nullMasked = EquipmentMasker.maskInventoryItem(
                nullAsset, ArmorVisibilitySetting.ALL_HIDDEN);
        EquipmentMasker.ItemMaskResult nullRestored =
                EquipmentMasker.restoreCreativeItem(nullMasked.item());
        assertTrue(nullRestored.changed());
        assertNull(requireEquippable(nullRestored.item()).getAssetId());
        assertEquivalent(nullAsset, nullRestored.item());

        ItemStack saddleAsset = item(ItemTypes.GOLDEN_BOOTS);
        saddleAsset.setComponent(ComponentTypes.EQUIPPABLE,
                withAsset(requireEquippable(saddleAsset), new ResourceLocation("minecraft:saddle")));
        EquipmentMasker.ItemMaskResult saddleMasked = EquipmentMasker.maskInventoryItem(
                saddleAsset, ArmorVisibilitySetting.ALL_HIDDEN);
        EquipmentMasker.ItemMaskResult saddleRestored =
                EquipmentMasker.restoreCreativeItem(saddleMasked.item());
        assertTrue(saddleRestored.changed());
        assertEquals("minecraft:saddle", requireEquippable(saddleRestored.item()).getAssetId().toString());
        assertEquivalent(saddleAsset, saddleRestored.item());
    }

    @Test
    void skullsAndBlockItemsCanBeHidden() {
        ItemStack playerHead = item(ItemTypes.PLAYER_HEAD);
        assertTrue(EquipmentMasker.shouldHide(
                EquipmentSlot.HELMET, playerHead, ArmorVisibilitySetting.ALL_HIDDEN));
        EquipmentMasker.ItemMaskResult headView = EquipmentMasker.maskInventoryItem(
                playerHead, EquipmentSlot.HELMET, ArmorVisibilitySetting.ALL_HIDDEN);
        assertTrue(headView.changed());
        assertTrue(headView.item().is(ItemTypes.CARVED_PUMPKIN));
        assertEquivalent(playerHead, EquipmentMasker.restoreCreativeItem(headView.item()).item());

        ItemStack stone = item(ItemTypes.STONE);
        stone.setComponent(
                ComponentTypes.EQUIPPABLE,
                requireEquippable(item(ItemTypes.CARVED_PUMPKIN))
        );
        assertTrue(EquipmentMasker.shouldHide(
                EquipmentSlot.HELMET, stone, ArmorVisibilitySetting.ALL_HIDDEN));
        assertTrue(EquipmentMasker.maskInventoryItem(
                stone, EquipmentSlot.HELMET, ArmorVisibilitySetting.ALL_HIDDEN).changed());
    }

    @Test
    void identicalArmorProducesIdenticalClientViewsForTweakerooMatching() {
        ItemStack first = item(ItemTypes.NETHERITE_CHESTPLATE);
        ItemStack second = first.copy();

        ItemStack firstView = EquipmentMasker.maskInventoryItem(
                first, ArmorVisibilitySetting.ALL_HIDDEN).item();
        ItemStack secondView = EquipmentMasker.maskInventoryItem(
                second, ArmorVisibilitySetting.ALL_HIDDEN).item();
        assertTrue(ItemStack.isSameItemSameComponents(firstView, secondView));
    }

    @Test
    void unmarkedSaddleAssetIsNeverRestored() {
        ItemStack original = item(ItemTypes.IRON_LEGGINGS);
        original.setComponent(ComponentTypes.EQUIPPABLE,
                withAsset(requireEquippable(original), new ResourceLocation("minecraft:saddle")));

        EquipmentMasker.ItemMaskResult restored = EquipmentMasker.restoreCreativeItem(original);
        assertFalse(restored.changed());
        assertTrue(restored.item() == original);
    }

    @Test
    void incompleteLookalikeMarkerIsNeverTrusted() {
        ItemStack original = item(ItemTypes.IRON_HELMET);
        original.setComponent(ComponentTypes.EQUIPPABLE,
                withAsset(requireEquippable(original), new ResourceLocation("minecraft:saddle")));
        NBTCompound lookalike = new NBTCompound();
        NBTCompound marker = new NBTCompound();
        marker.setTag("format", new NBTString("HideArmor/1"));
        lookalike.setTag("hidearmor:client_view_v1", marker);
        original.setComponent(ComponentTypes.CUSTOM_DATA, lookalike);

        EquipmentMasker.ItemMaskResult restored = EquipmentMasker.restoreCreativeItem(original);
        assertFalse(restored.changed());
        assertTrue(restored.item() == original);
    }

    private static ItemStack item(ItemType type) {
        return ItemStack.builder()
                .type(type)
                .amount(1)
                .version(ServerVersion.V_26_3.toClientVersion())
                .build();
    }

    private static ItemEquippable requireEquippable(ItemStack item) {
        ItemEquippable equippable = item.getComponent(ComponentTypes.EQUIPPABLE).orElse(null);
        assertNotNull(equippable, () -> "Expected equippable component on " + item.getType().getName());
        return equippable;
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

    private static void assertEquivalent(ItemStack expected, ItemStack actual) {
        assertEquals(expected.getType(), actual.getType());
        assertEquals(expected.getAmount(), actual.getAmount());
        assertEquals(expected.getLegacyData(), actual.getLegacyData());
        assertEquals(expected.getNBT(), actual.getNBT());
        assertTrue(ItemStack.isSameItemSameComponents(expected, actual),
                () -> "Expected " + expected + " but got " + actual);
    }

    private static final class TestPacketEventsApi extends PacketEventsAPI<Object> {
        private final ServerManager serverManager = () -> ServerVersion.V_26_3;
        private final NettyManager nettyManager =
                new io.github.retrooper.packetevents.impl.netty.NettyManagerImpl();

        @Override
        public boolean isLoaded() {
            return true;
        }

        @Override
        public void init() {
        }

        @Override
        public boolean isInitialized() {
            return true;
        }

        @Override
        public boolean isTerminated() {
            return false;
        }

        @Override
        public Object getPlugin() {
            return this;
        }

        @Override
        public ServerManager getServerManager() {
            return serverManager;
        }

        @Override
        public ProtocolManager getProtocolManager() {
            return null;
        }

        @Override
        public PlayerManager getPlayerManager() {
            return null;
        }

        @Override
        public NettyManager getNettyManager() {
            return nettyManager;
        }

        @Override
        public ChannelInjector getInjector() {
            return null;
        }
    }
}
