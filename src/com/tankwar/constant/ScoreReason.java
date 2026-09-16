package com.tankwar.constant;

/**
 * 计分原因枚举，便于日志统计与结算分类。
 *
 * @author TankWar Team
 */
public enum ScoreReason {
    /** 击毁敌方坦克。 */
    KILL_ENEMY("击毁敌坦"),
    /** 拾取道具。 */
    PICKUP_POWERUP("拾取道具"),
    /** 通过关卡。 */
    LEVEL_CLEAR("关卡奖励"),
    /** 剩余生命结算奖励。 */
    LIVES_BONUS("生命奖励"),
    /** 对战模式中击中对手。 */
    VERSUS_HIT("对战命中");

    /** 中文说明。 */
    private final String text;

    ScoreReason(String text) {
        this.text = text;
    }

    /** @return 中文说明。 */
    public String getText() {
        return text;
    }
}
