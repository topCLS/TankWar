package com.tankwar.util;

import com.tankwar.constant.GameConfig;

import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 精灵图缓存（单例）。
 * <p>游戏启动时一次性把全部 PNG 解码到内存，运行期直接取用，禁止在
 * 渲染方法中做任何图片 IO。资源缺失时 {@code get} 返回 null，由实体
 * 回退到程序化矢量绘制，保证无美术资源也能运行。</p>
 *
 * @author TankWar Team
 */
public final class SpriteCache {

    private static final SpriteCache INSTANCE = new SpriteCache();

    /** 图片缓存。 */
    private final Map<String, BufferedImage> cache =
            new ConcurrentHashMap<String, BufferedImage>();
    /** 是否已预加载。 */
    private volatile boolean loaded = false;

    private SpriteCache() {
    }

    /** @return 单例。 */
    public static SpriteCache getInstance() {
        return INSTANCE;
    }

    /** 预加载全部游戏图片（启动时调用一次）。 */
    public void preload() {
        if (loaded) {
            return;
        }
        String[] images = {
                "tank_player1.png", "tank_player2.png",
                "enemy_basic.png", "enemy_fast.png",
                "enemy_armored.png", "enemy_bonus.png",
                "power_speed.png", "power_star.png", "power_shield.png",
                "base_eagle.png", "title_bg.png",
                "explosion_0.png", "explosion_1.png",
                "explosion_2.png", "explosion_3.png"
        };
        for (String name : images) {
            BufferedImage img = ResourceLoader.loadImage(GameConfig.IMAGE_DIR + name);
            if (img != null) {
                cache.put(name, img);
            }
        }
        loaded = true;
        System.out.println("[SpriteCache] 已加载 " + cache.size() + " 张精灵图");
    }

    /**
     * 获取精灵图。
     *
     * @param name 文件名
     * @return 图片；缺失返回 null
     */
    public BufferedImage get(String name) {
        return cache.get(name);
    }

    /**
     * 获取爆炸动画第 frame 帧。
     *
     * @param frame 帧序号（0~3）
     * @return 图片；缺失返回 null
     */
    public BufferedImage getExplosion(int frame) {
        return cache.get("explosion_" + frame + ".png");
    }
}
