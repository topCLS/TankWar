package com.tankwar.core.state;

import com.tankwar.core.GameContext;
import com.tankwar.core.Hud;
import com.tankwar.core.World;
import com.tankwar.input.Intent;
import com.tankwar.model.PlayerTank;

import java.awt.Graphics2D;
import java.util.List;

/**
 * 战斗进行状态：驱动 {@link World} 固定步推进，转发开火，处理暂停 /
 * 重开 / 退出，并在过关、失败或对战胜负出现时迁移到对应状态。
 * <p>进入本状态时创建全新世界并恢复 AI 调度；离开时暂停 AI 调度。
 * 从暂停恢复时由 {@link GameContext#restoreState} 直接切回、不触发
 * {@link #onEnter}，从而保留战斗现场。</p>
 *
 * @author TankWar Team
 */
public class RunningState implements GameState {

    /** 本状态关卡序号。 */
    private final int level;
    /** 信息栏（无状态，复用同一实例）。 */
    private final Hud hud = new Hud();
    /** 过关/失败迁移是否已结算（防止同一帧重复发奖）。 */
    private boolean resolved = false;

    /**
     * 构造进行状态。
     *
     * @param level 关卡序号（从 1 开始）
     */
    public RunningState(int level) {
        this.level = level;
    }

    /** @return 关卡序号。 */
    public int getLevel() {
        return level;
    }

    @Override
    public void onEnter(GameContext context) {
        World world = new World(context, level);
        context.setWorld(world);
        context.getLevelManager().setLevel(level);
        context.resumeAi();
    }

    @Override
    public void handleInput(GameContext context, List<Intent> intents) {
        World world = context.getWorld();
        if (world == null) {
            return;
        }
        for (Intent intent : intents) {
            switch (intent.getKind()) {
                case FIRE:
                    world.requestFire(intent.getPlayerId());
                    break;
                case PAUSE_TOGGLE:
                    context.setState(new PausedState(this, level));
                    return;
                case RESTART:
                    context.setState(new RunningState(level));
                    return;
                case QUIT_TO_MENU:
                    context.backToMenu();
                    return;
                default:
                    break;
            }
        }
    }

    @Override
    public void update(GameContext context, long dtMs) {
        World world = context.getWorld();
        if (world == null || resolved) {
            return;
        }
        long now = System.currentTimeMillis();
        world.update(dtMs, now);

        if (world.isFailed()) {
            resolved = true;
            context.getAudio().playOnce("gameover.wav");
            context.setState(new GameOverState(false, -1));
            return;
        }
        int versusWinner = world.getVersusWinner();
        if (versusWinner >= 0) {
            resolved = true;
            context.getAudio().playOnce("gameover.wav");
            context.setState(new GameOverState(false, versusWinner));
            return;
        }
        if (world.isLevelCleared()) {
            resolved = true;
            awardClearBonus(context, world);
            context.getAudio().playOnce("levelclear.wav");
            if (context.getLevelManager().hasNext()) {
                context.setState(new LevelClearState(level));
            } else {
                context.setState(new GameOverState(true, -1));
            }
        }
    }

    /** 过关奖励：每名玩家固定通关分 + 剩余生命奖励分。 */
    private void awardClearBonus(GameContext context, World world) {
        for (PlayerTank p : world.getPlayers()) {
            int id = p.getPlayerId();
            context.getScoreManager().addScore(id, 500,
                    com.tankwar.constant.ScoreReason.LEVEL_CLEAR);
            context.getScoreManager().addScore(id, p.getLives() * 200,
                    com.tankwar.constant.ScoreReason.LIVES_BONUS);
        }
    }

    @Override
    public void render(GameContext context, Graphics2D g) {
        World world = context.getWorld();
        if (world != null) {
            world.draw(g);
            hud.draw(g, world, context);
        }
    }

    @Override
    public void onExit(GameContext context) {
        context.suspendAi();
    }
}
