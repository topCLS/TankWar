package com.tankwar.model;

import com.tankwar.constant.Direction;
import com.tankwar.constant.GameConfig;
import com.tankwar.model.interfaces.Updatable;
import com.tankwar.util.SpriteCache;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * 坦克抽象基类：玩家坦克与 AI 坦克的公共状态与行为。
 * <p>包括朝向/速度/血量/火力/护盾/复活无敌、开火节流、履带颠簸动画，
 * 以及"按朝向旋转精灵图 + 资源缺失时矢量兜底"的统一绘制。</p>
 *
 * @author TankWar Team
 */
public abstract class Tank extends GameObject implements Updatable {

    /** 当前朝向（炮管方向）。 */
    protected Direction dir = Direction.UP;
    /** 移动速度（像素/逻辑步）。 */
    protected int speed;
    /** 当前血量。 */
    protected int hp;
    /** 最大血量。 */
    protected int maxHp;
    /** 子弹威力：1 普通 / 2 可破钢墙。 */
    protected int bulletPower = 1;
    /** 同时允许存在的最大子弹数。 */
    protected int maxBullets = 1;
    /** 开火冷却（毫秒）。 */
    protected long fireCooldownMs;
    /** 最近一次开火时间。 */
    protected long lastFireMs = 0L;

    /** 道具护盾（限时免疫）。 */
    protected boolean shield = false;
    /** 复活/出生无敌到期时刻（epoch ms）。 */
    protected long invincibleUntilMs = 0L;

    /** 出生格列。 */
    protected int spawnCol;
    /** 出生格行。 */
    protected int spawnRow;

    /** 本逻辑步是否发生了移动（用于履带动画）。 */
    protected boolean moving = false;
    /** 履带颠簸相位。 */
    protected int treadPhase = 0;

    /** 精灵图文件名。 */
    protected String spriteName;
    /** 矢量兜底车身色。 */
    protected Color bodyColor;

    /**
     * 构造坦克。
     *
     * @param col 出生格列
     * @param row 出生格行
     */
    protected Tank(int col, int row) {
        super(col * GameConfig.TILE_SIZE + CollisionMargin.MARGIN,
                row * GameConfig.TILE_SIZE + CollisionMargin.MARGIN,
                GameConfig.TANK_SIZE, GameConfig.TANK_SIZE);
        this.spawnCol = col;
        this.spawnRow = row;
    }

    /**
     * 是否允许开火：冷却到期且场上子弹不超限。
     *
     * @param now            当前时间
     * @param activeBullets  该坦克当前在场子弹数
     * @return 可开火返回 true
     */
    public boolean canFire(long now, int activeBullets) {
        return now - lastFireMs >= fireCooldownMs && activeBullets < maxBullets;
    }

    /** 记录一次开火。 */
    public void markFired(long now) {
        this.lastFireMs = now;
    }

    /**
     * 承受伤害。
     *
     * @param damage 伤害值
     * @param now    当前时间
     * @return 真正扣血返回 true；被护盾/无敌抵挡返回 false
     */
    public boolean takeDamage(int damage, long now) {
        if (shield || now < invincibleUntilMs) {
            return false;
        }
        hp -= damage;
        if (hp <= 0) {
            hp = 0;
            alive = false;
        }
        return true;
    }

    /** @return 当前是否处于无敌（复活/出生保护）。 */
    public boolean isInvincible(long now) {
        return now < invincibleUntilMs;
    }

    /** @return 朝向。 */
    public Direction getDir() {
        return dir;
    }

    /** @param dir 朝向。 */
    public void setDir(Direction dir) {
        this.dir = dir;
    }

    /** @return 速度。 */
    public int getSpeed() {
        return speed;
    }

    /** @param speed 速度。 */
    public void setSpeed(int speed) {
        this.speed = speed;
    }

    /** @return 当前血量。 */
    public int getHp() {
        return hp;
    }

    /** @return 最大血量。 */
    public int getMaxHp() {
        return maxHp;
    }

    /** @return 子弹威力。 */
    public int getBulletPower() {
        return bulletPower;
    }

    /** @param bulletPower 子弹威力。 */
    public void setBulletPower(int bulletPower) {
        this.bulletPower = bulletPower;
    }

    /** @return 最大在场子弹数。 */
    public int getMaxBullets() {
        return maxBullets;
    }

    /** @return 是否有道具护盾。 */
    public boolean hasShield() {
        return shield;
    }

    /** @param shield 设置护盾。 */
    public void setShield(boolean shield) {
        this.shield = shield;
    }

    /** @param ms 设置无敌到期时间。 */
    public void setInvincibleUntil(long ms) {
        this.invincibleUntilMs = ms;
    }

    /** @return 出生格列。 */
    public int getSpawnCol() {
        return spawnCol;
    }

    /** @return 出生格行。 */
    public int getSpawnRow() {
        return spawnRow;
    }

    /** @param moving 本步是否移动。 */
    public void setMoving(boolean moving) {
        this.moving = moving;
    }

    /** @return 精灵图文件名（子类可覆盖以做闪烁）。 */
    public String getSpriteName() {
        return spriteName;
    }

    /** @return 矢量车身色。 */
    protected Color getBodyColor() {
        return bodyColor;
    }

    /** @return 坦克主题色（HUD 图标等外部使用）。 */
    public Color getColor() {
        return bodyColor;
    }

    /** @return 奖励坦克是否处于闪烁相位（默认否，奖励坦克覆盖）。 */
    public boolean isBonusFlashing(long now) {
        return false;
    }

    /**
     * 推进履带颠簸动画。
     *
     * @param dtMs 逻辑步长
     */
    @Override
    public void update(long dtMs) {
        if (moving && treadPhase < Integer.MAX_VALUE - 1) {
            treadPhase++;
        }
    }

    /**
     * 统一绘制：旋转到当前朝向、贴图（缺失则矢量绘制）、护盾与无敌特效。
     */
    @Override
    public void draw(Graphics2D g) {
        long now = System.currentTimeMillis();
        int cx = getCenterX();
        int cy = getCenterY();
        double angle = angleOf(dir);
        int jitter = moving ? (treadPhase % 2 == 0 ? 1 : -1) : 0;

        Graphics2D ctx = (Graphics2D) g.create();
        ctx.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        ctx.rotate(angle, cx, cy);
        ctx.translate(jitter, 0);

        // 奖励坦克偶发白色高亮
        if (isBonusFlashing(now)) {
            ctx.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
        }
        BufferedImage img = SpriteCache.getInstance().get(getSpriteName());
        if (img != null) {
            ctx.drawImage(img, x, y, width, height, null);
            if (isBonusFlashing(now)) {
                ctx.setColor(Color.WHITE);
                ctx.fillRect(x, y, width, height);
            }
        } else {
            drawVectorTank(ctx);
        }
        ctx.dispose();

        // 道具护盾：蓝色光环
        if (shield) {
            drawShieldRing(g, cx, cy, now);
        }
        // 复活/出生无敌：白色节拍闪烁
        if (now < invincibleUntilMs) {
            drawInvincibleBlink(g, cx, cy, now);
        }
    }

    /**
     * 朝向到旋转角（精灵图炮管默认朝上）。
     *
     * @param d 方向
     * @return 弧度
     */
    protected static double angleOf(Direction d) {
        switch (d) {
            case RIGHT:
                return Math.PI / 2.0;
            case DOWN:
                return Math.PI;
            case LEFT:
                return -Math.PI / 2.0;
            default:
                return 0.0;
        }
    }

    /**
     * 无图片资源时的矢量坦克绘制（局部坐标系，炮管朝上）。
     *
     * @param g 已旋转到局部坐标的上下文
     */
    protected void drawVectorTank(Graphics2D g) {
        Color body = getBodyColor();
        // 履带
        g.setColor(new Color(30, 30, 34));
        g.fillRoundRect(x, y + 2, 6, height - 4, 2, 2);
        g.fillRoundRect(x + width - 6, y + 2, 6, height - 4, 2, 2);
        // 车体
        g.setColor(body);
        g.fillRoundRect(x + 5, y + 2, width - 10, height - 4, 4, 4);
        // 车身高光
        g.setColor(body.brighter());
        g.fillRect(x + 7, y + 4, width - 14, 3);
        // 炮塔
        g.setColor(body.darker());
        int t = 10;
        g.fillOval(x + (width - t) / 2, y + (height - t) / 2, t, t);
        // 炮管（朝上）
        g.setColor(new Color(40, 40, 44));
        g.fillRect(x + width / 2 - 2, y - 2, 4, height / 2);
    }

    /** 蓝色护盾光环。 */
    private void drawShieldRing(Graphics2D g, int cx, int cy, long now) {
        float pulse = 0.55f + 0.25f * (float) Math.sin(now / 120.0);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, pulse));
        g.setColor(new Color(90, 170, 255));
        g.setStroke(new BasicStroke(2.5f));
        g.drawOval(x - 3, y - 3, width + 6, height + 6);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.15f));
        g.fillOval(x - 3, y - 3, width + 6, height + 6);
        g.setComposite(AlphaComposite.SrcOver);
    }

    /** 复活无敌期间的白色星芒节拍闪烁。 */
    private void drawInvincibleBlink(Graphics2D g, int cx, int cy, long now) {
        if ((now / 130) % 2 != 0) {
            return;
        }
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.55f));
        g.setColor(Color.WHITE);
        g.fillRect(x - 1, y - 1, width + 2, height + 2);
        g.setComposite(AlphaComposite.SrcOver);
    }

    /** 坦克相对格子的居中余量桥接。 */
    private static final class CollisionMargin {
        static final int MARGIN =
                (GameConfig.TILE_SIZE - GameConfig.TANK_SIZE) / 2;
    }
}
