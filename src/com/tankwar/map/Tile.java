package com.tankwar.map;

import com.tankwar.constant.TileType;

/**
 * 地形格不可变值对象。
 * <p>记录某格的行列坐标、类型与当前耐久（仅砖墙有意义）。</p>
 *
 * @author TankWar Team
 */
public final class Tile {

    /** 行号。 */
    private final int row;
    /** 列号。 */
    private final int col;
    /** 地形类型。 */
    private final TileType type;
    /** 当前耐久（钢墙为 0，砖墙动态维护）。 */
    private final int hp;

    /**
     * 构造地形格。
     *
     * @param row  行
     * @param col  列
     * @param type 类型
     * @param hp   耐久
     */
    public Tile(int row, int col, TileType type, int hp) {
        this.row = row;
        this.col = col;
        this.type = type;
        this.hp = hp;
    }

    /** @return 行号。 */
    public int getRow() {
        return row;
    }

    /** @return 列号。 */
    public int getCol() {
        return col;
    }

    /** @return 地形类型。 */
    public TileType getType() {
        return type;
    }

    /** @return 当前耐久。 */
    public int getHp() {
        return hp;
    }
}
