package com.tankwar.model.interfaces;

import java.awt.Rectangle;

/**
 * 可碰撞对象接口：提供轴对齐包围盒（AABB）与相交判定。
 *
 * @author TankWar Team
 */
public interface Collidable {

    /** @return 当前帧的轴对齐包围盒。 */
    Rectangle getBounds();

    /**
     * 判断与另一个可碰撞对象是否相交。
     *
     * @param other 另一个对象
     * @return 相交返回 true
     */
    default boolean intersects(Collidable other) {
        return getBounds().intersects(other.getBounds());
    }
}
