package com.tankwar.util;

import com.tankwar.constant.GameConfig;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * 音效播放器（单例）。
 * <p>并发与资源管理要点：</p>
 * <ul>
 *   <li>启动时一次性把 WAV 解码为字节缓存，运行期不再做磁盘 IO；</li>
 *   <li>所有播放动作提交到独立守护线程池，绝不阻塞游戏逻辑线程与 EDT；</li>
 *   <li>同一种音效设置最小触发间隔，避免开火等高频音效叠加爆音；</li>
 *   <li>Clip 播放结束后在监听器中 close，防止音频句柄泄漏；</li>
 *   <li>机器无音频设备或资源缺失时静默降级，游戏照常运行。</li>
 * </ul>
 *
 * @author TankWar Team
 */
public final class AudioPlayer {

    /** 单例。 */
    private static final AudioPlayer INSTANCE = new AudioPlayer();

    /** 音效字节缓存：资源名 -> WAV 字节。 */
    private final Map<String, byte[]> soundCache = new ConcurrentHashMap<String, byte[]>();
    /** 每种音效最近一次播放时间（限流）。 */
    private final Map<String, Long> lastPlayed = new ConcurrentHashMap<String, Long>();
    /** 音效播放线程池。 */
    private final ExecutorService soundPool =
            Executors.newFixedThreadPool(4, new DaemonThreadFactory("audio"));
    /** 音频是否可用。 */
    private volatile boolean available = true;
    /** 是否静音。 */
    private volatile boolean muted = false;
    /** 背景音乐 Clip（独立长连接、循环播放）。 */
    private volatile Clip bgmClip;

    /** 同名音效最小间隔（毫秒）。 */
    private static final long MIN_INTERVAL_MS = 55L;

    private AudioPlayer() {
        try {
            AudioSystem.getMixer(null);
        } catch (Throwable t) {
            available = false;
        }
    }

    /** @return 单例实例。 */
    public static AudioPlayer getInstance() {
        return INSTANCE;
    }

    /**
     * 预加载全部音效（在启动画面阶段调用一次）。
     */
    public void preload() {
        if (!available) {
            return;
        }
        String[] names = {"fire.wav", "brick.wav", "steel.wav",
                "explode_small.wav", "explode_big.wav", "powerup.wav",
                "levelclear.wav", "gameover.wav", "start.wav", "bgm.wav"};
        for (String name : names) {
            byte[] data = ResourceLoader.readBytes(GameConfig.AUDIO_DIR + name);
            if (data != null) {
                soundCache.put(name, data);
            }
        }
    }

    /**
     * 播放一次性音效（带限流）。
     *
     * @param name WAV 文件名
     */
    public void playOnce(final String name) {
        if (!available || muted) {
            return;
        }
        long now = System.currentTimeMillis();
        Long last = lastPlayed.get(name);
        if (last != null && now - last < MIN_INTERVAL_MS) {
            return;
        }
        lastPlayed.put(name, now);
        final byte[] data = soundCache.get(name);
        if (data == null) {
            return;
        }
        soundPool.execute(new Runnable() {
            @Override
            public void run() {
                Clip clip = null;
                try {
                    clip = AudioSystem.getClip();
                    AudioInputStream ais = AudioSystem.getAudioInputStream(
                            new ByteArrayInputStream(data));
                    clip.open(ais);
                    applyVolume(clip, 0.85f);
                    final Clip running = clip;
                    clip.addLineListener(new LineListener() {
                        @Override
                        public void update(LineEvent event) {
                            if (event.getType() == LineEvent.Type.STOP) {
                                running.close();
                            }
                        }
                    });
                    clip.start();
                } catch (LineUnavailableException | UnsupportedAudioFileException
                         | IOException e) {
                    if (clip != null) {
                        clip.close();
                    }
                }
            }
        });
    }

    /**
     * 循环播放背景音乐。
     *
     * @param name WAV 文件名
     */
    public void loopBgm(final String name) {
        if (!available || muted) {
            return;
        }
        stopBgm();
        final byte[] data = soundCache.get(name);
        if (data == null) {
            return;
        }
        soundPool.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Clip clip = AudioSystem.getClip();
                    AudioInputStream ais = AudioSystem.getAudioInputStream(
                            new ByteArrayInputStream(data));
                    clip.open(ais);
                    applyVolume(clip, 0.45f);
                    clip.loop(Clip.LOOP_CONTINUOUSLY);
                    bgmClip = clip;
                } catch (LineUnavailableException | UnsupportedAudioFileException
                         | IOException e) {
                    // 静默降级
                }
            }
        });
    }

    /** 停止背景音乐。 */
    public void stopBgm() {
        Clip clip = bgmClip;
        if (clip != null) {
            try {
                clip.stop();
                clip.close();
            } catch (IllegalStateException e) {
                // 已关闭，忽略
            }
            bgmClip = null;
        }
    }

    /**
     * 设置静音；静音时立即停止 BGM，取消静音后由调用方重新播放。
     *
     * @param muted 是否静音
     */
    public void setMuted(boolean muted) {
        this.muted = muted;
        if (muted) {
            stopBgm();
        }
    }

    /** @return 当前是否静音。 */
    public boolean isMuted() {
        return muted;
    }

    /** 关闭播放器（退出游戏时回收）。 */
    public void shutdown() {
        stopBgm();
        soundPool.shutdown();
        try {
            soundPool.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 设置 Clip 主音量（不支持增益控制时忽略）。
     */
    private void applyVolume(Clip clip, float gain) {
        try {
            FloatControl control = (FloatControl)
                    clip.getControl(FloatControl.Type.MASTER_GAIN);
            float min = control.getMinimum();
            float max = control.getMaximum();
            float value = (float) (min + (max - min) * gain);
            control.setValue(Math.max(min, Math.min(max, value)));
        } catch (IllegalArgumentException e) {
            // 该行不支持音量控制，保持默认
        }
    }

    /** 守护线程工厂：保证音频线程不会阻止 JVM 退出。 */
    private static final class DaemonThreadFactory implements ThreadFactory {
        private final String namePrefix;
        private int index = 0;

        DaemonThreadFactory(String namePrefix) {
            this.namePrefix = namePrefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, namePrefix + "-" + (index++));
            thread.setDaemon(true);
            return thread;
        }
    }
}
