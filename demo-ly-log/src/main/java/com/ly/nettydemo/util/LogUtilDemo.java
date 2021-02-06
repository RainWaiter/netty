package com.ly.nettydemo.util;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 *
 */
public class LogUtilDemo {

    public static void log(Object content) {
        System.out.println(new SimpleDateFormat("MM-dd HH:mm:ss").format(new Date()) + " " +
                           "[" + Thread.currentThread().getName() + "] " +
                           content);
    }
    public static void log(Object content, Object that) {
        System.out.println(
                new SimpleDateFormat("MM-dd HH:mm:ss").format(new Date()) + " " +
                "[" + Thread.currentThread().getName() + "] " +
               // that + " " +
                "[" + that.getClass().getSimpleName() + "] " +
                content);
    }
}
