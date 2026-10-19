package cn.jeifing.hidearmor;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VisibilityStateCodecTest {
    private static final UUID FIRST = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID SECOND = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test
    void migratesLegacyHiddenPlayersAndLetsNewSettingsOverrideThem() {
        Map<String, String> current = new LinkedHashMap<>();
        current.put(FIRST.toString(), "1111");
        current.put(SECOND.toString(), "0100");
        List<String> warnings = new ArrayList<>();

        Map<UUID, ArmorVisibilitySetting> decoded = VisibilityStateCodec.decode(
                List.of(FIRST.toString(), SECOND.toString()),
                current,
                warnings::add
        );

        assertFalse(decoded.containsKey(FIRST));
        assertEquals("0100", decoded.get(SECOND).code());
        assertTrue(warnings.isEmpty());
    }

    @Test
    void ignoresInvalidEntriesWithoutLosingOtherSettings() {
        List<String> warnings = new ArrayList<>();
        Map<UUID, ArmorVisibilitySetting> decoded = VisibilityStateCodec.decode(
                List.of(FIRST.toString(), "not-a-uuid"),
                Map.of(SECOND.toString(), "0123"),
                warnings::add
        );

        assertEquals(ArmorVisibilitySetting.ALL_HIDDEN, decoded.get(FIRST));
        assertFalse(decoded.containsKey(SECOND));
        assertEquals(2, warnings.size());
    }

    @Test
    void encodesOnlyHiddenSettingsInStableUuidOrder() {
        Map<UUID, ArmorVisibilitySetting> settings = new LinkedHashMap<>();
        settings.put(SECOND, ArmorVisibilitySetting.fromCode("0100"));
        settings.put(FIRST, ArmorVisibilitySetting.ALL_HIDDEN);
        settings.put(UUID.fromString("00000000-0000-0000-0000-000000000003"),
                ArmorVisibilitySetting.ALL_VISIBLE);

        Map<String, String> encoded = VisibilityStateCodec.encode(settings);

        assertEquals(List.of(FIRST.toString(), SECOND.toString()),
                new ArrayList<>(encoded.keySet()));
        assertEquals("0000", encoded.get(FIRST.toString()));
        assertEquals("0100", encoded.get(SECOND.toString()));
    }
}
