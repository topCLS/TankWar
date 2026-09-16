package com.tankwar.core.state;

import com.tankwar.constant.GameConfig;
import com.tankwar.core.GameContext;
import com.tankwar.core.Hud;
import com.tankwar.core.World;
import com.tankwar.input.Intent;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.List;

/**
 * 暂停状态：冻结世界与 AI，叠加暂停面板；继续时恢复原战斗现场。
 *
 * @author TankWar Team
 */
public class PausedState implements GameState {

    private final RunningState running;
    private final int level;
    private final Hud hud = new Hud();

    /**
     * 构造暂停状态。
     *
     * @param running 被暂停的进行状态（用于恢复）
     * @param level   当前关卡序号（用于重开）
     */
    public PausedState(RunningState running, int level) {
        this.running = running;
        this.level = level;
    }

    @Override
    public void onEnter(GameContext context) {
        context.suspendAi();
    }

    @Override
    public void handleInput(GameContext context, List<Intent> intents) {
        for (Intent intent : intents) {
            switch (intent.getKind()) {
                case PAUSE_TOGGLE:
                case CONFIRM:
                    context.restoreState(running);
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
        // 完全冻结：不推进世界
    }

    @Override
    public void render(GameContext context, Graphics2D g) {
        World world = context.getWorld();
        if (world != null) {
            world.draw(g);
            hud.draw(g, world, context);
        }
        g.setColor(new Color(4, 6, 12, 170));
        g.fillRect(0, 0, GameConfig.WINDOW_W, GameConfig.WINDOW_H);

        int cx = GameConfig.FIELD_W / 2;
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 56));
        ReadyState.drawCentered(g, "已 暂 停", cx, 360,
                new Color(255, 220, 110), new Color(50, 38, 0));
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 20));
        ReadyState.drawCentered(g, "P / Enter 继续      R 重开本关      Q 返回菜单",
                cx, 430, new Color(210, 216, 230), null);
    }

    @Override
    public void onExit(GameContext context) {
        context.resumeAi();
    }
}
