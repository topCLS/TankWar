package com.tankwar.map;

import java.awt.Point;
import java.util.Collections;
import java.util.List;

/**
 * 单关加载结果：地图 + 各出生点（格子坐标）+ 基地位置。
 *
 * @author TankWar Team
 */
public class LevelData {

    /** 地形地图。 */
    private final GameMap map;
    /** 敌方出生点（Point.x=列, Point.y=行）。 */
    private final List<Point> enemySpawns;
    /** 玩家 1 出生格。 */
    private final Point player1Spawn;
    /** 玩家 2 出生格。 */
    private final Point player2Spawn;
    /** 基地所在格。 */
    private final Point baseCell;

    /**
     * 构造关卡数据。
     *
     * @param map           地图
     * @param enemySpawns   敌方出生点列表
     * @param player1Spawn  玩家1出生格
     * @param player2Spawn  玩家2出生格
     * @param baseCell      基地格
     */
    public LevelData(GameMap map, List<Point> enemySpawns, Point player1Spawn,
                     Point player2Spawn, Point baseCell) {
        this.map = map;
        this.enemySpawns = Collections.unmodifiableList(enemySpawns);
        this.player1Spawn = player1Spawn;
        this.player2Spawn = player2Spawn;
        this.baseCell = baseCell;
    }

    /** @return 地图。 */
    public GameMap getMap() {
        return map;
    }

    /** @return 敌方出生点列表。 */
    public List<Point> getEnemySpawns() {
        return enemySpawns;
    }

    /** @return 玩家1出生格。 */
    public Point getPlayer1Spawn() {
        return player1Spawn;
    }

    /** @return 玩家2出生格。 */
    public Point getPlayer2Spawn() {
        return player2Spawn;
    }

    /** @return 基地格。 */
    public Point getBaseCell() {
        return baseCell;
    }
}
