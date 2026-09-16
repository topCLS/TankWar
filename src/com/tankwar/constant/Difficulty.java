package com.tankwar.constant;

/**
 * AI 难度枚举，携带全部难度参数。
 * <p>所有敌方行为参数（速度、开火频率、瞄准失误率、视野、重算路径间隔、
 * 同时追击者上限、刷怪间隔）均从此处读取，实现"调枚举即调难度"。</p>
 *
 * @author TankWar Team
 */
public enum Difficulty {
    /** 简单：慢速、低射速、大失误、短视野、少追击。 */
    EASY(1, 1500L, 0.40, 8, 1200L, 1, 3600L, "简单"),
    /** 普通。 */
    NORMAL(2, 950L, 0.20, 12, 700L, 2, 3000L, "普通"),
    /** 困难：快速、高射速、几乎不失误、全图视野、集体追击。 */
    HARD(3, 600L, 0.08, 999, 350L, 4, 2400L, "困难");

    /** 基础移动速度（像素/逻辑步）。 */
    private final int moveSpeed;
    /** 开火冷却（毫秒）。 */
    private final long fireCooldownMs;
    /** 瞄准失误率（0~1，越大越不准）。 */
    private final double aimError;
    /** 视野范围（格）。 */
    private final int visionRange;
    /** 路径重算间隔（毫秒）。 */
    private final long repathIntervalMs;
    /** 同时处于追击状态的敌人上限。 */
    private final int maxChasers;
    /** 刷怪间隔（毫秒）。 */
    private final long spawnIntervalMs;
    /** 中文名。 */
    private final String displayName;

    Difficulty(int moveSpeed, long fireCooldownMs, double aimError, int visionRange,
               long repathIntervalMs, int maxChasers, long spawnIntervalMs,
               String displayName) {
        this.moveSpeed = moveSpeed;
        this.fireCooldownMs = fireCooldownMs;
        this.aimError = aimError;
        this.visionRange = visionRange;
        this.repathIntervalMs = repathIntervalMs;
        this.maxChasers = maxChasers;
        this.spawnIntervalMs = spawnIntervalMs;
        this.displayName = displayName;
    }

    /** @return 基础移动速度。 */
    public int getMoveSpeed() {
        return moveSpeed;
    }

    /** @return 开火冷却（毫秒）。 */
    public long getFireCooldownMs() {
        return fireCooldownMs;
    }

    /** @return 瞄准失误率。 */
    public double getAimError() {
        return aimError;
    }

    /** @return 视野范围（格）。 */
    public int getVisionRange() {
        return visionRange;
    }

    /** @return 路径重算间隔（毫秒）。 */
    public long getRepathIntervalMs() {
        return repathIntervalMs;
    }

    /** @return 同时追击者上限。 */
    public int getMaxChasers() {
        return maxChasers;
    }

    /** @return 刷怪间隔（毫秒）。 */
    public long getSpawnIntervalMs() {
        return spawnIntervalMs;
    }

    /** @return 中文显示名。 */
    public String getDisplayName() {
        return displayName;
    }
}
