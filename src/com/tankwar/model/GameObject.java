package com.tankwar.model;

import com.tankwar.model.interfaces.Collidable;
import com.tankwar.model.interfaces.Drawable;

import java.awt.Rectangle;

/**
 * 一切游戏对象的抽象基类。
 * <p>统一持有位置（{@link #x}/{@link #y} 使用 volatile，供 AI 决策线程
 * 在不加锁的情况下读取最新位置）、尺寸与存活标志，并实现 AABB 包围盒。</p>
 *
 * @author TankWar Team
 */
public abstract class GameObject implements Collidable, Drawable {

    /** 左上角像素 x（游戏线程写、AI 线程读，故 volatile）。 */
    protected volatile int x;
    /** 左上角像素 y。 */
    protected volatile int y;
    /** 宽度。 */
    protected int width;
    /** 高度。 */
    protected int height;
    /** 是否存活（false 时将在帧末被统一回收）。 */
    protected volatile boolean alive = true;

    /**
     * 构造对象。
     *
     * @param x      左上角 x
     * @param y      左上角 y
     * @param width  宽
     * @param height 高
     */
    protected GameObject(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /** @return 包围盒。 */
    @Override
    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }

    /** @return x。 */
    public int getX() {
        return x;
    }

    /** @return y。 */
    public int getY() {
        return y;
    }

    /** @param x 设置 x。 */
    public void setX(int x) {
        this.x = x;
    }

    /** @param y 设置 y。 */
    public void setY(int y) {
        this.y = y;
    }

    /** @return 宽。 */
    public int getWidth() {
        return width;
    }

    /** @return 高。 */
    public int getHeight() {
        return height;
    }

    /** @return 中心点 x。 */
    public int getCenterX() {
        return x + width / 2;
    }

    /** @return 中心点 y。 */
    public int getCenterY() {
        return y + height / 2;
    }

    /** @return 是否存活。 */
    public boolean isAlive() {
        return alive;
    }

    /** @param alive 设置存活标志。 */
    public void setAlive(boolean alive) {
        this.alive = alive;
    }
}
