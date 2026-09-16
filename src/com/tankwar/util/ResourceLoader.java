package com.tankwar.util;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 资源加载器：统一从 classpath 读取图片 / 文本 / 音频。
 * <p>使用 {@code getResourceAsStream} 而非文件相对路径，保证打包成 JAR 后
 * 资源仍可加载；所有方法在资源缺失时返回 null/空集合或降级处理，绝不
 * 抛出未捕获异常导致游戏崩溃。</p>
 *
 * @author TankWar Team
 */
public final class ResourceLoader {

    private ResourceLoader() {
    }

    /**
     * 加载图片。
     *
     * @param resource classpath 资源名（如 "/images/tank_player1.png"）
     * @return 图片；不存在或解码失败返回 null
     */
    public static BufferedImage loadImage(String resource) {
        try (InputStream in = ResourceLoader.class.getResourceAsStream(resource)) {
            if (in == null) {
                return null;
            }
            return ImageIO.read(in);
        } catch (IOException | IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * 读取文本文件全部行（UTF-8）。
     *
     * @param resource classpath 资源名
     * @return 行列表；失败返回 null（调用方负责回退内置关卡）
     */
    public static List<String> readLines(String resource) {
        try (InputStream in = ResourceLoader.class.getResourceAsStream(resource)) {
            if (in == null) {
                return null;
            }
            List<String> lines = new ArrayList<String>();
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
            return lines;
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * 读取二进制资源全部字节（音频等）。
     *
     * @param resource classpath 资源名
     * @return 字节数组；失败返回 null
     */
    public static byte[] readBytes(String resource) {
        try (InputStream in = ResourceLoader.class.getResourceAsStream(resource);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            if (in == null) {
                return null;
            }
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * 获取资源 URL（诊断用）。
     *
     * @param resource 资源名
     * @return URL；不存在返回 null
     */
    public static URL url(String resource) {
        return ResourceLoader.class.getResource(resource);
    }
}
