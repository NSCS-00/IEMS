@echo off
set JAVA_HOME=D:\Java\jdk-21
set PATH=%JAVA_HOME%\bin;%PATH%
D:\GRADLE\gradle-8.14.3-bin\bin\gradle.bat build --no-daemon
