package com.tankwar.map;

/**
 * 子弹命中地形的结果枚举。
 *
 * @author TankWar Team
 */
public enum TileHitResult {
    /** 未命中任何有效地形（空地/水/草）。 */
    NONE,
    /** 砖墙被击中但仍有耐久。 */
    BRICK_DAMAGED,
    /** 砖墙被彻底摧毁。 */
    BRICK_DESTROYED,
    /** 钢墙抵挡了普通子弹。 */
    STEEL_BLOCKED,
    /** 强化子弹击穿了钢墙。 */
    STEEL_DESTROYED,
    /** 基地被命中（触发游戏失败）。 */
    BASE_HIT
}
