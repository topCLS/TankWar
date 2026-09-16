package com.tankwar.map;

import com.tankwar.constant.GameConfig;
import com.tankwar.constant.TileType;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

/**
 * 关卡加载器：把 resources/levels/level_*.txt 文本解析为 {@link LevelData}。
 * <p>文本字符约定：</p>
 * <pre>
 * . 或 0 空地   1 砖墙   2 钢墙   3 草丛   4 河流   9 基地
 * P 玩家1出生   Q 玩家2出生   E 敌方出生
 * </pre>
 * 读取失败、尺寸不符或缺少关键锚点时，回退到内置默认关卡，保证游戏
 * 在任何环境下都能启动。
 *
 * @author TankWar Team
 */
public final class MapLoader {

    private MapLoader() {
    }

    /**
     * 加载指定关卡。
     *
     * @param levelIndex 关卡序号（从 1 开始）
     * @return 关卡数据（永不返回 null）
     */
    public static LevelData load(int levelIndex) {
        List<String> lines = ResourceReader.readLevelLines(levelIndex);
        if (lines != null) {
            try {
                return parse(lines);
            } catch (IllegalArgumentException e) {
                System.err.println("关卡 " + levelIndex + " 格式非法，回退内置关卡："
                        + e.getMessage());
            }
        }
        return parse(buildInLines());
    }

    /**
     * 将文本行解析为关卡数据。
     *
     * @param lines 文本行
     * @return 关卡数据
     */
    private static LevelData parse(List<String> lines) {
        if (lines.size() < GameConfig.MAP_ROWS) {
            throw new IllegalArgumentException(
                    "行数不足：" + lines.size());
        }
        GameMap map = new GameMap();
        List<Point> enemies = new ArrayList<Point>();
        Point p1 = null;
        Point p2 = null;
        Point base = null;

        for (int r = 0; r < GameConfig.MAP_ROWS; r++) {
            String line = lines.get(r);
            if (line.length() < GameConfig.MAP_COLS) {
                throw new IllegalArgumentException(
                        "第 " + r + " 行宽度不足：" + line.length());
            }
            for (int c = 0; c < GameConfig.MAP_COLS; c++) {
                char ch = line.charAt(c);
                switch (ch) {
                    case '1':
                        map.setTileType(r, c, TileType.BRICK);
                        break;
                    case '2':
                        map.setTileType(r, c, TileType.STEEL);
                        break;
                    case '3':
                        map.setTileType(r, c, TileType.GRASS);
                        break;
                    case '4':
                        map.setTileType(r, c, TileType.WATER);
                        break;
                    case '9':
                        map.setTileType(r, c, TileType.BASE);
                        base = new Point(c, r);
                        break;
                    case 'P':
                        p1 = new Point(c, r);
                        break;
                    case 'Q':
                        p2 = new Point(c, r);
                        break;
                    case 'E':
                        enemies.add(new Point(c, r));
                        break;
                    default:
                        // '.' '0' 及一切未知字符按空地处理（容错）
                        break;
                }
            }
        }

        if (base == null || p1 == null || p2 == null || enemies.size() < 3) {
            throw new IllegalArgumentException("缺少基地或出生点锚点");
        }
        return new LevelData(map, enemies.subList(0, 3), p1, p2, base);
    }

    /**
     * 内置兜底关卡（与外置 level_1 同构的 26x26 对称战场）。
     *
     * @return 文本行
     */
    private static List<String> buildInLines() {
        String[] rows = {
                "E...........E...........E.",
                "..........................",
                "...11.....11....11.....11.",
                "...11.....11....11.....11.",
                "...11.....11....11.....11.",
                "...11.....11....11.....11.",
                "............22............",
                "..........................",
                ".11111...111..111...11111.",
                "..........................",
                "........33333333..........",
                "........33333333..........",
                ".....2..........2.........",
                "..........................",
                "...4444..........4444.....",
                "...4444..........4444.....",
                "..........................",
                "..........................",
                ".1.1...1.1......1.1...1.1.",
                ".1.1...1.1......1.1...1.1.",
                "..........................",
                ".11..............11.......",
                "..........................",
                "..........................",
                "........P...111...Q.......",
                "............191...........",
        };
        List<String> list = new ArrayList<String>();
        for (String row : rows) {
            list.add(row);
        }
        return list;
    }

    /**
     * 资源读取小包装（便于测试时替换）。
     */
    private static final class ResourceReader {
        static List<String> readLevelLines(int levelIndex) {
            return com.tankwar.util.ResourceLoader.readLines(
                    GameConfig.LEVEL_DIR + "level_" + levelIndex + ".txt");
        }
    }
}
