package com.tankwar;

import com.tankwar.ai.PathFinder;
import com.tankwar.constant.Difficulty;
import com.tankwar.constant.Direction;
import com.tankwar.constant.GameConfig;
import com.tankwar.constant.GameMode;
import com.tankwar.constant.PowerUpType;
import com.tankwar.constant.TileType;
import com.tankwar.constant.TankType;
import com.tankwar.core.GameContext;
import com.tankwar.core.ScoreManager;
import com.tankwar.core.World;
import com.tankwar.input.InputManager;
import com.tankwar.map.GameMap;
import com.tankwar.map.LevelData;
import com.tankwar.map.LevelManager;
import com.tankwar.map.MapLoader;
import com.tankwar.map.TileHitResult;
import com.tankwar.model.AITank;
import com.tankwar.model.PlayerTank;
import com.tankwar.prop.EffectManager;
import com.tankwar.prop.PowerUp;
import com.tankwar.prop.PowerUpFactory;
import com.tankwar.prop.SpeedBoost;
import com.tankwar.util.CollisionDetector;
import com.tankwar.util.SpriteCache;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 无第三方依赖的断言测试套件（JUnit 风格）。
 * <p>覆盖：碰撞检测 5 例、寻路 3 例、地图与破坏 4 例、计分 2 例、
 * 道具效果 3 例、世界开火/受伤集成 4 例。任意断言失败以非零码退出。
 * 用 {@code java -cp classes;testclasses;resources com.tankwar.TestRunner} 运行。</p>
 *
 * @author TankWar Team
 */
public final class TestRunner {

    private static int passed = 0;
    private static int failed = 0;

    private TestRunner() {
    }

    /**
     * 入口。
     *
     * @param args 未使用
     */
    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        SpriteCache.getInstance().preload();

        collisionTests();
        pathTests();
        mapTests();
        scoreTests();
        powerUpTests();
        worldIntegrationTests();

        System.out.println();
        System.out.println("================ 测试结果 ================");
        System.out.println("通过: " + passed + "   失败: " + failed);
        if (failed > 0) {
            System.exit(1);
        }
        System.out.println("全部测试通过 ✔");
    }

    /* --------------------------- 碰撞 5 例 --------------------------- */

    private static void collisionTests() {
        group("碰撞检测");

        GameMap map = new GameMap();
        // C1 空地可占据
        check("C1 空地坦克可占据",
                CollisionDetector.canTankOccupy(map, 0, 0, GameConfig.TANK_SIZE));

        // C2 覆盖钢墙不可占据
        for (int r = 2; r < 4; r++) {
            for (int c = 2; c < 4; c++) {
                map.setTileType(r, c, TileType.STEEL);
            }
        }
        check("C2 压在钢墙上不可占据",
                !CollisionDetector.canTankOccupy(map, 2 * 32 + 2, 2 * 32 + 2,
                        GameConfig.TANK_SIZE));

        // C3 向上移动被砖墙挡住，不会越界进入砖格
        GameMap map2 = new GameMap();
        for (int c = 0; c < 2; c++) {
            map2.setTileType(4, c, TileType.BRICK);
        }
        int tankSize = GameConfig.TANK_SIZE;
        int startY = 5 * 32 + 2; // 28px 坦克在第 5 行
        int[] moved = CollisionDetector.tryMoveAxis(map2, 2, startY,
                tankSize, Direction.UP, 2);
        check("C3 撞墙贴齐且未进入砖墙",
                moved[1] >= 5 * 32 && moved[1] <= 5 * 32 + 2);

        // C4 高速子弹单步跨越坦克，扫掠仍命中（防穿透）
        Rectangle tank = new Rectangle(100, 100, 28, 28);
        boolean hit = CollisionDetector.sweptAABB(tank,
                114, 40, 114, 200, 8);
        check("C4 高速跨越扫掠命中", hit);

        // C5 贴着坦克旁边平行飞过不命中
        boolean miss = CollisionDetector.sweptAABB(tank,
                40, 60, 60, 60, 8);
        check("C5 旁侧平行飞行不命中", !miss);
    }

    /* --------------------------- 寻路 3 例 --------------------------- */

    private static void pathTests() {
        group("寻路算法");

        // P1 空地图 BFS 可达且终点正确
        GameMap open = new GameMap();
        List<Point> p1 = PathFinder.findPathBfs(open,
                new Point(2, 2), new Point(20, 20));
        boolean reachesGoal = !p1.isEmpty()
                && p1.get(p1.size() - 1).equals(new Point(20, 20));
        check("P1 开阔地 BFS 找到路径并到达目标", reachesGoal);

        // P2 整列钢墙隔断，BFS 无解
        GameMap blocked = new GameMap();
        for (int r = 0; r < GameConfig.MAP_ROWS; r++) {
            blocked.setTileType(r, 12, TileType.STEEL);
        }
        List<Point> p2 = PathFinder.findPathBfs(blocked,
                new Point(3, 13), new Point(20, 13));
        check("P2 钢墙完全隔断时 BFS 返回空路径", p2.isEmpty());

        // P3 同样位置换成砖墙：BFS 不通，A* 允许打穿砖墙可通
        GameMap brickWall = new GameMap();
        for (int r = 0; r < GameConfig.MAP_ROWS; r++) {
            brickWall.setTileType(r, 12, TileType.BRICK);
        }
        List<Point> p3bfs = PathFinder.findPathBfs(brickWall,
                new Point(3, 13), new Point(20, 13));
        List<Point> p3astar = PathFinder.findPathAStar(brickWall,
                new Point(3, 13), new Point(20, 13));
        check("P3 砖墙隔断: BFS 不通而 A* 可穿砖到达",
                p3bfs.isEmpty() && !p3astar.isEmpty()
                        && p3astar.get(p3astar.size() - 1).equals(new Point(20, 13)));
    }

    /* --------------------------- 地图/破坏 4 例 --------------------------- */

    private static void mapTests() {
        group("地图加载与地形破坏");

        // M1 第 1 关 26x26、基地与三类出生点齐全
        LevelData l1 = MapLoader.load(1);
        check("M1 关卡1基地格存在且在底部",
                l1.getBaseCell() != null && l1.getBaseCell().y >= 24);
        check("M1 关卡1含3个敌方出生点",
                l1.getEnemySpawns().size() == 3);
        check("M1 关卡1两名玩家出生点存在",
                l1.getPlayer1Spawn() != null && l1.getPlayer2Spawn() != null);

        // M2 三关均可加载且每关敌方编成 20 辆
        LevelManager lm = new LevelManager();
        boolean plansOk = true;
        for (int lv = 1; lv <= LevelManager.LEVEL_COUNT; lv++) {
            LevelData data = MapLoader.load(lv);
            if (data.getMap() == null
                    || lm.enemyPlan(lv).size() != GameConfig.ENEMY_TOTAL_PER_LEVEL) {
                plansOk = false;
            }
        }
        check("M2 三关可加载且每关敌坦20辆", plansOk);

        // M3 砖墙普通弹两发摧毁
        GameMap brick = new GameMap();
        brick.setTileType(5, 5, TileType.BRICK);
        TileHitResult dmg1 = brick.damageTile(5, 5, 1);
        TileHitResult dmg2 = brick.damageTile(5, 5, 1);
        check("M3 砖墙普通弹先受损后摧毁",
                dmg1 == TileHitResult.BRICK_DAMAGED
                        && dmg2 == TileHitResult.BRICK_DESTROYED
                        && brick.getTileType(5, 5) == TileType.EMPTY);

        // M4 钢墙普通弹不可破、强化弹可破
        GameMap steel = new GameMap();
        steel.setTileType(6, 6, TileType.STEEL);
        TileHitResult block = steel.damageTile(6, 6, 1);
        TileHitResult pierce = steel.damageTile(6, 6, 2);
        check("M4 钢墙普通弹抵挡、穿甲弹击穿",
                block == TileHitResult.STEEL_BLOCKED
                        && pierce == TileHitResult.STEEL_DESTROYED);
    }

    /* --------------------------- 计分 2 例 --------------------------- */

    private static void scoreTests() {
        group("计分与最高分");

        ScoreManager sm = new ScoreManager();
        sm.addScore(1, 100, com.tankwar.constant.ScoreReason.KILL_ENEMY);
        sm.addScore(1, 50, com.tankwar.constant.ScoreReason.PICKUP_POWERUP);
        sm.addScore(2, 300, com.tankwar.constant.ScoreReason.KILL_ENEMY);
        check("S1 分玩家累计分数正确",
                sm.getScore(1) == 150 && sm.getScore(2) == 300);

        int base = sm.getHighScore();
        boolean raised = sm.commitHighScore(base + 500);
        boolean rejected = sm.commitHighScore(base + 100);
        check("S2 最高分仅在被超越时更新",
                raised && !rejected && sm.getHighScore() == base + 500);
    }

    /* --------------------------- 道具 3 例 --------------------------- */

    private static void powerUpTests() {
        group("道具效果与到期还原");

        // 游戏全程使用真实墙钟，测试也必须使用真实时间
        long now = System.currentTimeMillis();
        PlayerTank p = new PlayerTank(1, 5, 5);
        EffectManager fx = new EffectManager();
        fx.registerTank(p);

        // U1 加速：2 -> 3，到期还原 2
        fx.apply(p, new SpeedBoost(5, 5, now), now);
        boolean boosted = p.getSpeed() == 3;
        fx.update(now + PowerUpType.SPEED_BOOST.getDurationMs() + 1);
        check("U1 加速生效并在到期后还原速度",
                boosted && p.getSpeed() == GameConfig.PLAYER_SPEED);

        // U2 穿甲弹：1 -> 2，到期还原
        PowerUp star = PowerUpFactory.create(PowerUpType.POWER_BULLET, 5, 5, now);
        fx.apply(p, star, now);
        boolean powered = p.getBulletPower() == 2;
        fx.update(now + PowerUpType.POWER_BULLET.getDurationMs() + 1);
        check("U2 穿甲弹生效并在到期后还原", powered && p.getBulletPower() == 1);

        // U3 护盾期免伤
        PlayerTank p2 = new PlayerTank(1, 8, 8);
        EffectManager fx2 = new EffectManager();
        fx2.registerTank(p2);
        PowerUp shield = PowerUpFactory.create(PowerUpType.SHIELD, 8, 8, now);
        fx2.apply(p2, shield, now);
        boolean immune = !p2.takeDamage(1, now + 1000);
        fx2.update(now + PowerUpType.SHIELD.getDurationMs() + 1);
        boolean vulnerableAfter = p2.takeDamage(1,
                now + PowerUpType.SHIELD.getDurationMs() + 2000);
        check("U3 护盾期免伤、到期后可受伤", immune && vulnerableAfter);

        // 工厂加权抽取可覆盖全部三种类型
        Set<PowerUpType> seen = new HashSet<PowerUpType>();
        java.util.Random rnd = new java.util.Random(7);
        for (int i = 0; i < 200; i++) {
            seen.add(PowerUpFactory.randomCreate(1, 1, now, rnd).getType());
        }
        check("U4 工厂加权抽取覆盖三种道具", seen.size() == 3);
    }

    /* --------------------------- 世界集成 4 例 --------------------------- */

    private static void worldIntegrationTests() {
        group("世界开火与受伤集成");

        GameContext context = newContext();
        World world = new World(context, 1);

        // W1 玩家开火在炮口生成 1 发子弹
        world.requestFire(1);
        check("W1 玩家开火生成一发子弹", world.getBulletCount() == 1);

        // W2 冷却与子弹数量上限
        sleep(340);
        world.requestFire(1);                 // 第二发（冷却已过）
        world.requestFire(1);                 // 冷却中，被忽略
        check("W2 开火受冷却限制，场上2发", world.getBulletCount() == 2);

        // W3 AI 出生保护结束后可被击杀
        long past = System.currentTimeMillis() - 3000L;
        AITank enemy = new AITank(TankType.AI_BASIC, 10, 10,
                Difficulty.NORMAL, past);
        boolean lethal = enemy.takeDamage(1, past + 3000);
        check("W3 敌方坦克保护期外可被击毁",
                lethal && !enemy.isAlive());

        // W4 世界持续推进 1000 步不异常且刷怪
        GameContext c2 = newContext();
        World w2 = new World(c2, 1);
        long base = System.currentTimeMillis();
        boolean sawEnemy = false;
        for (int i = 0; i < 1200; i++) {
            long now = base + i * 16L;
            if (i % 5 == 0) {
                w2.runAiDecisions(now);
            }
            w2.update(16L, now);
            if (!w2.getEnemies().isEmpty()) {
                sawEnemy = true;
            }
            if (w2.isFailed() || w2.isLevelCleared()) {
                break;
            }
        }
        check("W4 世界推进刷出敌方且全程无异常", sawEnemy);
    }

    /* --------------------------- 辅助 --------------------------- */

    private static GameContext newContext() {
        InputManager input = new InputManager();
        GameContext context = new GameContext(null, input);
        context.setMode(GameMode.SOLO);
        context.setDifficulty(Difficulty.NORMAL);
        context.getScoreManager().reset();
        context.getLevelManager().reset();
        return context;
    }

    private static void group(String name) {
        System.out.println();
        System.out.println("【" + name + "】");
    }

    private static void check(String name, boolean condition) {
        if (condition) {
            passed++;
            System.out.println("  [PASS] " + name);
        } else {
            failed++;
            System.out.println("  [FAIL] " + name);
        }
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
