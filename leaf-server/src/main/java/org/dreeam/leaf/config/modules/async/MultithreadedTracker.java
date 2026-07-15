package org.dreeam.leaf.config.modules.async;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;
import org.dreeam.leaf.async.tracker.AsyncTracker;
import org.dreeam.leaf.config.ConfigModules;
import org.dreeam.leaf.config.EnumConfigCategory;
import org.dreeam.leaf.config.LeafConfig;
import org.dreeam.leaf.config.annotations.Experimental;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MultithreadedTracker extends ConfigModules {

    public String getBasePath() {
        return EnumConfigCategory.ASYNC.getBaseKeyName() + ".async-entity-tracker";
    }

    @Experimental
    public static boolean enabled = false;
    public static int threads = 0;
    public static List<String> blacklistedEntities = new ArrayList<>(List.of("end_crystal"));
    private static Set<EntityType<?>> blacklistedEntityTypes = Set.of();
    private static boolean asyncMultithreadedTrackerInitialized;

    @Override
    public void onLoaded() {
        config.addCommentRegionBased(getBasePath(), """
                ** Experimental Feature **
                Make entity tracking asynchronously, can improve performance significantly,
                especially in some massive entities in small area situations.""", """
                ** 实验性功能 **
                异步实体跟踪,
                在实体数量多且密集的情况下效果明显.""");

        if (asyncMultithreadedTrackerInitialized) {
            config.getConfigSection(getBasePath());
            return;
        }
        asyncMultithreadedTrackerInitialized = true;

        enabled = config.getBoolean(getBasePath() + ".enabled", false);
        threads = config.getInt(getBasePath() + ".threads", 0);
        blacklistedEntities = config.getList(getBasePath() + ".blacklisted-entities", blacklistedEntities,
            config.pickStringRegionBased("""
                    Entities in this list will not be processed by the async entity tracker.
                    They are tracked synchronously on the main thread instead.
                    Use entity IDs such as end_crystal or minecraft:end_crystal.
                    Requires a server restart.""", """
                    此列表中的实体不会由异步实体追踪器处理.
                    它们将改为在主线程上同步追踪.
                    使用实体 ID, 例如 end_crystal 或 minecraft:end_crystal.
                    需要重启服务器."""));

        if (threads <= 0) {
            threads = Math.min(Runtime.getRuntime().availableProcessors(), 4);
        }
        threads = Math.max(threads, 1);

        if (enabled) {
            LeafConfig.LOGGER.info("Using {} threads for Async Entity Tracker", threads);
            AsyncTracker.init();
        }
    }

    @Override
    public void onPostLoaded() {
        Set<EntityType<?>> resolvedTypes = new HashSet<>();
        for (String name : blacklistedEntities) {
            String typeId = name.toLowerCase(Locale.ROOT);
            if (typeId.indexOf(Identifier.NAMESPACE_SEPARATOR) < 0) {
                typeId = Identifier.DEFAULT_NAMESPACE + Identifier.NAMESPACE_SEPARATOR + typeId;
            }

            String configuredName = name;
            EntityType.byString(typeId).ifPresentOrElse(
                resolvedTypes::add,
                () -> LeafConfig.LOGGER.warn("Skip unknown entity {}, in {}", configuredName, getBasePath() + ".blacklisted-entities")
            );
        }
        blacklistedEntityTypes = Set.copyOf(resolvedTypes);
    }

    public static boolean isBlacklisted(EntityType<?> entityType) {
        return blacklistedEntityTypes.contains(entityType);
    }
}
