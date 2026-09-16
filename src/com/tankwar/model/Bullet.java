package com.tankwar.model;

import com.tankwar.constant.Direction;
import com.tankwar.constant.GameConfig;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 * 子弹实体。
 * <p>支持对象池复用（{@link #reset}），位置用 double 记录扫掠起点，
 * 供线段-AABB 连续碰撞检测使用；真正的分步扫掠移动由 {@code core.World}
 * 调度执行。</p>
 *
 * @author TankWar Team
 */
public class Bullet extends GameObject {

    /** 飞行方向。 */
    private Direction dir;
    /** 飞行速度。 */
    private int speed;
    /** 威力：1 普通 / 2 可破钢墙。 */
    private int power;
    /** 是否由玩家发射。 */
    private boolean fromPlayer;
    /** 发射者玩家编号（AI 为 -1）。 */
    private int ownerPlayerId;
    /** 发射者坦克引用（用于避免打自己）。 */
    private Tank owner;

    /** 上一子步位置（扫掠线段起点，双精度）。 */
    private double prevX;
    /** 上一子步位置 y。 */
    private double prevY;
    /** 双精度当前位置（支持非整数子步，取整后同步到包围盒）。 */
    private double posX;
    /** 双精度当前位置 y。 */
    private double posY;

    /** 池化默认构造，字段随后必须由 {@link #reset} 初始化。 */
    public Bullet() {
        super(0, 0, GameConfig.BULLET_SIZE, GameConfig.BULLET_SIZE);
        this.dir = Direction.UP;
        this.speed = GameConfig.BULLET_SPEED;
    }

    /**
     * 复用初始化（对象池）。
     *
     * @param owner         发射者
     * @param fromPlayer    是否玩家子弹
     * @param ownerPlayerId 发射者玩家编号（AI=-1）
     * @param startX        起点 x
     * @param startY        起点 y
     * @param dir           方向
     * @param speed         速度
     * @param power         威力
     */
    public void reset(Tank owner, boolean fromPlayer, int ownerPlayerId,
                      int startX, int startY, Direction dir, int speed, int power) {
        this.owner = owner;
        this.fromPlayer = fromPlayer;
        this.ownerPlayerId = ownerPlayerId;
        this.dir = dir;
        this.speed = speed;
        this.power = power;
        this.posX = startX;
        this.posY = startY;
        this.prevX = startX;
        this.prevY = startY;
        this.x = startX;
        this.y = startY;
        this.alive = true;
    }

    /** 在每个扫掠子步前记录当前位置作为线段起点。 */
    public void markPrev() {
        this.prevX = posX;
        this.prevY = posY;
    }

    /**
     * 沿方向推进一个子步（双精度），并同步整数包围盒。
     *
     * @param dx 水平位移
     * @param dy 垂直位移
     */
    public void moveBy(double dx, double dy) {
        this.posX += dx;
        this.posY += dy;
        this.x = (int) Math.round(posX);
        this.y = (int) Math.round(posY);
    }

    /** @return 上一子步位置中心 x。 */
    public double getPrevCenterX() {
        return prevX + width / 2.0;
    }

    /** @return 上一子步位置中心 y。 */
    public double getPrevCenterY() {
        return prevY + height / 2.0;
    }

    /** @return 当前位置中心 x。 */
    public double getCurCenterX() {
        return posX + width / 2.0;
    }

    /** @return 当前位置中心 y。 */
    public double getCurCenterY() {
        return posY + height / 2.0;
    }

    /** @return 方向。 */
    public Direction getDir() {
        return dir;
    }

    /** @return 速度。 */
    public int getSpeed() {
        return speed;
    }

    /** @return 威力。 */
    public int getPower() {
        return power;
    }

    /** @return 是否玩家子弹。 */
    public boolean isFromPlayer() {
        return fromPlayer;
    }

    /** @return 发射者玩家编号（AI=-1）。 */
    public int getOwnerPlayerId() {
        return ownerPlayerId;
    }

    /** @return 发射者坦克。 */
    public Tank getOwner() {
        return owner;
    }

    /**
     * 绘制发光炮弹：玩家为金白色、AI 为白红色，带微光外晕。
     */
    @Override
    public void draw(Graphics2D g) {
        Color core = fromPlayer ? new Color(255, 240, 150) : Color.WHITE;
        Color glow = fromPlayer ? new Color(255, 170, 40) : new Color(255, 90, 60);
        g.setColor(glow);
        g.fillOval(x - 1, y - 1, width + 2, height + 2);
        g.setColor(core);
        g.fillOval(x + 1, y + 1, width - 2, height - 2);
    }
}
