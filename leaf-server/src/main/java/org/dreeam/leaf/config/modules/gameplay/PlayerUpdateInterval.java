package org.dreeam.leaf.config.modules.gameplay;

import org.dreeam.leaf.config.ConfigModules;
import org.dreeam.leaf.config.EnumConfigCategory;

public class PlayerUpdateInterval extends ConfigModules {

    public String getBasePath() {
        return EnumConfigCategory.GAMEPLAY.getBaseKeyName() + ".player-update-interval";
    }

    public static int playerUpdateInterval = 2;

    @Override
    public void onLoaded() {
        playerUpdateInterval = config.getInt(getBasePath(), playerUpdateInterval,
            config.pickStringRegionBased(
                "The number of ticks between player tracker updates. Lower values make other players appear smoother but send more packets. Requires a restart.",
                "玩家追踪器更新之间的刻数. 较低的值会使其他玩家看起来更流畅, 但会发送更多数据包. 需要重启服务器."
            ));
        if (playerUpdateInterval < 1) {
            playerUpdateInterval = 2;
        }
    }
}
