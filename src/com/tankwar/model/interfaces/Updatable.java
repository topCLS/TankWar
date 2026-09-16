package com.tankwar.model.interfaces;

/**
 * 可逻辑更新对象接口。
 * <p>仅承担与世界无关的轻量动画推进（帧动画、计时器）；
 * 移动、碰撞、开火等世界交互由 {@code core.World} 统一调度。</p>
 *
 * @author TankWar Team
 */
public interface Updatable {

    /**
     * 推进一个逻辑步。
     *
     * @param dtMs 距上一逻辑步的毫秒数（固定步长下为常量）
     */
    void update(long dtMs);
}
