package com.tankwar.ai;

import com.tankwar.constant.Direction;
import com.tankwar.constant.GameConfig;
import com.tankwar.constant.TileType;
import com.tankwar.map.GameMap;

import java.awt.Point;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

/**
 * 网格寻路器（无状态、线程安全）：BFS 追踪移动目标、A* 攻击固定基地。
 * <p>代价模型：空地/草丛代价 1，砖墙代价 3（A* 允许"打穿砖墙"的路线，
 * 但尽量绕行），钢墙/河流/基地不可通行。BFS 用于追玩家——目标频繁
 * 移动，路径最优性意义不大且 O(V+E) 稳定；A* 用于打基地——目标固定，
 * 曼哈顿启发式显著减少展开节点。单次展开上限 2000 节点防止极端卡顿。</p>
 *
 * @author TankWar Team
 */
public final class PathFinder {

    /** 单次寻路最大展开节点数。 */
    private static final int MAX_EXPANDED = 2000;
    /** 砖墙通行代价（仅 A*）。 */
    private static final int COST_BRICK = 3;
    /** 空地代价。 */
    private static final int COST_OPEN = 1;

    private PathFinder() {
    }

    private static int idx(int r, int c) {
        return r * GameConfig.MAP_COLS + c;
    }

    private static int rowOf(int index) {
        return index / GameConfig.MAP_COLS;
    }

    private static int colOf(int index) {
        return index % GameConfig.MAP_COLS;
    }

    /**
     * BFS 求到移动目标的最短路径（只走坦克可通行格）。
     *
     * @param map   地图（只读）
     * @param start 起点格
     * @param goal  目标格
     * @return 路径点（不含起点、含目标方向上的可达邻格）；无解返回空表
     */
    public static List<Point> findPathBfs(GameMap map, Point start, Point goal) {
        int total = GameConfig.MAP_ROWS * GameConfig.MAP_COLS;
        int[] parent = new int[total];
        java.util.Arrays.fill(parent, -1);
        boolean[] visited = new boolean[total];
        ArrayDeque<Integer> queue = new ArrayDeque<Integer>();
        int startIdx = idx(start.y, start.x);
        int goalIdx = idx(goal.y, goal.x);
        queue.add(startIdx);
        visited[startIdx] = true;
        int expanded = 0;

        while (!queue.isEmpty()) {
            int cur = queue.poll();
            if (++expanded > MAX_EXPANDED) {
                break;
            }
            if (cur == goalIdx) {
                return rebuild(parent, startIdx, goalIdx);
            }
            int r = rowOf(cur);
            int c = colOf(cur);
            for (Direction d : Direction.values()) {
                int nr = r + d.getDy();
                int nc = c + d.getDx();
                if (nr < 0 || nc < 0 || nr >= GameConfig.MAP_ROWS
                        || nc >= GameConfig.MAP_COLS) {
                    continue;
                }
                int ni = idx(nr, nc);
                if (visited[ni]) {
                    continue;
                }
                boolean walkable = map.getTileType(nr, nc).isTankPassable();
                // 目标格本身可作为终点（哪怕目标贴着不可通行格）
                if (!walkable && ni != goalIdx) {
                    continue;
                }
                visited[ni] = true;
                parent[ni] = cur;
                queue.add(ni);
            }
        }
        return Collections.emptyList();
    }

    /**
     * A* 求到固定基地的路径，允许以高代价穿越砖墙。
     *
     * @param map   地图（只读）
     * @param start 起点格
     * @param goal  目标格
     * @return 路径点（不含起点）；无解返回空表
     */
    public static List<Point> findPathAStar(GameMap map, Point start, Point goal) {
        int total = GameConfig.MAP_ROWS * GameConfig.MAP_COLS;
        final int[] gScore = new int[total];
        final int[] parent = new int[total];
        boolean[] closed = new boolean[total];
        java.util.Arrays.fill(gScore, Integer.MAX_VALUE);
        java.util.Arrays.fill(parent, -1);

        int startIdx = idx(start.y, start.x);
        int goalIdx = idx(goal.y, goal.x);
        gScore[startIdx] = 0;

        PriorityQueue<Integer> open = new PriorityQueue<Integer>(32,
                new java.util.Comparator<Integer>() {
                    @Override
                    public int compare(Integer a, Integer b) {
                        return score(a) - score(b);
                    }

                    private int score(int i) {
                        return gScore[i] + manhattan(i, goalIdx);
                    }
                });
        open.add(startIdx);
        int expanded = 0;

        while (!open.isEmpty()) {
            int cur = open.poll();
            if (closed[cur]) {
                continue;
            }
            closed[cur] = true;
            if (++expanded > MAX_EXPANDED) {
                break;
            }
            if (cur == goalIdx) {
                return rebuild(parent, startIdx, goalIdx);
            }
            int r = rowOf(cur);
            int c = colOf(cur);
            for (Direction d : Direction.values()) {
                int nr = r + d.getDy();
                int nc = c + d.getDx();
                if (nr < 0 || nc < 0 || nr >= GameConfig.MAP_ROWS
                        || nc >= GameConfig.MAP_COLS) {
                    continue;
                }
                int ni = idx(nr, nc);
                if (closed[ni]) {
                    continue;
                }
                TileType type = map.getTileType(nr, nc);
                int stepCost;
                if (ni == goalIdx) {
                    stepCost = COST_OPEN;
                } else if (type.isTankPassable()) {
                    stepCost = COST_OPEN;
                } else if (type == TileType.BRICK) {
                    stepCost = COST_BRICK;
                } else {
                    continue; // 钢墙 / 水 / 基地不可走
                }
                int tentative = gScore[cur] + stepCost;
                if (tentative < gScore[ni]) {
                    gScore[ni] = tentative;
                    parent[ni] = cur;
                    open.add(ni);
                }
            }
        }
        return Collections.emptyList();
    }

    /**
     * 曼哈顿距离启发值。
     *
     * @param i  节点
     * @param gi 目标节点
     * @return 距离
     */
    private static int manhattan(int i, int gi) {
        return Math.abs(rowOf(i) - rowOf(gi)) + Math.abs(colOf(i) - colOf(gi));
    }

    /**
     * 由 parent 数组重建路径（不含起点、含终点）。
     *
     * @param parent 父节点表
     * @param start  起点
     * @param goal   终点
     * @return 路径点
     */
    private static List<Point> rebuild(int[] parent, int start, int goal) {
        List<Point> path = new ArrayList<Point>();
        int cur = goal;
        int guard = 0;
        while (cur != start && cur != -1 && guard++ < MAX_EXPANDED) {
            path.add(new Point(colOf(cur), rowOf(cur)));
            cur = parent[cur];
        }
        Collections.reverse(path);
        return path;
    }
}
