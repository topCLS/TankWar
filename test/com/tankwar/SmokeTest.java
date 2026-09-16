package com.tankwar;

import com.tankwar.constant.Difficulty;
import com.tankwar.constant.GameMode;
import com.tankwar.core.GameContext;
import com.tankwar.core.World;
import com.tankwar.input.InputManager;
import com.tankwar.map.GameMap;
import com.tankwar.util.SpriteCache;

/**
 * 无头冒烟测试：不创建窗口，直接驱动世界跑数千逻辑步，
 * 验证地图加载、刷怪、AI 决策、开火、分步扫掠碰撞与对象回收全链路不异常。
 */
public final class SmokeTest {

    private SmokeTest() {
    }

    /**
     * 入口。
     *
     * @param args 未使用
     */
    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "true");
        SpriteCache.getInstance().preload();

        InputManager input = new InputManager();
        GameContext context = new GameContext(null, input);
        context.setMode(GameMode.SOLO);
        context.setDifficulty(Difficulty.NORMAL);
        context.getScoreManager().reset();
        context.getLevelManager().reset();

        World world = new World(context, 1);
        context.setWorld(world);

        int maxEnemies = 0;
        int maxBullets = 0;
        long base = System.currentTimeMillis();
        int steps = 0;
        for (int i = 0; i < 3000; i++) {
            long virtualNow = base + i * 16L;
            if (i % 5 == 0) {
                world.runAiDecisions(virtualNow);
            }
            world.update(16L, virtualNow);
            steps++;
            maxEnemies = Math.max(maxEnemies, world.getEnemies().size());
            maxBullets = Math.max(maxBullets, world.getBulletCount());
            if (world.isFailed() || world.isLevelCleared()
                    || world.getVersusWinner() >= 0) {
                break;
            }
        }

        GameMap map = world.getMap();
        System.out.println("=== SMOKE RESULT ===");
        System.out.println("steps=" + steps);
        System.out.println("maxConcurrentEnemies=" + maxEnemies);
        System.out.println("maxBullets=" + maxBullets);
        System.out.println("enemiesRemaining=" + world.getEnemiesRemaining());
        System.out.println("failed=" + world.isFailed()
                + " cleared=" + world.isLevelCleared());
        System.out.println("mapRows/cols via tile(0,0)=" + map.getTileType(0, 0));
        System.out.println("p1score=" + context.getScoreManager().getScore(1));

        if (map == null) {
            throw new AssertionError("地图未加载");
        }
        if (maxEnemies <= 0) {
            throw new AssertionError("从未刷出敌方坦克");
        }
        if (maxBullets <= 0) {
            throw new AssertionError("从未产生子弹（开火/对象池链路异常）");
        }
        if (world.getPlayers().isEmpty()) {
            throw new AssertionError("玩家坦克缺失");
        }
        System.out.println("SMOKE TEST PASSED");
    }
}
