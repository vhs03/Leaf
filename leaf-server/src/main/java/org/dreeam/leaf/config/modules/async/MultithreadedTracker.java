package org.dreeam.leaf.config.modules.async;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import org.dreeam.leaf.async.tracker.AsyncTracker;
import org.dreeam.leaf.config.ConfigModules;
import org.dreeam.leaf.config.EnumConfigCategory;
import org.dreeam.leaf.config.LeafConfig;
import org.dreeam.leaf.config.annotations.Experimental;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MultithreadedTracker extends ConfigModules {

    private static final String BASE_PATH = EnumConfigCategory.ASYNC.getBaseKeyName() + ".async-entity-tracker";

    public String getBasePath() {
        return BASE_PATH;
    }

    @Experimental
    public static boolean enabled = false;
    public static int threads = 0;
    public static List<String> blacklistedEntityNames = new ArrayList<>(Arrays.asList("end_crystal"));
    private static final Set<EntityType<?>> blacklistedTypes = new HashSet<>();
    private static volatile boolean blacklistTypesResolved;
    private static boolean asyncMultithreadedTrackerInitialized;

    public static boolean isBlacklisted(EntityType<?> type) {
        final Set<EntityType<?>> types = blacklistedTypes;
        if (!blacklistTypesResolved) {
            synchronized (types) {
                if (!blacklistTypesResolved) {
                    resolveBlacklistedTypes();
                    blacklistTypesResolved = true;
                }
            }
        }
        return types.contains(type);
    }

    public static Set<EntityType<?>> getBlacklistedTypes() {
        final Set<EntityType<?>> types = blacklistedTypes;
        if (!blacklistTypesResolved) {
            synchronized (types) {
                if (!blacklistTypesResolved) {
                    resolveBlacklistedTypes();
                    blacklistTypesResolved = true;
                }
            }
        }
        return types;
    }

    @Override
    public void onLoaded() {
        config.addCommentRegionBased(BASE_PATH, """
                ** Experimental Feature **
                Make entity tracking asynchronously, can improve performance significantly,
                especially in some massive entities in small area situations.""", """
                ** 实验性功能 **
                异步实体跟踪,
                在实体数量多且密集的情况下效果明显.""");

        if (asyncMultithreadedTrackerInitialized) {
            config.getConfigSection(BASE_PATH);
            blacklistTypesResolved = false;
            return;
        }
        asyncMultithreadedTrackerInitialized = true;

        enabled = config.getBoolean(BASE_PATH + ".enabled", false);
        threads = config.getInt(BASE_PATH + ".threads", 0);

        if (threads <= 0) {
            threads = Math.min(Runtime.getRuntime().availableProcessors(), 4);
        }
        threads = Math.max(threads, 1);

        blacklistedEntityNames = config.getList(BASE_PATH + ".blacklisted-entities", blacklistedEntityNames,
            config.pickStringRegionBased("""
                    Entities in this list will NOT be processed by the async entity tracker.
                    They will be tracked synchronously on the main thread instead.
                    Useful for entities like end crystals that become inconsistent with async tracking.""",
                """
                    不会被异步跟踪的实体列表,
                    在此列表中的实体将使用主线程同步跟踪."""));

        if (enabled) {
            LeafConfig.LOGGER.info("Using {} threads for Async Entity Tracker", threads);
            AsyncTracker.init();
        }
    }

    private static void resolveBlacklistedTypes() {
        blacklistedTypes.clear();
        final String DEFAULT_PREFIX = Identifier.DEFAULT_NAMESPACE + Identifier.NAMESPACE_SEPARATOR;
        for (String name : blacklistedEntityNames) {
            String lowerName = name.toLowerCase(Locale.ROOT);
            String typeId = lowerName.startsWith(DEFAULT_PREFIX) ? lowerName : DEFAULT_PREFIX + lowerName;
            EntityType.byString(typeId).ifPresentOrElse(
                blacklistedTypes::add,
                () -> LeafConfig.LOGGER.warn("Skip unknown entity {}, in {}", name, BASE_PATH + ".blacklisted-entities")
            );
        }
    }
}
