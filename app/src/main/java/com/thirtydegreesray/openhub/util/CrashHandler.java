package com.thirtydegreesray.openhub.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Build;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

public class CrashHandler implements Thread.UncaughtExceptionHandler {

    public static final String CRASH_FILE = "last_crash.txt";

    private final Context context;
    private final Thread.UncaughtExceptionHandler defaultHandler;

    private CrashHandler(Context context, Thread.UncaughtExceptionHandler defaultHandler) {
        this.context = context;
        this.defaultHandler = defaultHandler;
    }

    public static void install(Context context) {
        Thread.UncaughtExceptionHandler current = Thread.getDefaultUncaughtExceptionHandler();
        if (!(current instanceof CrashHandler)) {
            Thread.setDefaultUncaughtExceptionHandler(
                    new CrashHandler(context.getApplicationContext(), current));
        }
    }

    public static File getCrashFile(Context context) {
        return new File(context.getFilesDir(), CRASH_FILE);
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        try {
            StringWriter stringWriter = new StringWriter();
            PrintWriter writer = new PrintWriter(stringWriter);
            writer.println("Thread: " + thread.getName());
            writer.println("Android: " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")");
            writer.println("Device: " + Build.MANUFACTURER + " " + Build.MODEL);
            try {
                PackageInfo info = context.getPackageManager()
                        .getPackageInfo(context.getPackageName(), 0);
                writer.println("App: " + info.versionName + " (" + info.versionCode + ")");
            } catch (Exception ignored) {
            }
            writer.println();
            throwable.printStackTrace(writer);
            writer.flush();

            File file = getCrashFile(context);
            FileOutputStream outputStream = new FileOutputStream(file, false);
            outputStream.write(stringWriter.toString().getBytes("UTF-8"));
            outputStream.close();
        } catch (Throwable ignored) {
        }

        if (defaultHandler != null) {
            defaultHandler.uncaughtException(thread, throwable);
        }
    }
}
