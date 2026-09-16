package com.tankwar.constant;

/**
 * 地形类型枚举。
 * <p>每个地形携带三类通行/破坏属性，碰撞与渲染统一读取这些语义字段，
 * 避免散落在代码中的类型判断。</p>
 *
 * @author TankWar Team
 */
public enum TileType {
    /** 空地：坦克可走、子弹可过、不可摧毁。 */
    EMPTY(true, true, false, false),
    /** 砖墙：坦克不可走、可被子弹逐步摧毁。 */
    BRICK(false, false, true, false),
    /** 钢墙：坦克与普通子弹均不可通过、不可摧毁（强化弹可毁）。 */
    STEEL(false, false, false, false),
    /** 草丛：坦克可藏身，渲染时覆盖在坦克之上。 */
    GRASS(true, true, false, true),
    /** 河流：坦克不可通行、子弹可飞越。 */
    WATER(false, true, false, false),
    /** 基地（鹰巢）：不可通行，被命中即判负。 */
    BASE(false, false, true, false);

    /** 坦克是否可通行。 */
    private final boolean tankPassable;
    /** 子弹是否可无碰撞通过。 */
    private final boolean bulletPassable;
    /** 是否可被子弹摧毁。 */
    private final boolean destructible;
    /** 渲染时是否覆盖在坦克之上（草丛遮挡）。 */
    private final boolean coversTank;

    TileType(boolean tankPassable, boolean bulletPassable,
             boolean destructible, boolean coversTank) {
        this.tankPassable = tankPassable;
        this.bulletPassable = bulletPassable;
        this.destructible = destructible;
        this.coversTank = coversTank;
    }

    /** @return 坦克是否可通行。 */
    public boolean isTankPassable() {
        return tankPassable;
    }

    /** @return 子弹是否可直接通过。 */
    public boolean isBulletPassable() {
        return bulletPassable;
    }

    /** @return 是否可摧毁。 */
    public boolean isDestructible() {
        return destructible;
    }

    /** @return 是否在坦克之上渲染（草丛）。 */
    public boolean isCoversTank() {
        return coversTank;
    }
}
