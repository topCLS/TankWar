package com.tankwar.model;

import com.tankwar.constant.Difficulty;
import com.tankwar.constant.Direction;
import com.tankwar.constant.GameConfig;
import com.tankwar.constant.TankType;

import java.awt.Color;

/**
 * 敌方 AI 坦克。
 * <p>除坦克属性外，还承载 AI 决策的跨线程数据交换：决策线程（调度器）
 * 周期性地把"下一步移动方向 / 是否开火"写入 volatile 字段，游戏主线程
 * 在逻辑步中消费，实现"决策与执行分离"。</p>
 *
 * @author TankWar Team
 */
public class AITank extends Tank {

    /** 敌方坦克种类。 */
    private final TankType type;
    /** 当前难度参数。 */
    private final Difficulty difficulty;

    /** 决策线程输出的下一步移动方向。 */
    private volatile Direction aiMoveDir = Direction.DOWN;
    /** 决策线程输出的开火请求。 */
    private volatile boolean aiWantFire = false;
    /** 上一逻辑步移动是否被阻挡（供决策线程判断撞墙换向）。 */
    private volatile boolean moveBlocked = false;
    /** 击毁该坦克的玩家编号（计分归属，AI 子弹为 -1）。 */
    private volatile int killerPlayerId = -1;

    /**
     * 构造敌方坦克。
     *
     * @param type       坦克种类
     * @param col        出生格列
     * @param row        出生格行
     * @param difficulty 当前难度
     * @param now        出生时间
     */
    public AITank(TankType type, int col, int row, Difficulty difficulty, long now) {
        super(col, row);
        this.type = type;
        this.difficulty = difficulty;
        this.maxHp = type.getMaxHp();
        this.hp = maxHp;
        int baseSpeed = difficulty.getMoveSpeed() + type.getSpeedDelta();
        this.speed = Math.max(1, Math.min(3, baseSpeed));
        this.bulletPower = type.getBulletPower();
        this.maxBullets = GameConfig.MAX_AI_BULLETS;
        this.fireCooldownMs = difficulty.getFireCooldownMs();
        this.spriteName = type.getSprite();
        this.dir = Direction.DOWN;
        this.aiMoveDir = Direction.DOWN;
        this.invincibleUntilMs = now + GameConfig.AI_SPAWN_SHIELD_MS;
        this.bodyColor = colorOf(type);
    }

    /** @return 敌方坦克种类。 */
    public TankType getType() {
        return type;
    }

    /** @return 当前难度。 */
    public Difficulty getDifficulty() {
        return difficulty;
    }

    /** @return 决策给出的移动方向。 */
    public Direction getAiMoveDir() {
        return aiMoveDir;
    }

    /** @param dir 决策写入的移动方向。 */
    public void setAiMoveDir(Direction dir) {
        if (dir != null) {
            this.aiMoveDir = dir;
        }
    }

    /** @return 是否请求开火（读后由主线程清）。 */
    public boolean consumeFireRequest() {
        boolean value = aiWantFire;
        aiWantFire = false;
        return value;
    }

    /** @param fire 决策写入开火请求。 */
    public void setAiWantFire(boolean fire) {
        this.aiWantFire = fire;
    }

    /** @return 上一步是否被阻挡。 */
    public boolean isMoveBlocked() {
        return moveBlocked;
    }

    /** @param blocked 设置阻挡标志。 */
    public void setMoveBlocked(boolean blocked) {
        this.moveBlocked = blocked;
    }

    /** @return 击毁者玩家编号。 */
    public int getKillerPlayerId() {
        return killerPlayerId;
    }

    /** @param killerPlayerId 设置击毁者玩家编号。 */
    public void setKillerPlayerId(int killerPlayerId) {
        this.killerPlayerId = killerPlayerId;
    }

    /** @return 是否奖励坦克。 */
    public boolean isBonus() {
        return type == TankType.AI_BONUS;
    }

    /**
     * 奖励坦克每 200ms 在高亮/常态之间切换，吸引玩家优先击毁。
     */
    @Override
    public boolean isBonusFlashing(long now) {
        return isBonus() && (now / 200) % 2 == 0;
    }

    /** 敌方坦克种类 -> 矢量车身色。 */
    private static Color colorOf(TankType t) {
        switch (t) {
            case AI_FAST:
                return new Color(168, 210, 255);
            case AI_ARMORED:
                return new Color(210, 70, 50);
            case AI_BONUS:
                return new Color(240, 200, 40);
            default:
                return new Color(176, 176, 176);
        }
    }
}
