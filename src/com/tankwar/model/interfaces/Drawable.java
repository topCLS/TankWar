package com.tankwar.model.interfaces;

import java.awt.Graphics2D;

/**
 * 可绘制对象接口。
 *
 * @author TankWar Team
 */
public interface Drawable {

    /**
     * 由自身把自己绘制到画布上。
     *
     * @param g 图形上下文（由渲染管线统一提供，禁止在方法内创建大图对象）
     */
    void draw(Graphics2D g);
}
