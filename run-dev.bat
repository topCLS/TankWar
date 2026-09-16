@echo off
chcp 65001 >nul
title 坦克大战 Tank War (dev)
cd /d "%~dp0"
echo 以开发模式启动（classes + resources）...
java -cp "classes;resources" com.tankwar.Main
if errorlevel 1 pause
