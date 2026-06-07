package com.example.fish;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import android.os.Environment;
import java.io.File;

public class LogSaveUtil {
    // 日志文件 → 系统公共 Pictures 文件夹，所有人可见
    private static File logFile;

    // 延迟初始化文件路径
    private static void initLogFile() {
        if (logFile == null) {
            // 获取系统公共 图片目录
            File picDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            // 目录不存在则创建
            if (!picDir.exists()) {
                picDir.mkdirs();
            }
            logFile = new File(picDir, "run_log.txt");
        }
    }

    //普通运行日志
    public static void saveLog(String content) {
        initLogFile();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String time = sdf.format(new Date());
        String logStr = time + " | " + content + "\r\n";
        try (FileWriter fw = new FileWriter(logFile, true);
             BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write(logStr);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 带代码行号日志
    public static void saveLogWithLine(String content) {
        initLogFile();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String time = sdf.format(new Date());
        StackTraceElement stack = new Throwable().getStackTrace()[1];
        String lineInfo = stack.getClassName() + "." + stack.getMethodName() + " 行:" + stack.getLineNumber();
        String logStr = time + " [" + lineInfo + "] | " + content + "\r\n";

        try (FileWriter fw = new FileWriter(logFile, true);
             BufferedWriter bw = new BufferedWriter(fw)) {
            bw.write(logStr);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //异常崩溃全堆栈写入
    public static void saveException(Exception e) {
        initLogFile();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String time = sdf.format(new Date());
        try (FileWriter fw = new FileWriter(logFile, true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter pw = new PrintWriter(bw)) {
            bw.write("\r\n======异常 " + time + " ======\r\n");
            e.printStackTrace(pw);
            bw.write("\r\n=========================\r\n");
        } catch (Exception err) {
            err.printStackTrace();
        }
    }
}
