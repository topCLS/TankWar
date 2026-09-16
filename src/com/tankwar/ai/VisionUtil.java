package com.tankwar.ai;

import com.tankwar.constant.Direction;
import com.tankwar.constant.GameConfig;
import com.tankwar.constant.TileType;
import com.tankwar.map.GameMap;
import com.tankwar.model.Tank;
import com.tankwar.util.CollisionDetector;

/**
 * AI 视觉工具：视野测距、草丛隐蔽、同行同列判定与视线遮挡检测。
 * <p>AI 不会作弊：视野受距离限制、砖墙/钢墙/基地阻挡视线、目标躲进草丛后
 * 有效视野减半。</p>
 *
 * @author TankWar Team
 */
public final class VisionUtil {

    private VisionUtil() {
    }

    /**
     * 能否看见目标。
     *
     * @param map          地图
     * @param observer     观察坦克
     * @param target       目标坦克
     * @param visionTiles  视野半径（格）
     * @return 可见返回 true
     */
    public static boolean canSee(GameMap map, Tank observer, Tank target,
                                 int visionTiles) {
        double dx = observer.getCenterX() - target.getCenterX();
        double dy = observer.getCenterY() - target.getCenterY();
        double distanceTiles = Math.hypot(dx, dy) / GameConfig.TILE_SIZE;

        // 草丛隐蔽：目标藏身草丛时有效视野减半
        int targetRow = GameMap.pixelToRow(target.getCenterY());
        int targetCol = GameMap.pixelToCol(target.getCenterX());
        int effectiveVision = visionTiles;
        if (map.getTileType(targetRow, targetCol) == TileType.GRASS) {
            effectiveVision = Math.max(1, visionTiles / 2);
        }
        if (distanceTiles > effectiveVision) {
            return false;
        }
        return !CollisionDetector.isLineBlocked(map,
                observer.getCenterX(), observer.getCenterY(),
                target.getCenterX(), target.getCenterY(),
                GameConfig.BULLET_SUBSTEP);
    }

    /**
     * 若两坦克严格同行或同列，返回相互朝向的精确四向；否则 null。
     *
     * @param from 观察坦克
     * @param to   目标坦克
     * @return 朝向；非同行同列返回 null
     */
    public static Direction cardinalDir(Tank from, Tank to) {
        int fromRow = GameMap.pixelToRow(from.getCenterY());
        int fromCol = GameMap.pixelToCol(from.getCenterX());
        int toRow = GameMap.pixelToRow(to.getCenterY());
        int toCol = GameMap.pixelToCol(to.getCenterX());
        if (fromCol == toCol) {
            return to.getCenterY() < from.getCenterY() ? Direction.UP : Direction.DOWN;
        }
        if (fromRow == toRow) {
            return to.getCenterX() < from.getCenterX() ? Direction.LEFT : Direction.RIGHT;
        }
        return null;
    }

    /**
     * 按中心点主轴粗判朝向（不要求同行同列）。
     *
     * @param fromX 起点中心 x
     * @param fromY 起点中心 y
     * @param toX   目标中心 x
     * @param toY   目标中心 y
     * @return 四向之一
     */
    public static Direction roughDir(int fromX, int fromY, int toX, int toY) {
        int dx = toX - fromX;
        int dy = toY - fromY;
        if (Math.abs(dx) >= Math.abs(dy)) {
            return dx >= 0 ? Direction.RIGHT : Direction.LEFT;
        }
        return dy >= 0 ? Direction.DOWN : Direction.UP;
    }
}
