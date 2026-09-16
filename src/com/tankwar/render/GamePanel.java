package com.tankwar.render;

import com.tankwar.constant.GameConfig;
import com.tankwar.core.GameContext;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * 游戏面板：离屏缓冲双缓冲渲染容器。
 * <p>游戏主循环在离屏 {@link BufferedImage} 上完整绘制一帧后再一次性
 * blit 到屏幕，杜绝闪烁；{@link #paintComponent} 只做图像拷贝，
 * 不在 EDT 上构建/分配任何游戏对象。</p>
 *
 * @author TankWar Team
 */
public class GamePanel extends JPanel {

    private static final long serialVersionUID = 1L;

    /** 绘制锁：保证离屏图在 blit 期间不会被游戏线程改写。 */
    private final Object bufferLock = new Object();
    /** 离屏缓冲。 */
    private final BufferedImage buffer;

    /**
     * 构造面板并创建离屏缓冲。
     */
    public GamePanel() {
        this.buffer = new BufferedImage(GameConfig.WINDOW_W,
                GameConfig.WINDOW_H, BufferedImage.TYPE_INT_RGB);
        setPreferredSize(new java.awt.Dimension(
                GameConfig.WINDOW_W, GameConfig.WINDOW_H));
        setFocusable(true);
        setDoubleBuffered(false);
        setBackground(Color.BLACK);
    }

    /**
     * 在离屏缓冲上渲染当前状态的完整一帧，随后请求重绘。
     *
     * @param context 游戏上下文
     */
    public void renderFrame(GameContext context) {
        synchronized (bufferLock) {
            Graphics2D g = buffer.createGraphics();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_OFF);
                g.setColor(Color.BLACK);
                g.fillRect(0, 0, GameConfig.WINDOW_W, GameConfig.WINDOW_H);
                context.getState().render(context, g);
            } finally {
                g.dispose();
            }
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        synchronized (bufferLock) {
            g.drawImage(buffer, 0, 0, null);
        }
    }
}
