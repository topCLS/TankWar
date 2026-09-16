package com.tankwar.render;

import com.tankwar.model.interfaces.Drawable;
import com.tankwar.model.interfaces.Updatable;

import java.awt.Color;
import java.awt.Graphics2D;

/**
 * 火花粒子：子弹击中钢墙、坦克擦弹时迸发的短促光点。
 * <p>位置使用 double，按速度积分并逐帧衰减，生命结束自动回收；
 * 大量粒子也只产生极少对象开销。</p>
 *
 * @author TankWar Team
 */
public class Spark implements Updatable, Drawable {

    /** 位置与速度（像素/逻辑步）。 */
    private double x;
    private double y;
    private double vx;
    private double vy;
    /** 颜色。 */
    private final Color color;
    /** 总生命与已存活毫秒。 */
    private final long lifeMs;
    private long ageMs = 0L;
    /** 是否存活。 */
    private boolean alive = true;

    /**
     * 构造火花。
     *
     * @param x      初始 x
     * @param y      初始 y
     * @param vx     x 速度
     * @param vy     y 速度
     * @param color  颜色
     * @param lifeMs 生命时长
     */
    public Spark(double x, double y, double vx, double vy, Color color, long lifeMs) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.color = color;
        this.lifeMs = lifeMs;
    }

    /**
     * 推进粒子：位置积分、速度衰减、生命判定。
     *
     * @param dtMs 逻辑步长
     */
    @Override
    public void update(long dtMs) {
        double scale = dtMs / (1000.0 / 60.0);
        ageMs += dtMs;
        x += vx * scale;
        y += vy * scale;
        vx *= 0.90;
        vy *= 0.90;
        if (ageMs >= lifeMs) {
            alive = false;
        }
    }

    /** @return 是否存活。 */
    public boolean isAlive() {
        return alive;
    }

    /** 绘制渐隐光点。 */
    @Override
    public void draw(Graphics2D g) {
        float alpha = Math.max(0f, 1.0f - (float) ageMs / lifeMs);
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(),
                (int) (alpha * 255)));
        int size = 3;
        g.fillRect((int) x, (int) y, size, size);
    }
}
