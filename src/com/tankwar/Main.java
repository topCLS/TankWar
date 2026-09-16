package com.tankwar;

import com.tankwar.constant.GameConfig;
import com.tankwar.core.GameContext;
import com.tankwar.core.GameLoop;
import com.tankwar.input.InputManager;
import com.tankwar.render.GamePanel;
import com.tankwar.util.AudioPlayer;
import com.tankwar.util.SpriteCache;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * 坦克大战程序入口。
 * <p>在 EDT 上完成窗口与组件装配、美术与音频资源预加载，随后启动
 * 固定步长游戏主循环与上下文调度线程；窗口关闭时优雅释放音频与线程。</p>
 *
 * @author TankWar Team
 */
public final class Main {

    private Main() {
    }

    /**
     * 应用入口。
     *
     * @param args 启动参数（未使用）
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                bootstrap();
            }
        });
    }

    /** 在 EDT 上构建并启动游戏。 */
    private static void bootstrap() {
        // 资源预加载（图片缺失也不影响运行，实体有矢量兜底）
        SpriteCache.getInstance().preload();
        AudioPlayer.getInstance().preload();

        GamePanel panel = new GamePanel();
        InputManager input = new InputManager();
        input.attach(panel);

        final GameContext context = new GameContext(panel, input);

        JFrame frame = new JFrame(GameConfig.WINDOW_TITLE);
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.setResizable(false);
        frame.add(panel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        panel.requestFocusInWindow();

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                context.shutdown();
                frame.dispose();
                System.exit(0);
            }
        });

        // 进入主菜单并启动主循环
        context.start();
        new GameLoop(context, input, panel).start();
    }
}
