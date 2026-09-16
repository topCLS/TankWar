package com.tankwar.model;

import com.tankwar.constant.Direction;
import com.tankwar.constant.GameConfig;
import com.tankwar.constant.TankType;

import java.awt.Color;

/**
 * 玩家坦克。
 * <p>在 {@link Tank} 基础上增加玩家编号与剩余生命，以及回到出生点、
 * 满血复活并获得 3 秒无敌的行为。</p>
 *
 * @author TankWar Team
 */
public class PlayerTank extends Tank {

    /** 玩家编号（1 或 2）。 */
    private final int playerId;
    /** 剩余生命数。 */
    private int lives;
    /** 坦克类型（决定外观）。 */
    private final TankType type;

    /**
     * 构造玩家坦克。
     *
     * @param playerId 玩家编号
     * @param col      出生格列
     * @param row      出生格行
     */
    public PlayerTank(int playerId, int col, int row) {
        super(col, row);
        this.playerId = playerId;
        this.type = playerId == 1 ? TankType.PLAYER_1 : TankType.PLAYER_2;
        this.maxHp = type.getMaxHp();
        this.hp = maxHp;
        this.speed = GameConfig.PLAYER_SPEED;
        this.bulletPower = type.getBulletPower();
        this.maxBullets = GameConfig.MAX_PLAYER_BULLETS;
        this.fireCooldownMs = GameConfig.PLAYER_FIRE_COOLDOWN_MS;
        this.lives = GameConfig.PLAYER_LIVES;
        this.spriteName = type.getSprite();
        this.bodyColor = playerId == 1
                ? new Color(248, 200, 40) : new Color(88, 200, 40);
        this.dir = Direction.UP;
        // 首次出生同样给予短暂无敌，避免刚开局即被流弹击中
        this.invincibleUntilMs = System.currentTimeMillis()
                + GameConfig.RESPAWN_INVINCIBLE_MS;
    }

    /** @return 玩家编号。 */
    public int getPlayerId() {
        return playerId;
    }

    /** @return 剩余生命数。 */
    public int getLives() {
        return lives;
    }

    /**
     * 扣除一条生命。
     *
     * @return 扣除后的生命数
     */
    public int loseLife() {
        lives--;
        return lives;
    }

    /** @return 是否仍有复活机会。 */
    public boolean hasLivesLeft() {
        return lives > 0;
    }

    /**
     * 在出生点满血复活：朝上、清护盾、获得复活无敌。
     *
     * @param now 当前时间
     */
    public void respawn(long now) {
        int px = spawnCol * GameConfig.TILE_SIZE
                + (GameConfig.TILE_SIZE - GameConfig.TANK_SIZE) / 2;
        int py = spawnRow * GameConfig.TILE_SIZE
                + (GameConfig.TILE_SIZE - GameConfig.TANK_SIZE) / 2;
        setX(px);
        setY(py);
        dir = Direction.UP;
        hp = maxHp;
        bulletPower = 1;
        shield = false;
        alive = true;
        invincibleUntilMs = now + GameConfig.RESPAWN_INVINCIBLE_MS;
        lastFireMs = 0L;
    }

    /** @return 坦克类型。 */
    public TankType getType() {
        return type;
    }
}
