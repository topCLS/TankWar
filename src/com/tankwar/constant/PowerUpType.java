package com.tankwar.constant;

import java.awt.Color;

/**
 * 道具类型枚举，承载持续时长、掉落权重、主题色与精灵图。
 * <p>工厂据此做加权随机，新增道具只需增加枚举项 + 一个子类，
 * 无需改动工厂中的 if-else 链（开闭原则）。</p>
 *
 * @author TankWar Team
 */
public enum PowerUpType {
    /** 加速：移动速度提升，持续 8 秒。 */
    SPEED_BOOST(8000L, 35, new Color(248, 216, 48), "power_speed.png", "加速"),
    /** 强化子弹：可击穿钢墙，持续 10 秒。 */
    POWER_BULLET(10000L, 35, new Color(255, 150, 60), "power_star.png", "穿甲弹"),
    /** 护盾：免疫一切伤害，持续 10 秒。 */
    SHIELD(10000L, 30, new Color(90, 170, 255), "power_shield.png", "护盾");

    /** 效果持续时间（毫秒）。 */
    private final long durationMs;
    /** 掉落权重。 */
    private final int weight;
    /** 主题色（HUD 进度条等）。 */
    private final Color color;
    /** 精灵图资源名。 */
    private final String sprite;
    /** 中文名。 */
    private final String displayName;

    PowerUpType(long durationMs, int weight, Color color, String sprite,
                String displayName) {
        this.durationMs = durationMs;
        this.weight = weight;
        this.color = color;
        this.sprite = sprite;
        this.displayName = displayName;
    }

    /** @return 持续时间（毫秒）。 */
    public long getDurationMs() {
        return durationMs;
    }

    /** @return 掉落权重。 */
    public int getWeight() {
        return weight;
    }

    /** @return 主题色。 */
    public Color getColor() {
        return color;
    }

    /** @return 精灵图资源名。 */
    public String getSprite() {
        return sprite;
    }

    /** @return 中文名。 */
    public String getDisplayName() {
        return displayName;
    }
}
