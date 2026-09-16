package com.tankwar.map;

import com.tankwar.constant.GameConfig;
import com.tankwar.constant.TileType;

/**
 * 二维网格地图。
 * <p>内部维护地形类型网格与砖墙耐久网格，提供通行查询、坐标互转与
 * 地形破坏；所有越界访问均按"阻挡"安全处理，不抛异常。</p>
 *
 * @author TankWar Team
 */
public class GameMap {

    /** 地形类型网格 [row][col]。 */
    private final TileType[][] grid;
    /** 砖墙耐久网格 [row][col]，仅 BRICK 有意义。 */
    private final int[][] brickHp;

    /** 构造空白地图。 */
    public GameMap() {
        this.grid = new TileType[GameConfig.MAP_ROWS][GameConfig.MAP_COLS];
        this.brickHp = new int[GameConfig.MAP_ROWS][GameConfig.MAP_COLS];
        for (int r = 0; r < GameConfig.MAP_ROWS; r++) {
            for (int c = 0; c < GameConfig.MAP_COLS; c++) {
                grid[r][c] = TileType.EMPTY;
            }
        }
    }

    /**
     * 设置某格类型并初始化耐久。
     *
     * @param row  行
     * @param col  列
     * @param type 类型
     */
    public void setTileType(int row, int col, TileType type) {
        if (!inBounds(row, col)) {
            return;
        }
        grid[row][col] = type;
        brickHp[row][col] = type == TileType.BRICK ? GameConfig.BRICK_HP : 0;
    }

    /**
     * 读取地形类型；越界统一视为钢墙（最安全的阻挡）。
     *
     * @param row 行
     * @param col 列
     * @return 地形类型
     */
    public TileType getTileType(int row, int col) {
        if (!inBounds(row, col)) {
            return TileType.STEEL;
        }
        return grid[row][col];
    }

    /**
     * 坦克能否通行该格；越界返回 false。
     *
     * @param row 行
     * @param col 列
     * @return 可通行返回 true
     */
    public boolean isTankPassable(int row, int col) {
        return inBounds(row, col) && grid[row][col].isTankPassable();
    }

    /**
     * 坐标是否在地图范围内。
     *
     * @param row 行
     * @param col 列
     * @return 在范围内返回 true
     */
    public boolean inBounds(int row, int col) {
        return row >= 0 && row < GameConfig.MAP_ROWS
                && col >= 0 && col < GameConfig.MAP_COLS;
    }

    /**
     * 读取砖墙当前耐久（渲染裂纹用）。
     *
     * @param row 行
     * @param col 列
     * @return 耐久值；非砖墙返回 0
     */
    public int getBrickHp(int row, int col) {
        if (!inBounds(row, col)) {
            return 0;
        }
        return brickHp[row][col];
    }

    /**
     * 子弹对某格造成伤害。
     * <p>砖墙 4 点耐久：普通弹（威力1）每次扣 2，两发摧毁；强化弹
     * （威力2）一发摧毁。钢墙仅强化弹可破。基地被命中直接上报。</p>
     *
     * @param row   行
     * @param col   列
     * @param power 子弹威力
     * @return 命中结果
     */
    public TileHitResult damageTile(int row, int col, int power) {
        if (!inBounds(row, col)) {
            return TileHitResult.STEEL_BLOCKED;
        }
        TileType type = grid[row][col];
        switch (type) {
            case BRICK: {
                brickHp[row][col] -= power * 2;
                if (brickHp[row][col] <= 0) {
                    grid[row][col] = TileType.EMPTY;
                    brickHp[row][col] = 0;
                    return TileHitResult.BRICK_DESTROYED;
                }
                return TileHitResult.BRICK_DAMAGED;
            }
            case STEEL:
                if (power >= 2) {
                    grid[row][col] = TileType.EMPTY;
                    return TileHitResult.STEEL_DESTROYED;
                }
                return TileHitResult.STEEL_BLOCKED;
            case BASE:
                return TileHitResult.BASE_HIT;
            default:
                return TileHitResult.NONE;
        }
    }

    /**
     * 像素坐标 -> 所在行。
     *
     * @param y 像素 y
     * @return 行号
     */
    public static int pixelToRow(int y) {
        return Math.floorDiv(y, GameConfig.TILE_SIZE);
    }

    /**
     * 像素坐标 -> 所在列。
     *
     * @param x 像素 x
     * @return 列号
     */
    public static int pixelToCol(int x) {
        return Math.floorDiv(x, GameConfig.TILE_SIZE);
    }

    /**
     * 行/列 -> 该格左上角像素坐标。
     *
     * @param row 行
     * @param col 列
     * @return {x, y}
     */
    public static int[] cellToPixel(int row, int col) {
        return new int[]{col * GameConfig.TILE_SIZE, row * GameConfig.TILE_SIZE};
    }
}
