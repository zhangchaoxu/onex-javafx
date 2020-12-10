package com.mellonrobot.faceunlockfx.utils;

import java.math.BigDecimal;

/**
 * 数字处理方法
 *
 * @author Charles
 */
public class NumberUtils {

    public static Integer stringToInt(String value) {
        if (StringUtils.isEmpty(value)) {
            return null;
        } else {
            try {
                return Integer.parseInt(value);
            } catch (Exception e) {
                return null;
            }
        }
    }

    public static Long stringToLong(String value) {
        if (StringUtils.isEmpty(value)) {
            return null;
        } else {
            try {
                return Long.parseLong(value);
            } catch (Exception e) {
                return null;
            }
        }
    }

    public static Double stringToDouble(String value) {
        if (StringUtils.isEmpty(value)) {
            return null;
        } else {
            try {
                return Double.parseDouble(value);
            } catch (Exception e) {
                return null;
            }
        }
    }

    public static int[] stringToIntList(String value, String splitRegex) {
        if (StringUtils.isEmpty(value)) {
            return new int[0];
        } else {
            String[] strings = value.split(splitRegex);
            int[] ints = new int[strings.length];
            try {
                for (int i = 0; i < strings.length; i++) {
                    ints[i] = Integer.parseInt(strings[i]);
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
                return new int[0];
            }
            return ints;
        }
    }

    public static double[] stringToDoubleList(String value, String splitRegex) {
        if (StringUtils.isEmpty(value)) {
            return new double[0];
        } else {
            String[] strings = value.split(splitRegex);
            double[] newDoubles = new double[strings.length];
            try {
                for (int i = 0; i < strings.length; i++) {
                    // 只保留2位
                    newDoubles[i] = new BigDecimal(Double.parseDouble(strings[i])).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
                return new double[0];
            }
            return newDoubles;
        }
    }

    public static double[] stringToDoubleListNoScale(String value, String splitRegex) {
        if (StringUtils.isEmpty(value)) {
            return new double[0];
        } else {
            String[] strings = value.split(splitRegex);
            double[] newDoubles = new double[strings.length];
            try {
                for (int i = 0; i < strings.length; i++) {
                    newDoubles[i] = Double.parseDouble(strings[i]);
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
                return new double[0];
            }
            return newDoubles;
        }
    }

    /**
     * 转换以后再乘以一个因子
     */
    public static double[] stringToDoubleList(String value, String splitRegex, double factor) {
        if (StringUtils.isEmpty(value)) {
            return new double[0];
        } else {
            String[] strings = value.split(splitRegex);
            double[] newDouble = new double[strings.length];
            try {
                for (int i = 0; i < strings.length; i++) {
                    // 只保留1位
                    newDouble[i] = new BigDecimal(Double.parseDouble(strings[i]) * factor).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
                return new double[0];
            }
            return newDouble;
        }
    }

    public static String listToString(String[] strings, String splitRegex) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < strings.length; i++) {
            if (i == 0) {
                result = new StringBuilder(strings[i]);
            } else {
                result.append(splitRegex).append(strings[i]);
            }
        }
        return result.toString();
    }

    public static String listToString(double[] doubles, String splitRegex) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < doubles.length; i++) {
            if (i == 0) {
                result = new StringBuilder(String.valueOf(doubles[i]));
            } else {
                result.append(splitRegex).append(doubles[i]);
            }
        }
        return result.toString();
    }

    public static String listToString(int[] ints, String splitRegex) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < ints.length; i++) {
            if (i == 0) {
                result = new StringBuilder(String.valueOf(ints[i]));
            } else {
                result.append(splitRegex).append(ints[i]);
            }
        }
        return result.toString();
    }

    public static double[] listMultFactor(double[] doubles, double factor) {
        double[] newDoubles = new double[doubles.length];
        for (int i = 0; i < doubles.length; i++) {
            newDoubles[i] = doubles[i] * factor;
        }
        return newDoubles;
    }

    public static double[] paramAddAngle(double[] doubles, int position1, double position1Value, int position2, double position2Value, int position3, double position3Value) {
        if (position1 > doubles.length || position2 > doubles.length || position3 > doubles.length) {
            return new double[0];
        } else {
            double[] newDoubles = new double[doubles.length];
            for (int i = 0; i < doubles.length; i++) {
                if (i + 1 == position1) {
                    newDoubles[i] = doubles[i] + position1Value;
                } else if (i + 1 == position2) {
                    newDoubles[i] = doubles[i] + position2Value;
                } else if (i + 1 == position3) {
                    newDoubles[i] = doubles[i] + position3Value;
                } else {
                    newDoubles[i] = doubles[i];
                }
            }
            return newDoubles;
        }
    }
}
