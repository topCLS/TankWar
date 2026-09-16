package com.tankwar;

import com.tankwar.constant.Difficulty;
import com.tankwar.constant.GameMode;
import com.tankwar.core.GameContext;
import com.tankwar.core.World;
import com.tankwar.input.InputManager;
import com.tankwar.model.PlayerTank;
import com.tankwar.util.SpriteCache;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 真实时间节奏诊断：用独立调度线程按 200ms 驱动 AI（复刻 GUI），
 * 主线程按 60TPS 推进世界，周期打印基地/生命/敌人状态，定位失败原因。
 */
public final class DiagTest {

    private DiagTest() {
    }

    /**
     * 入口。
     *
     * @param args 未使用
     */
    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        SpriteCache.getInstance().preload();

        InputManager input = new InputManager();
        final GameContext context = new GameContext(null, input);
        context.setMode(GameMode.SOLO);
        context.setDifficulty(Difficulty.NORMAL);
        context.getScoreManager().reset();
        context.getLevelManager().reset();

        final World world = new World(context, 1);
        context.setWorld(world);

        ScheduledExecutorService ai = Executors.newSingleThreadScheduledExecutor();
        ai.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                world.runAiDecisions(System.currentTimeMillis());
            }
        }, 0, 200, TimeUnit.MILLISECONDS);

        long start = System.currentTimeMillis();
        int tick = 0;
        while (true) {
            long now = System.currentTimeMillis();
            world.update(16L, now);
            tick++;
            if (tick % 120 == 0) {
                StringBuilder sb = new StringBuilder();
                sb.append(String.format("t=%4.1fs aliveEnemies=%d remain=%d bullets=%d",
                        (now - start) / 1000.0,
                        world.getEnemies().size(),
                        world.getEnemiesRemaining(),
                        world.getBulletCount()));
                if (world.getBase() != null) {
                    sb.append(" baseDestroyed=").append(world.getBase().isDestroyed());
                }
                for (PlayerTank p : world.getPlayers()) {
                    sb.append(" p").append(p.getPlayerId())
                            .append("(alive=").append(p.isAlive())
                            .append(",lives=").append(p.getLives()).append(")");
                }
                System.out.println(sb.toString());
            }
            if (world.isFailed() || world.isLevelCleared()) {
                long dur = System.currentTimeMillis() - start;
                System.out.println("=== END after " + dur + "ms failed="
                        + world.isFailed() + " cleared=" + world.isLevelCleared()
                        + " baseDestroyed="
                        + (world.getBase() != null && world.getBase().isDestroyed()));
                break;
            }
            if (now - start > 60000) {
                System.out.println("=== 60s timeout, no end");
                break;
            }
            Thread.sleep(16L);
        }
        ai.shutdownNow();
    }
}
