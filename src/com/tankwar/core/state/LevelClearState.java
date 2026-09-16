package com.tankwar.core.state;

import com.tankwar.constant.GameConfig;
import com.tankwar.core.GameContext;
import com.tankwar.core.Hud;
import com.tankwar.core.World;
import com.tankwar.input.Intent;
import com.tankwar.model.PlayerTank;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.List;

/**
 * 关卡通过状态：保留战场与爆炸余韵，展示结算分数，Enter 进入下一关。
 *
 * @author TankWar Team
 */
public class LevelClearState implements GameState {

    private final int clearedLevel;
    private final Hud hud = new Hud();

    /**
     * 构造过关状态。
     *
     * @param clearedLevel 已通过的关卡序号
     */
    public LevelClearState(int clearedLevel) {
        this.clearedLevel = clearedLevel;
    }

    @Override
    public void onEnter(GameContext context) {
        // AI 已由 RunningState.onExit 暂停
    }

    @Override
    public void handleInput(GameContext context, List<Intent> intents) {
        for (Intent intent : intents) {
            if (intent.getKind() == Intent.Kind.CONFIRM
                    || intent.getKind() == Intent.Kind.PAUSE_TOGGLE) {
                int next = context.getLevelManager().nextLevel();
                context.setState(new RunningState(next));
                return;
            }
            if (intent.getKind() == Intent.Kind.QUIT_TO_MENU) {
                context.backToMenu();
                return;
            }
        }
    }

    @Override
    public void update(GameContext context, long dtMs) {
        World world = context.getWorld();
        // 过关后世界仅推进特效（爆炸/火花），不再刷怪与移动
        if (world != null) {
            world.update(dtMs, System.currentTimeMillis());
        }
    }

    @Override
    public void render(GameContext context, Graphics2D g) {
        World world = context.getWorld();
        if (world != null) {
            world.draw(g);
            hud.draw(g, world, context);
        }
        g.setColor(new Color(6, 12, 8, 165));
        g.fillRect(0, 0, GameConfig.FIELD_W, GameConfig.FIELD_H);

        int cx = GameConfig.FIELD_W / 2;
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 52));
        ReadyState.drawCentered(g, "关卡 " + clearedLevel + " 完成！",
                cx, 320, new Color(120, 235, 150), new Color(10, 50, 20));

        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        int y = 390;
        if (world != null) {
            for (PlayerTank p : world.getPlayers()) {
                ReadyState.drawCentered(g,
                        "玩家" + p.getPlayerId() + " 总分："
                                + context.getScoreManager().getScore(p.getPlayerId()),
                        cx, y, new Color(235, 240, 250), null);
                y += 34;
            }
        }
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 20));
        ReadyState.drawCentered(g, "按 Enter 进入下一关      Q 返回菜单",
                cx, y + 20, new Color(205, 212, 226), null);
    }

    @Override
    public void onExit(GameContext context) {
        // 无需清理
    }
}
