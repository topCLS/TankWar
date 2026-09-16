package com.tankwar.util;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 轻量崩溃日志：把游戏循环中逻辑或渲染线程捕获到的异常堆栈追加写入
 * 工作目录下的 crash.log，便于在无法查看控制台时定位问题。
 *
 * @author TankWar Team
 */
public final class CrashLog {

    private static final String PATH = "crash.log";
    private static final SimpleDateFormat FMT =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    private CrashLog() {
    }

    /**
     * 记录一次异常。
     *
     * @param phase 发生阶段（如 render / logic step）
     * @param t     异常
     */
    public static synchronized void log(String phase, Throwable t) {
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(new FileWriter(PATH, true));
            pw.println("[" + FMT.format(new Date()) + "] " + phase + " -> "
                    + t.getClass().getName() + ": " + t.getMessage());
            t.printStackTrace(pw);
            pw.println();
            pw.flush();
        } catch (Exception ignored) {
            // 日志本身失败不再抛出
        } finally {
            if (pw != null) {
                pw.close();
            }
        }
    }
}
