package com.tankwar.constant;

/**
 * 坦克种类枚举，承载血量、速度修正、火力与击毁分值。
 *
 * @author TankWar Team
 */
public enum TankType {
    /** 玩家 1（黄色）。 */
    PLAYER_1(1, 0, 1, 0, "tank_player1.png"),
    /** 玩家 2（绿色）。 */
    PLAYER_2(1, 0, 1, 0, "tank_player2.png"),
    /** 普通敌方坦克（灰色，100 分）。 */
    AI_BASIC(1, 0, 1, 100, "enemy_basic.png"),
    /** 快速敌方坦克（浅蓝，200 分）。 */
    AI_FAST(1, 1, 1, 200, "enemy_fast.png"),
    /** 重装甲方坦克（红色，3 点血，300 分）。 */
    AI_ARMORED(3, 0, 1, 300, "enemy_armored.png"),
    /** 奖励坦克（红黄闪烁，击毁必掉道具，500 分）。 */
    AI_BONUS(1, 0, 1, 500, "enemy_bonus.png");

    /** 最大血量。 */
    private final int maxHp;
    /** 速度修正（在难度基础速度上叠加）。 */
    private final int speedDelta;
    /** 初始子弹威力（1 普通 / 2 可破钢墙）。 */
    private final int bulletPower;
    /** 击毁得分。 */
    private final int scoreValue;
    /** 精灵图资源文件名。 */
    private final String sprite;

    TankType(int maxHp, int speedDelta, int bulletPower, int scoreValue,
             String sprite) {
        this.maxHp = maxHp;
        this.speedDelta = speedDelta;
        this.bulletPower = bulletPower;
        this.scoreValue = scoreValue;
        this.sprite = sprite;
    }

    /** @return 最大血量。 */
    public int getMaxHp() {
        return maxHp;
    }

    /** @return 速度修正。 */
    public int getSpeedDelta() {
        return speedDelta;
    }

    /** @return 初始子弹威力。 */
    public int getBulletPower() {
        return bulletPower;
    }

    /** @return 击毁分值。 */
    public int getScoreValue() {
        return scoreValue;
    }

    /** @return 精灵图资源名。 */
    public String getSprite() {
        return sprite;
    }

    /** @return 是否为敌方坦克。 */
    public boolean isEnemy() {
        return this != PLAYER_1 && this != PLAYER_2;
    }
}
