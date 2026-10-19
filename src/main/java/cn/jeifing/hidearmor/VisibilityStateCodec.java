package cn.jeifing.hidearmor;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

final class VisibilityStateCodec {
    private VisibilityStateCodec() {
    }

    static Map<UUID, ArmorVisibilitySetting> decode(
            List<String> legacyHiddenPlayers,
            Map<String, String> visibilitySettings,
            Consumer<String> warningSink
    ) {
        Map<UUID, ArmorVisibilitySetting> decoded = new LinkedHashMap<>();
        for (String rawUuid : legacyHiddenPlayers) {
            UUID playerId = parseUuid(rawUuid, warningSink);
            if (playerId != null) {
                decoded.put(playerId, ArmorVisibilitySetting.ALL_HIDDEN);
            }
        }

        for (Map.Entry<String, String> entry : visibilitySettings.entrySet()) {
            UUID playerId = parseUuid(entry.getKey(), warningSink);
            if (playerId == null) {
                continue;
            }
            ArmorVisibilitySetting setting = ArmorVisibilitySetting.fromCode(entry.getValue());
            if (setting == null) {
                warningSink.accept("Ignoring invalid visibility setting for "
                        + entry.getKey() + ": " + entry.getValue());
                continue;
            }
            if (setting.isAllVisible()) {
                decoded.remove(playerId);
            } else {
                decoded.put(playerId, setting);
            }
        }
        return decoded;
    }

    static Map<String, String> encode(Map<UUID, ArmorVisibilitySetting> settings) {
        Map<String, String> encoded = new LinkedHashMap<>();
        settings.entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue().hasHiddenSlots())
                .sorted(Map.Entry.comparingByKey(Comparator.comparing(UUID::toString)))
                .forEach(entry -> encoded.put(entry.getKey().toString(), entry.getValue().code()));
        return encoded;
    }

    private static UUID parseUuid(String rawUuid, Consumer<String> warningSink) {
        try {
            return UUID.fromString(rawUuid);
        } catch (IllegalArgumentException invalidUuid) {
            warningSink.accept("Ignoring invalid UUID in hidden-players.yml: " + rawUuid);
            return null;
        }
    }
}
