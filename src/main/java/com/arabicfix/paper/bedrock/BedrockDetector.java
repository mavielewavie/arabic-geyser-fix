package com.arabicfix.paper.bedrock;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Decides whether a connected player is a Bedrock player (joined through Geyser).
 *
 * <p>Floodgate and Geyser are reached by reflection, so the plugin has no hard dependency on
 * either. Checks run in this order: Floodgate API, Geyser API, Floodgate-style UUID
 * (most significant bits are zero), optional username prefix.</p>
 */
public final class BedrockDetector {

    /** Negative answers are re-checked after this long, in case Geyser registered the session late. */
    private static final long NEGATIVE_TTL_NANOS = 5_000_000_000L;

    private record Entry(boolean bedrock, long checkedAt) {
    }

    private final Logger logger;
    private final boolean useFloodgate;
    private final boolean useGeyser;
    private final boolean uuidFallback;
    private final String namePrefix;

    private final Map<UUID, Entry> cache = new ConcurrentHashMap<>();

    private Method floodgateGetInstance;
    private Method floodgateIsPlayer;
    private Method geyserApi;
    private Method geyserIsBedrock;

    public BedrockDetector(Logger logger, boolean useFloodgate, boolean useGeyser, boolean uuidFallback,
                           String namePrefix, ClassLoader loader) {
        this.logger = logger;
        this.useFloodgate = useFloodgate;
        this.useGeyser = useGeyser;
        this.uuidFallback = uuidFallback;
        this.namePrefix = namePrefix == null ? "" : namePrefix;
        resolve(loader);
    }

    private void resolve(ClassLoader loader) {
        if (useFloodgate) {
            try {
                Class<?> api = Class.forName("org.geysermc.floodgate.api.FloodgateApi", true, loader);
                floodgateGetInstance = api.getMethod("getInstance");
                floodgateIsPlayer = api.getMethod("isFloodgatePlayer", UUID.class);
                logger.info("Bedrock detection: Floodgate API found.");
            } catch (ReflectiveOperationException | LinkageError ignored) {
                logger.info("Bedrock detection: Floodgate API not found (skipping).");
            }
        }
        if (useGeyser) {
            try {
                Class<?> api = Class.forName("org.geysermc.geyser.api.GeyserApi", true, loader);
                geyserApi = api.getMethod("api");
                geyserIsBedrock = api.getMethod("isBedrockPlayer", UUID.class);
                logger.info("Bedrock detection: Geyser API found.");
            } catch (ReflectiveOperationException | LinkageError ignored) {
                logger.info("Bedrock detection: Geyser API not found (skipping).");
            }
        }
    }

    public boolean isBedrock(UUID uuid, String name) {
        if (uuid == null) return false;
        Entry cached = cache.get(uuid);
        if (cached != null) {
            if (cached.bedrock() || System.nanoTime() - cached.checkedAt() < NEGATIVE_TTL_NANOS) {
                return cached.bedrock();
            }
        }
        boolean result = detect(uuid, name);
        cache.put(uuid, new Entry(result, System.nanoTime()));
        return result;
    }

    private boolean detect(UUID uuid, String name) {
        if (floodgateGetInstance != null) {
            try {
                Object api = floodgateGetInstance.invoke(null);
                if (api != null && (boolean) floodgateIsPlayer.invoke(api, uuid)) return true;
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // API not ready yet, fall through to the other checks
            }
        }
        if (geyserApi != null) {
            try {
                Object api = geyserApi.invoke(null);
                if (api != null && (boolean) geyserIsBedrock.invoke(api, uuid)) return true;
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // Geyser still starting up
            }
        }
        if (uuidFallback && uuid.getMostSignificantBits() == 0L) return true;
        return !namePrefix.isEmpty() && name != null && name.startsWith(namePrefix);
    }

    public void forget(UUID uuid) {
        cache.remove(uuid);
    }

    public void clear() {
        cache.clear();
    }
}
