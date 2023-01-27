package com.aminekili.aitrading.utils;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class LoggingUtils {

    public static void print(String message) {
        System.out.println(MessageFormat.format("[{0}] {1}", getCurrentTimeForLogging(), message));
    }

    public static void format(String message, Object... args) {
        System.out.println(MessageFormat.format("[{0}] {1}", getCurrentTimeForLogging(), MessageFormat.format(message, args)));
    }

    public static String getCurrentTimeForLogging() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
    }

}
