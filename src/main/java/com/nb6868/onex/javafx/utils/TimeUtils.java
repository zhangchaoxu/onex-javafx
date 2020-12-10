package com.mellonrobot.faceunlockfx.utils;

import java.util.Date;
import java.text.SimpleDateFormat;

public class TimeUtils {

    /**
     * 时间格式(yyyy-MM-dd)
     */
    public final static String DATE_PATTERN = "yyyy-MM-dd";

    /**
     * 时间格式(yyyy-MM-dd HH:mm:ss)
     */
    public final static String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /**
     * 时间格式(yyyy-MM-dd HH:mm:ss)
     */
    public final static String DATE_TIME_FULL_PATTERN = "yyyy-MM-dd HH:mm:ss:SSS";

    /**
     * 时间格式
     */
    public final static String DATE_SHORT_PATTERN = "yyyyMMdd";

    /**
     * 日期格式化 日期格式为：yyyy-MM-dd
     *
     * @param date 日期
     * @return 返回yyyy-MM-dd格式日期
     */
    public static String format(Date date) {
        return format(date, DATE_PATTERN);
    }

    /**
     * 日期格式化 日期格式为：yyyy-MM-dd
     *
     * @param date    日期
     * @param pattern 格式，如：DateUtils.DATE_TIME_PATTERN
     * @return 返回yyyy-MM-dd格式日期
     */
    public static String format(Date date, String pattern) {
        if (date != null) {
            SimpleDateFormat df = new SimpleDateFormat(pattern);
            return df.format(date);
        }
        return null;
    }

    /**
     * 日期格式化 日期格式为：yyyy-MM-dd
     *
     * @param millions    日期
     * @param pattern 格式，如：DateUtils.DATE_TIME_PATTERN
     * @return 返回yyyy-MM-dd格式日期
     */
    public static String format(long millions, String pattern) {
        SimpleDateFormat df = new SimpleDateFormat(pattern);
        return df.format(new Date(millions));
    }

    public static String todaySimple() {
        return format(new Date(), DATE_SHORT_PATTERN);
    }

    public static String now() {
        return format(new Date(), DATE_TIME_PATTERN);
    }

    public static String nowFull() {
        return format(new Date(), DATE_TIME_FULL_PATTERN);
    }

}
