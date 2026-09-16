package com.tankwar.render;

import com.tankwar.model.interfaces.Drawable;
import com.tankwar.model.interfaces.Updatable;
import com.tankwar.util.SpriteCache;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * 爆炸帧动画（4 帧），支持小/中/大三种规格。
 * <p>大爆炸由 {@code core.World} 同时触发屏幕震动。图片缺失时用
 * 程序化同心圆兜底。</p>
 *
 * @author TankWar Team
 */
public class Explosion implements Updatable, Drawable {

    /** 爆炸规格。 */
    public enum Scale {
        /** 子弹撞墙的小爆炸。 */
        SMALL(20, 55L),
        /** 坦克爆炸。 */
        MEDIUM(44, 75L),
        /** 基地爆炸（伴随震屏）。 */
        LARGE(72, 95L);

        /** 绘制直径。 */
        private final int diameter;
        /** 单帧时长。 */
        private final long frameIntervalMs;

        Scale(int diameter, long frameIntervalMs) {
            this.diameter = diameter;
            this.frameIntervalMs = frameIntervalMs;
        }

        /** @return 直径。 */
        public int getDiameter() {
            return diameter;
        }

        /** @return 单帧时长。 */
        public long getFrameIntervalMs() {
            return frameIntervalMs;
        }
    }

    /** 中心 x。 */
    private final int centerX;
    /** 中心 y。 */
    private final int centerY;
    /** 规格。 */
    private final Scale scale;
    /** 已播放时长。 */
    private long elapsedMs = 0L;
    /** 当前帧。 */
    private int frameIndex = 0;
    /** 是否存活。 */
    private boolean alive = true;

    /**
     * 构造爆炸。
     *
     * @param centerX 中心 x
     * @param centerY 中心 y
     * @param scale   规格
     */
    public Explosion(int centerX, int centerY, Scale scale) {
        this.centerX = centerX;
        this.centerY = centerY;
        this.scale = scale;
    }

    /**
     * 推进帧动画。
     *
     * @param dtMs 逻辑步长
     */
    @Override
    public void update(long dtMs) {
        elapsedMs += dtMs;
        frameIndex = Math.min(3,
                (int) (elapsedMs / scale.getFrameIntervalMs()));
        if (elapsedMs >= scale.getFrameIntervalMs() * 4L) {
            alive = false;
        }
    }

    /** @return 是否存活。 */
    public boolean isAlive() {
        return alive;
    }

    /** 绘制当前帧。 */
    @Override
    public void draw(Graphics2D g) {
        int d = scale.getDiameter();
        int x = centerX - d / 2;
        int y = centerY - d / 2;
        BufferedImage frame = SpriteCache.getInstance().getExplosion(frameIndex);
        if (frame != null) {
            g.drawImage(frame, x, y, d, d, null);
        } else {
            drawVector(g, d);
        }
    }

    /** 图片缺失时的程序化爆炸（黄橙红灰渐变同心圆）。 */
    private void drawVector(Graphics2D g, int d) {
        java.awt.Color[] palette = {
                new java.awt.Color(255, 240, 160),
                new java.awt.Color(255, 170, 40),
                new java.awt.Color(230, 80, 30),
                new java.awt.Color(120, 110, 110)
        };
        g.setColor(palette[Math.min(frameIndex, 3)]);
        int inset = frameIndex * 3;
        g.fillOval(centerX - d / 2 + inset, centerY - d / 2 + inset,
                d - inset * 2, d - inset * 2);
    }
}
