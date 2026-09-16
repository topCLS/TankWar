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
 * 游戏结束状态：覆盖单人/合作失败、全部通关胜利与双人对战胜负三种结算，
 * 提交并展示历史最高分，Enter 返回主菜单。
 *
 * @author TankWar Team
 */
public class GameOverState implements GameState {

    /** 是否全关通关胜利。 */
    private final boolean victory;
    /** 对战胜方：-1 非对战，0 平局，1/2 玩家。 */
    private final int versusWinner;
    private final Hud hud = new Hud();

    /** 本局总分。 */
    private int totalScore;
    /** 是否刷新了最高分纪录。 */
    private boolean newRecord;

    /**
     * 构造结算状态。
     *
     * @param victory      是否全关通关
     * @param versusWinner 对战胜方（-1 表示非对战）
     */
    public GameOverState(boolean victory, int versusWinner) {
        this.victory = victory;
        this.versusWinner = versusWinner;
    }

    @Override
    public void onEnter(GameContext context) {
        context.suspendAi();
        World world = context.getWorld();
        int total = 0;
        if (world != null) {
            for (PlayerTank p : world.getPlayers()) {
                total += context.getScoreManager().getScore(p.getPlayerId());
            }
        }
        this.totalScore = total;
        this.newRecord = context.getScoreManager().commitHighScore(total);
    }

    @Override
    public void handleInput(GameContext context, List<Intent> intents) {
        for (Intent intent : intents) {
            if (intent.getKind() == Intent.Kind.CONFIRM
                    || intent.getKind() == Intent.Kind.QUIT_TO_MENU) {
                context.backToMenu();
                return;
            }
        }
    }

    @Override
    public void update(GameContext context, long dtMs) {
        World world = context.getWorld();
        // 失败/胜利画面保留爆炸等特效余韵
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
        g.setColor(new Color(4, 5, 10, 190));
        g.fillRect(0, 0, GameConfig.FIELD_W, GameConfig.FIELD_H);

        int cx = GameConfig.FIELD_W / 2;
        String headline;
        Color headColor;
        if (versusWinner >= 0) {
            if (versusWinner == 0) {
                headline = "平 局";
            } else {
                headline = "玩家 " + versusWinner + " 获胜！";
            }
            headColor = new Color(120, 200, 255);
        } else if (victory) {
            headline = "恭 喜 通 关 ！";
            headColor = new Color(120, 235, 150);
        } else {
            headline = "游 戏 失 败";
            headColor = new Color(235, 110, 95);
        }
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 54));
        ReadyState.drawCentered(g, headline, cx, 300, headColor,
                new Color(30, 20, 12));

        int y = 372;
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
        if (world != null) {
            for (PlayerTank p : world.getPlayers()) {
                ReadyState.drawCentered(g,
                        "玩家" + p.getPlayerId() + " 得分："
                                + context.getScoreManager().getScore(p.getPlayerId()),
                        cx, y, new Color(232, 238, 248), null);
                y += 32;
            }
        }
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 18));
        ReadyState.drawCentered(g, "本局总分 " + totalScore + "    历史最高 "
                + context.getScoreManager().getHighScore(), cx, y + 12,
                new Color(220, 224, 236), null);
        if (newRecord) {
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
            ReadyState.drawCentered(g, "★ 新纪录 ★", cx, y + 48,
                    new Color(255, 214, 90), null);
        }
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 20));
        ReadyState.drawCentered(g, "按 Enter 返回主菜单", cx,
                y + 92, new Color(190, 198, 214), null);
    }

    @Override
    public void onExit(GameContext context) {
        // 无需清理
    }
}
