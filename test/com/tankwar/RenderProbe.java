package com.tankwar;

import com.tankwar.constant.Difficulty;
import com.tankwar.constant.GameConfig;
import com.tankwar.constant.GameMode;
import com.tankwar.core.GameContext;
import com.tankwar.core.Hud;
import com.tankwar.core.World;
import com.tankwar.input.InputManager;
import com.tankwar.util.SpriteCache;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * 离屏渲染探针：不创建窗口，直接把 world 与 HUD 画到一张缓冲图上，
 * 逐区域统计像素，判定 HUD 顶部文字是否绘制、战场是否越界进入 HUD。
 */
public final class RenderProbe {

    private RenderProbe() {
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
        GameContext ctx = new GameContext(null, input);
        ctx.setMode(GameMode.SOLO);
        ctx.setDifficulty(Difficulty.NORMAL);
        ctx.getScoreManager().reset();
        ctx.getLevelManager().reset();

        World world = new World(ctx, 1);
        long now = System.currentTimeMillis();
        for (int i = 0; i < 400; i++) {
            world.update(16L, now + i * 16L);
        }

        BufferedImage img = new BufferedImage(GameConfig.WINDOW_W,
                GameConfig.WINDOW_H, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, GameConfig.WINDOW_W, GameConfig.WINDOW_H);
        world.draw(g);
        // 先只画 world，扫描战场是否越界
        System.out.println("FIELD_W=" + GameConfig.FIELD_W + " HUD_W="
                + GameConfig.HUD_W + " WIN_W=" + GameConfig.WINDOW_W
                + " WIN_H=" + GameConfig.WINDOW_H);
        System.out.println("[world-only] x>=832 非黑像素="
                + countNonBlack(img, 833, GameConfig.WINDOW_W - 1, 0,
                        GameConfig.WINDOW_H - 1));
        printOverflow(img);
        // 再画 HUD
        new Hud().draw(g, world, ctx);
        g.dispose();

        // 直接落盘最终合成帧（绕开窗口与 DPI，ground truth）
        try {
            java.io.File out = new java.io.File("docs/probe_composite.png");
            out.getParentFile().mkdirs();
            javax.imageio.ImageIO.write(img, "png", out);
            System.out.println("saved " + out.getAbsolutePath());
        } catch (java.io.IOException e) {
            System.out.println("save failed: " + e);
        }

        System.out.println("[world+hud] 顶部 y0-120 x846-968 亮像素="
                + countBright(img, 846, 968, 0, 120));
        System.out.println("[world+hud] 敌人点阵区 y120-205 亮像素="
                + countBright(img, 846, 968, 120, 205));
        System.out.println("[world+hud] 底部 y740-820 亮像素="
                + countBright(img, 846, 968, 740, 820));
        // 采样若干点颜色
        for (int[] pt : new int[][]{{846, 18}, {860, 58}, {860, 74}, {860, 90},
                {850, 128}, {850, 760}}) {
            Color c = new Color(img.getRGB(pt[0], pt[1]));
            System.out.printf("px(%d,%d)=(%d,%d,%d)%n", pt[0], pt[1],
                    c.getRed(), c.getGreen(), c.getBlue());
        }
    }

    /** 打印战场越入 HUD 区像素的包围盒与典型颜色。 */
    private static void printOverflow(BufferedImage img) {
        int minX = 9999, minY = 9999, maxX = -1, maxY = -1, n = 0;
        java.util.Map<Integer, Integer> colors = new java.util.HashMap<Integer, Integer>();
        for (int y = 0; y < GameConfig.WINDOW_H; y++) {
            for (int x = GameConfig.FIELD_W + 1; x < GameConfig.WINDOW_W; x++) {
                int rgb = img.getRGB(x, y);
                Color c = new Color(rgb);
                if (c.getRed() + c.getGreen() + c.getBlue() <= 24) {
                    continue;
                }
                n++;
                minX = Math.min(minX, x);
                maxX = Math.max(maxX, x);
                minY = Math.min(minY, y);
                maxY = Math.max(maxY, y);
                int key = (c.getRed() / 40 << 16) | (c.getGreen() / 40 << 8)
                        | (c.getBlue() / 40);
                colors.merge(key, 1, Integer::sum);
            }
        }
        System.out.println("越界像素数=" + n + " 包围盒 x[" + minX + "," + maxX
                + "] y[" + minY + "," + maxY + "]");
        colors.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .limit(6)
                .forEach(e -> {
                    int k = e.getKey();
                    System.out.printf("  颜色桶~(%d,%d,%d) x%d%n",
                            (k >> 16) * 40, ((k >> 8) & 255) * 40,
                            (k & 255) * 40, e.getValue());
                });
    }

    private static int countNonBlack(BufferedImage img, int x0, int x1, int y0, int y1) {        int n = 0;
        for (int y = y0; y <= y1; y += 2) {
            for (int x = x0; x <= x1; x += 2) {
                Color c = new Color(img.getRGB(x, y));
                if (c.getRed() + c.getGreen() + c.getBlue() > 24) {
                    n++;
                }
            }
        }
        return n;
    }

    private static int countBright(BufferedImage img, int x0, int x1, int y0, int y1) {
        int n = 0;
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                Color c = new Color(img.getRGB(x, y));
                if (c.getRed() + c.getGreen() + c.getBlue() > 150) {
                    n++;
                }
            }
        }
        return n;
    }
}
