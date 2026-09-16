package com.tankwar.core;

import com.tankwar.constant.GameConfig;
import com.tankwar.input.InputManager;
import com.tankwar.input.Intent;
import com.tankwar.render.GamePanel;
import com.tankwar.util.CrashLog;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 固定时间步长游戏主循环（渲染-逻辑分离）。
 * <p>逻辑按 60 TPS 的固定步长累加器推进，慢帧时最多追
 * {@value GameConfig#MAX_FRAME_MS} 毫秒对应的步数后丢弃积压，避免
 * "死亡螺旋"；渲染每帧绘制一次并尽量对齐 60FPS 休眠。输入意图在每个
 * 逻辑步开始时一次性排空，先由上下文拦截全局键（静音），再分发给当前
 * 状态。本循环运行在独立守护线程上，不阻塞 EDT。</p>
 *
 * @author TankWar Team
 */
public class GameLoop {

    /** 固定逻辑步长（纳秒）。 */
    private static final long STEP_NANO = 1_000_000_000L / GameConfig.TICK_RATE;
    /** 固定逻辑步长（毫秒）。 */
    private static final long STEP_MS = 1000L / GameConfig.TICK_RATE;
    /** 慢帧最多追步数。 */
    private static final int MAX_STEPS_PER_FRAME =
            (int) (GameConfig.MAX_FRAME_MS / (1000L / GameConfig.TICK_RATE));

    private final GameContext context;
    private final InputManager input;
    private final GamePanel panel;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread loopThread;

    private long frameCount;
    private long fpsWindowStart;

    /**
     * 构造主循环。
     *
     * @param context 游戏上下文
     * @param input   输入管理器
     * @param panel   渲染面板
     */
    public GameLoop(GameContext context, InputManager input, GamePanel panel) {
        this.context = context;
        this.input = input;
        this.panel = panel;
    }

    /** 启动循环线程（幂等）。 */
    public void start() {
        if (running.getAndSet(true)) {
            return;
        }
        loopThread = new Thread(new Runnable() {
            @Override
            public void run() {
                runLoop();
            }
        }, "game-loop");
        loopThread.setDaemon(true);
        loopThread.start();
    }

    /** 请求停止循环。 */
    public void stop() {
        running.set(false);
        if (loopThread != null) {
            loopThread.interrupt();
        }
    }

    /** 循环主体。 */
    private void runLoop() {
        long last = System.nanoTime();
        long accumulator = 0L;
        fpsWindowStart = last;
        frameCount = 0L;

        while (running.get()) {
            long frameStart = System.nanoTime();
            long elapsed = frameStart - last;
            last = frameStart;
            // 防止暂停后的巨大时间跳变
            if (elapsed > STEP_NANO * MAX_STEPS_PER_FRAME) {
                elapsed = STEP_NANO * MAX_STEPS_PER_FRAME;
            }
            accumulator += elapsed;

            int steps = 0;
            while (accumulator >= STEP_NANO
                    && steps < MAX_STEPS_PER_FRAME) {
                try {
                    step(STEP_MS);
                } catch (Throwable t) {
                    CrashLog.log("logic step", t);
                }
                accumulator -= STEP_NANO;
                steps++;
            }
            if (steps == MAX_STEPS_PER_FRAME) {
                accumulator = 0L;
            }

            try {
                panel.renderFrame(context);
            } catch (Throwable t) {
                CrashLog.log("render", t);
            }

            frameCount++;
            long now = System.nanoTime();
            if (now - fpsWindowStart >= 1_000_000_000L) {
                context.setFps((int) frameCount);
                frameCount = 0L;
                fpsWindowStart = now;
            }

            long used = System.nanoTime() - frameStart;
            long sleepMs = (STEP_NANO - used) / 1_000_000L;
            try {
                if (sleepMs > 0L) {
                    Thread.sleep(sleepMs);
                } else {
                    Thread.yield();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    /** 推进一个逻辑步：排空输入并更新当前状态。 */
    private void step(long dtMs) {
        List<Intent> raw = input.pollIntents();
        List<Intent> passed = new ArrayList<Intent>(raw.size());
        for (Intent intent : raw) {
            if (!context.handleGlobal(intent)) {
                passed.add(intent);
            }
        }
        context.getState().handleInput(context, passed);
        context.getState().update(context, dtMs);
    }
}
