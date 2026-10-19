package cn.jeifing.hidearmor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

final class BedrockPlayerDetector {
    private final Predicate<UUID> floodgateQuery;
    private final Logger logger;
    private final Set<UUID> bedrockPlayers = ConcurrentHashMap.newKeySet();
    private volatile boolean queryFailed;

    BedrockPlayerDetector(Logger logger) {
        this(createFloodgateQuery(), logger);
    }

    BedrockPlayerDetector(Predicate<UUID> floodgateQuery) {
        this(floodgateQuery, null);
    }

    private BedrockPlayerDetector(Predicate<UUID> floodgateQuery, Logger logger) {
        this.floodgateQuery = floodgateQuery;
        this.logger = logger;
    }

    void track(UUID playerId) {
        boolean bedrock = hasFloodgateUuidShape(playerId);
        if (floodgateQuery != null && !queryFailed) {
            try {
                bedrock |= floodgateQuery.test(playerId);
            } catch (RuntimeException exception) {
                queryFailed = true;
                if (logger != null) {
                    logger.log(Level.WARNING,
                            "Floodgate player detection failed; falling back to UUID-shape detection.",
                            exception);
                }
            }
        }

        if (bedrock) {
            bedrockPlayers.add(playerId);
        } else {
            bedrockPlayers.remove(playerId);
        }
    }

    boolean isBedrock(UUID playerId) {
        return playerId != null
                && (bedrockPlayers.contains(playerId) || hasFloodgateUuidShape(playerId));
    }

    void untrack(UUID playerId) {
        bedrockPlayers.remove(playerId);
    }

    void clear() {
        bedrockPlayers.clear();
    }

    static boolean hasFloodgateUuidShape(UUID playerId) {
        return playerId != null && playerId.getMostSignificantBits() == 0L;
    }

    private static Predicate<UUID> createFloodgateQuery() {
        try {
            Class<?> apiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            Method getInstance = apiClass.getMethod("getInstance");
            Method isFloodgatePlayer = apiClass.getMethod("isFloodgatePlayer", UUID.class);
            Object api = getInstance.invoke(null);
            return playerId -> invokeFloodgate(api, isFloodgatePlayer, playerId);
        } catch (ClassNotFoundException ignored) {
            return null;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException exception) {
            return playerId -> {
                throw new IllegalStateException("Unable to initialize Floodgate API reflection", exception);
            };
        }
    }

    private static boolean invokeFloodgate(Object api, Method method, UUID playerId) {
        try {
            return Boolean.TRUE.equals(method.invoke(api, playerId));
        } catch (IllegalAccessException | InvocationTargetException exception) {
            throw new IllegalStateException("Unable to query Floodgate player state", exception);
        }
    }
}
