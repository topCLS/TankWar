@echo off
chcp 65001 >nul
title 坦克大战 Tank War
cd /d "%~dp0"

echo ============================================
echo            坦克大战 Tank War
echo      面向对象程序设计课程设计 2026
echo ============================================
echo.

where java >nul 2>nul
if errorlevel 1 (
    echo [错误] 未检测到 Java 运行环境。
    echo 请先安装 JDK 17 或更高版本，并将 java 加入 PATH。
    echo 下载地址: https://adoptium.net/
    echo.
    pause
    exit /b 1
)

echo 正在启动游戏，请稍候...
java -jar TankWar.jar
if errorlevel 1 (
    echo.
    echo [错误] 游戏异常退出，错误码 %errorlevel%。
    echo 可将同目录下生成的 crash.log 反馈给开发者。
    pause
)
