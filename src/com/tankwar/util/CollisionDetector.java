package com.tankwar.util;

import com.tankwar.constant.Direction;
import com.tankwar.constant.GameConfig;
import com.tankwar.constant.TileType;
import com.tankwar.map.GameMap;

import java.awt.Rectangle;

/**
 * 碰撞检测器（无状态纯函数工具类）—— 本项目技术含量最高的模块。
 * <p>三类碰撞：</p>
 * <ol>
 *   <li>坦克 vs 墙：候选包围盒覆盖格检测 + 格线吸附（贴墙顺滑、不卡角）；</li>
 *   <li>坦克 vs 坦克：AABB 检测（由 World 在移动后回退）；</li>
 *   <li>子弹 vs 墙/坦克：分步扫掠 + 线段-AABB 连续求交（高速不穿透）。</li>
 * </ol>
 * 本类方法均无副作用：只判定与计算位置，真正的伤害动作由调用方执行，
 * 因此可直接编写单元测试。
 *
 * @author TankWar Team
 */
public final class CollisionDetector {

    /** 坦克边长相对格子的对齐余量（28px 坦克在 32px 格内上下左右各 2px）。 */
    public static final int ALIGN_MARGIN = (GameConfig.TILE_SIZE - GameConfig.TANK_SIZE) / 2;
    /** 转向时允许自动吸附到格线的最大偏差（像素）。 */
    public static final int SNAP_DISTANCE = 10;

    private CollisionDetector() {
    }

    /* ====================== 坦克 vs 地形 ====================== */

    /**
     * 判断坦克包围盒是否可占据某位置（四角覆盖格全部可通行）。
     * <p>复杂度 O(1)：坦克最多覆盖 2&times;2=4 格。地图越界一律视为阻挡，
     * 不抛异常。</p>
     *
     * @param map  地图（只读）
     * @param x    左上角 x
     * @param y    左上角 y
     * @param size 坦克边长
     * @return 可占据返回 true
     */
    public static boolean canTankOccupy(GameMap map, int x, int y, int size) {
        int c1 = Math.floorDiv(x, GameConfig.TILE_SIZE);
        int r1 = Math.floorDiv(y, GameConfig.TILE_SIZE);
        int c2 = Math.floorDiv(x + size - 1, GameConfig.TILE_SIZE);
        int r2 = Math.floorDiv(y + size - 1, GameConfig.TILE_SIZE);
        for (int r = r1; r <= r2; r++) {
            for (int c = c1; c <= c2; c++) {
                if (!map.isTankPassable(r, c)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * 坦克沿单轴尝试移动一逻辑步（含格线吸附与贴墙）。
     * <p>手感关键算法：</p>
     * <ol>
     *   <li>转向瞬间，若坦克在垂直于行进方向上与最近格线偏差不超过
     *       {@link #SNAP_DISTANCE}，自动对齐到格线，消除"卡半格"；</li>
     *   <li>沿行进方向推进 {@code speed} 像素；</li>
     *   <li>若被阻挡，则贴齐到障碍格边界后停下。</li>
     * </ol>
     *
     * @param map   地图
     * @param x     当前 x
     * @param y     当前 y
     * @param size  坦克边长
     * @param dir   行进方向
     * @param speed 本步速度
     * @return 长度为 2 的数组 {新 x, 新 y}
     */
    public static int[] tryMoveAxis(GameMap map, int x, int y, int size,
                                    Direction dir, int speed) {
        int nx = x;
        int ny = y;

        // 1) 垂直于行进轴的格线吸附（仅在目标对齐位置合法时执行）
        if (dir.isHorizontal()) {
            int delta = alignDelta(y);
            if (delta != 0 && canTankOccupy(map, x, y + delta, size)) {
                ny = y + delta;
            }
        } else {
            int delta = alignDelta(x);
            if (delta != 0 && canTankOccupy(map, x + delta, y, size)) {
                nx = x + delta;
            }
        }

        // 2) 沿行进轴推进
        int tx = nx + dir.getDx() * speed;
        int ty = ny + dir.getDy() * speed;
        if (canTankOccupy(map, tx, ty, size)) {
            return new int[]{tx, ty};
        }

        // 3) 被阻挡：贴齐障碍边
        return hitch(map, tx, ty, size, dir, nx, ny);
    }

    /**
     * 计算坐标到最近格线对齐位置的偏差。
     *
     * @param v 当前 x 或 y
     * @return 需要修正的像素量；偏差过大时返回 0（不吸附）
     */
    private static int alignDelta(int v) {
        int rem = Math.floorMod(v, GameConfig.TILE_SIZE);
        int delta = ALIGN_MARGIN - rem;
        if (delta == 0 || Math.abs(delta) > SNAP_DISTANCE) {
            return 0;
        }
        return delta;
    }

    /**
     * 贴齐算法：在候选包围盒覆盖的阻挡格中，沿运动方向取最近阻挡边界。
     *
     * @return 贴齐后的 {x, y}
     */
    private static int[] hitch(GameMap map, int tx, int ty, int size,
                               Direction dir, int fallbackX, int fallbackY) {
        int c1 = Math.floorDiv(tx, GameConfig.TILE_SIZE);
        int r1 = Math.floorDiv(ty, GameConfig.TILE_SIZE);
        int c2 = Math.floorDiv(tx + size - 1, GameConfig.TILE_SIZE);
        int r2 = Math.floorDiv(ty + size - 1, GameConfig.TILE_SIZE);

        int resultX = fallbackX;
        int resultY = fallbackY;
        switch (dir) {
            case RIGHT:
                for (int r = r1; r <= r2; r++) {
                    if (!map.isTankPassable(r, c2)) {
                        resultX = c2 * GameConfig.TILE_SIZE - size;
                        break;
                    }
                }
                break;
            case LEFT:
                for (int r = r1; r <= r2; r++) {
                    if (!map.isTankPassable(r, c1)) {
                        resultX = (c1 + 1) * GameConfig.TILE_SIZE;
                        break;
                    }
                }
                break;
            case DOWN:
                for (int c = c1; c <= c2; c++) {
                    if (!map.isTankPassable(r2, c)) {
                        resultY = r2 * GameConfig.TILE_SIZE - size;
                        break;
                    }
                }
                break;
            case UP:
            default:
                for (int c = c1; c <= c2; c++) {
                    if (!map.isTankPassable(r1, c)) {
                        resultY = (r1 + 1) * GameConfig.TILE_SIZE;
                        break;
                    }
                }
                break;
        }
        return new int[]{resultX, resultY};
    }

    /* ====================== 子弹 vs 矩形（扫掠） ====================== */

    /**
     * 线段 vs AABB 连续碰撞（slab 法），复杂度 O(1)。
     * <p>把子弹上一子步位置到当前位置的中心线段与目标矩形（按子弹半宽
     * 向外扩张）求交。即使单子步位移大于目标尺寸也不会漏检，从根本上
     * 解决高速子弹穿透问题。</p>
     *
     * @param target  目标包围盒
     * @param prevX   子弹上一位置中心 x
     * @param prevY   子弹上一位置中心 y
     * @param curX    子弹当前位置中心 x
     * @param curY    子弹当前位置中心 y
     * @param bulletW 子弹边长
     * @return 在 [0,1] 参数区间内相交返回 true
     */
    public static boolean sweptAABB(Rectangle target, double prevX, double prevY,
                                    double curX, double curY, int bulletW) {
        double half = bulletW / 2.0;
        double minX = target.x - half;
        double maxX = target.x + target.width + half;
        double minY = target.y - half;
        double maxY = target.y + target.height + half;

        double tEnter = 0.0;
        double tExit = 1.0;
        double dx = curX - prevX;
        double dy = curY - prevY;

        // X 轴
        if (Math.abs(dx) < 1e-9) {
            if (prevX < minX || prevX > maxX) {
                return false;
            }
        } else {
            double t1 = (minX - prevX) / dx;
            double t2 = (maxX - prevX) / dx;
            if (t1 > t2) {
                double tmp = t1;
                t1 = t2;
                t2 = tmp;
            }
            tEnter = Math.max(tEnter, t1);
            tExit = Math.min(tExit, t2);
            if (tEnter > tExit) {
                return false;
            }
        }
        // Y 轴
        if (Math.abs(dy) < 1e-9) {
            if (prevY < minY || prevY > maxY) {
                return false;
            }
        } else {
            double t1 = (minY - prevY) / dy;
            double t2 = (maxY - prevY) / dy;
            if (t1 > t2) {
                double tmp = t1;
                t1 = t2;
                t2 = tmp;
            }
            tEnter = Math.max(tEnter, t1);
            tExit = Math.min(tExit, t2);
            if (tEnter > tExit) {
                return false;
            }
        }
        return tExit >= 0.0 && tEnter <= 1.0;
    }

    /* ====================== 视线（AI / 火炮遮挡） ====================== */

    /**
     * 两点间是否存在阻挡视线的硬地形（砖墙 / 钢墙 / 基地）。
     * <p>采用 DDA 思想按 {@code thickness} 步长沿线密集采样，水体与草丛
     * 不阻挡视线与子弹。</p>
     *
     * @param map       地图
     * @param x1        起点 x
     * @param y1        起点 y
     * @param x2        终点 x
     * @param y2        终点 y
     * @param thickness 采样步长（像素），取子弹子步长度即可
     * @return 存在阻挡返回 true
     */
    public static boolean isLineBlocked(GameMap map, int x1, int y1,
                                        int x2, int y2, int thickness) {
        int dist = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
        int steps = Math.max(1, (int) Math.ceil(dist * 1.0 / thickness));
        for (int i = 1; i < steps; i++) {
            double t = i * 1.0 / steps;
            int px = (int) (x1 + (x2 - x1) * t);
            int py = (int) (y1 + (y2 - y1) * t);
            TileType type = map.getTileType(
                    Math.floorDiv(py, GameConfig.TILE_SIZE),
                    Math.floorDiv(px, GameConfig.TILE_SIZE));
            if (type == TileType.BRICK || type == TileType.STEEL
                    || type == TileType.BASE) {
                return true;
            }
        }
        return false;
    }
}
