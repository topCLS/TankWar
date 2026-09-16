package com.tankwar.core;

import com.tankwar.constant.Difficulty;
import com.tankwar.constant.GameMode;
import com.tankwar.core.state.GameState;
import com.tankwar.core.state.ReadyState;
import com.tankwar.input.InputManager;
import com.tankwar.input.Intent;
import com.tankwar.map.LevelManager;
import com.tankwar.render.GamePanel;
import com.tankwar.util.AudioPlayer;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * 游戏上下文（状态机拥有者 + 跨子系统中介）。
 * <p>持有当前状态、当前世界、输入、计分、关卡、音频与并发设施：
 * 除游戏主循环线程外，另用 2 线程的 {@link ScheduledExecutorService}
 * 承担 AI 周期决策与性能监控；状态切换负责暂停/恢复 AI 定时任务。</p>
 *
 * @author TankWar Team
 */
public class GameContext {

    private final GamePanel panel;
    private final InputManager input;
    private final ScoreManager scoreManager = new ScoreManager();
    private final LevelManager levelManager = new LevelManager();
    private final AudioPlayer audio = AudioPlayer.getInstance();

    private GameMode mode = GameMode.SOLO;
    private Difficulty difficulty = Difficulty.NORMAL;

    private volatile GameState currentState;
    private volatile World world;
    private volatile int fps;

    /** 附属调度线程池（AI 决策 / 性能监控），线程均为守护线程。 */
    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(2, new ThreadFactory() {
                private int index = 0;

                @Override
                public Thread newThread(Runnable r) {
                    Thread t = new Thread(r, "game-scheduled-" + (index++));
                    t.setDaemon(true);
                    return t;
                }
            });
    private volatile ScheduledFuture<?> aiFuture;
    private ScheduledFuture<?> monitorFuture;

    /**
     * 构造上下文并安装输入。
     *
     * @param panel 游戏面板
     * @param input 输入管理器
     */
    public GameContext(GamePanel panel, InputManager input) {
        this.panel = panel;
        this.input = input;
    }

    /** 启动调度任务并进入主菜单。 */
    public void start() {
        scheduleAi();
        monitorFuture = scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                World w = world;
                System.out.println("[MONITOR] FPS=" + fps
                        + (w != null ? " 敌人=" + w.getEnemies().size()
                        + " 子弹=" + w.getBulletCount() : " 菜单"));
            }
        }, 1, 1, TimeUnit.SECONDS);
        audio.loopBgm("bgm.wav");
        setState(new ReadyState());
    }

    /** 调度 AI 周期决策（200ms 节拍，真正重算由 AIBrain 内部节流）。 */
    private synchronized void scheduleAi() {
        if (aiFuture != null && !aiFuture.isDone()) {
            return;
        }
        aiFuture = scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                World w = world;
                if (w != null && currentState instanceof com.tankwar.core.state.RunningState) {
                    w.runAiDecisions(System.currentTimeMillis());
                }
            }
        }, 200, 200, TimeUnit.MILLISECONDS);
    }

    /** 暂停 AI 定时任务（进入暂停/结算状态）。 */
    public synchronized void suspendAi() {
        if (aiFuture != null) {
            aiFuture.cancel(false);
            aiFuture = null;
        }
    }

    /** 恢复 AI 定时任务（回到进行状态）。 */
    public synchronized void resumeAi() {
        scheduleAi();
    }

    /**
     * 状态迁移：先 onExit 旧状态，再切换并 onEnter 新状态。
     *
     * @param state 新状态
     */
    public void setState(GameState state) {
        GameState old = currentState;
        if (old != null) {
            old.onExit(this);
        }
        input.clear();
        currentState = state;
        state.onEnter(this);
    }

    /**
     * 从暂停恢复到已有状态：调用旧状态 onExit，但不触发新状态 onEnter
     * （避免重建世界）。
     *
     * @param state 被恢复的状态
     */
    public void restoreState(GameState state) {
        GameState old = currentState;
        if (old != null) {
            old.onExit(this);
        }
        input.clear();
        currentState = state;
    }

    /**
     * 配置并开始一局新游戏。
     *
     * @param mode       模式
     * @param difficulty 难度
     */
    public void configureAndStart(GameMode mode, Difficulty difficulty) {
        this.mode = mode;
        this.difficulty = difficulty;
        scoreManager.reset();
        levelManager.reset();
        audio.playOnce("start.wav");
        setState(new com.tankwar.core.state.RunningState(1));
    }

    /** 返回主菜单。 */
    public void backToMenu() {
        suspendAi();
        world = null;
        setState(new ReadyState());
    }

    /**
     * 处理全局意图（静音等），在分发到具体状态前拦截。
     *
     * @param intent 意图
     * @return 已消费返回 true
     */
    public boolean handleGlobal(Intent intent) {
        if (intent.getKind() == Intent.Kind.MUTE_TOGGLE) {
            boolean muted = !audio.isMuted();
            audio.setMuted(muted);
            if (!muted) {
                audio.loopBgm("bgm.wav");
            }
            return true;
        }
        return false;
    }

    /** 关闭全部线程（窗口关闭时）。 */
    public void shutdown() {
        suspendAi();
        if (monitorFuture != null) {
            monitorFuture.cancel(false);
        }
        scheduler.shutdownNow();
        audio.shutdown();
    }

    /** @return 当前状态。 */
    public GameState getState() {
        return currentState;
    }

    /** @return 面板。 */
    public GamePanel getPanel() {
        return panel;
    }

    /** @return 输入。 */
    public InputManager getInput() {
        return input;
    }

    /** @return 计分器。 */
    public ScoreManager getScoreManager() {
        return scoreManager;
    }

    /** @return 关卡管理器。 */
    public LevelManager getLevelManager() {
        return levelManager;
    }

    /** @return 音频播放器。 */
    public AudioPlayer getAudio() {
        return audio;
    }

    /** @return 模式。 */
    public GameMode getMode() {
        return mode;
    }

    /** @return 难度。 */
    public Difficulty getDifficulty() {
        return difficulty;
    }

    /** @param mode 设置模式。 */
    public void setMode(GameMode mode) {
        this.mode = mode;
    }

    /** @param difficulty 设置难度。 */
    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    /** @return 当前世界。 */
    public World getWorld() {
        return world;
    }

    /** @param world 设置当前世界。 */
    public void setWorld(World world) {
        this.world = world;
    }

    /** @return 当前 FPS。 */
    public int getFps() {
        return fps;
    }

    /** @param fps 设置 FPS。 */
    public void setFps(int fps) {
        this.fps = fps;
    }
}
