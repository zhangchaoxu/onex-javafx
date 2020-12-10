package com.mellonrobot.faceunlockfx.utils;

import com.mellonrobot.faceunlockfx.serial.EngineStatus;

/**
 * 串口根工具
 */
public class SerialPortUtils {


    public static final String REQUEST_FRAME_HEADER = "AA";
    public static final String REQUEST_FRAME_END = "BB";
    public static final String RESPONSE_FRAME_HEADER = "CC";
    public static final String RESPONSE_FRAME_END = "DD";

    /**
     * N20电机通讯协议
     * 控制命令：
     * PC->ARM
     * <p>
     * 帧头	功能码	电机编号	运行命令	运行脉冲数	预留	和校验	帧尾
     * 0xAA	0x01	1byte	1byte	2byte	10byte	1byte	0xBB
     *
     * @param engineNo 电机编号 1~7；
     * @param cmdType  运行命令 0=停止，1=正转，2=反转，3=制动；
     * @param pulse    运行脉冲数 1~65535，高位在前；
     */
    public static String controlN20Cmd(int engineNo, int cmdType, int pulse) {
        String pre = REQUEST_FRAME_HEADER + "01" + intToByte(engineNo, 2) + intToByte(cmdType, 2) + intToByte(pulse, 4) + "00000000000000000000";
        return pre + checkSum(pre) + REQUEST_FRAME_END;
    }

    /**
     * N20电机通讯协议
     * 读状态指令：
     * PC->ARM
     * <p>
     * 帧头	功能码	电机编号	预留	和校验	帧尾
     * 0xAA	0x02	1byte	13byte	1byte	0xBB
     *
     * @param engineNo 电机编号 1~7；
     */
    public static String statueN20Cmd(int engineNo) {
        String pre = REQUEST_FRAME_HEADER + "02" + intToByte(engineNo, 2) + "00000000000000000000000000";
        return pre + checkSum(pre) + REQUEST_FRAME_END;
    }


    /**
     * N20电机通讯协议
     * 绝对位置命令：
     * PC->ARM
     * <p>
     * 帧头	功能码	电机编号	运行命令	运行脉冲数	预留	和校验	帧尾
     * 0xAA	0x03	1byte	0X01	0XFFFF	10byte	1byte	0xBB
     *
     * @param engineNo 电机编号 1~7；
     */
    public static String reset20Cmd(int engineNo) {
        String pre = REQUEST_FRAME_HEADER + "03" + intToByte(engineNo, 2) + "01FFFF00000000000000000000";
        return pre + checkSum(pre) + REQUEST_FRAME_END;
    }

    /**
     * 24电机通讯命令
     * 位置速度调整命令
     * PC->ARM
     * <p>
     * 帧头	功能码	电机编号	模式	速度	位置	预留	和校验	帧尾
     * 0xAA	0x11	1byte	1byte	4byte	4byte	4byte	1byte	0xBB
     *
     * @param engineNo 电机编号 1~7；
     * @param mode     00=电机从当前位置转动一个位置的度数，此时速度值只取正值；
     *                 01=电机转动到位置值这个位置点，此时速度值只取整；
     *                 01=电机已该速度一直转动，此时速度值可正可负，位置值忽略
     * @param speed    实际速度*16384/6000，取整数4字节，表示为有符号整数
     * @param location 实际度数*16384/360， 取整数 4 字节， 表示为有符号整数
     */
    public static String control24Cmd(int engineNo, int mode, int speed, int location) {
        String pre = REQUEST_FRAME_HEADER + "11"
                + intToByte(engineNo, 2)
                + intToByte(mode, 2)
                + intToByte(speed * 16384 / 6000, 8)
                + intToByte(location * 16384 / 360, 8) + "00000000";
        return pre + checkSum(pre) + REQUEST_FRAME_END;
    }

    /**
     * 24电机通讯命令
     * 清零相对位置命令
     * PC->ARM
     * <p>
     * 帧头	功能码	电机编号	预留	和校验	帧尾
     * 0xAA	0x12	1byte	13byte	1byte	0xBB
     *
     * @param engineNo 电机编号 1~7；
     */
    public static String reset24Cmd(int engineNo) {
        String pre = REQUEST_FRAME_HEADER + "12" + intToByte(engineNo, 2) + "00000000000000000000000000";
        return pre + checkSum(pre) + REQUEST_FRAME_END;
    }

    /**
     * 读取电机状态：
     * PC->ARM
     * <p>
     * 帧头	功能码	电机编号	预留	和校验	帧尾
     * 0xAA	0x13	1byte	13byte	1byte	0xBB
     *
     * @param engineNo 电机编号 1~7；
     */
    public static String statue24Cmd(int engineNo) {
        String pre = REQUEST_FRAME_HEADER + "13" + intToByte(engineNo, 2) + "00000000000000000000000000";
        return pre + checkSum(pre) + REQUEST_FRAME_END;
    }

    /**
     * 读取电机状态：
     * PC->ARM
     * <p>
     * 帧头	功能码	电机编号	预留	预留	预留	预留	和校验	帧尾
     * 0xAA	0x15	1byte	1byte	4byte	4byte	4byte	1byte	0xBB
     *
     * @param engineNo 电机编号 1~7；
     */
    public static String stop24Cmd(int engineNo) {
        String pre = REQUEST_FRAME_HEADER + "15" + intToByte(engineNo, 2) + "00000000000000000000000000";
        return pre + checkSum(pre) + REQUEST_FRAME_END;
    }

    /**
     * N20电机通讯协议
     * 往返命令：
     * PC->ARM
     * <p>
     * 帧头	功能码	电机编号	预留	和校验	帧尾
     * 0xAA	0x13	1byte	13byte	1byte	0xBB
     *
     * @param engineNo     电机编号
     * @param returnTimes  往返次数
     * @param speed        速度取正值 单位rpm
     * @param location     速度取正值
     * @param intervalTime 间隔时间 为0，表示只运行往返次数就停
     */
    public static String return24Cmd(int engineNo, int returnTimes, int speed, int location, int intervalTime) {
        String pre = REQUEST_FRAME_HEADER + "14"
                + intToByte(engineNo, 2)
                + intToByte(returnTimes, 2)
                + intToByte(speed * 16384 / 6000, 8)
                + intToByte(location * 16384 / 360, 8)
                + intToByte(intervalTime, 4)
                + "0000";
        return pre + checkSum(pre) + REQUEST_FRAME_END;
    }

    public static EngineStatus parseResponseCmd(String cmd) {
        if (StringUtils.isEmpty(cmd)) {
            return new EngineStatus("内容为空");
        }
        cmd = cmd.toUpperCase();
        if (!cmd.startsWith(RESPONSE_FRAME_HEADER)) {
            return new EngineStatus("非指定帧头:" + RESPONSE_FRAME_HEADER);
        }
        if (!cmd.endsWith(RESPONSE_FRAME_END)) {
            return new EngineStatus("非指定帧尾:" + RESPONSE_FRAME_END);
        }
        /**
         * 功能码
         * 01 N20控制返回
         * 02 N20状态返回
         * 03 N20绝对位置返回
         * 11 24位置速度调整
         * 12 24清零相对位置
         * 13 24读取电机状态
         * 14 24往返指令
         * 15 24停止指令
         */
        if (cmd.startsWith(RESPONSE_FRAME_HEADER + "01")) {
            // N20控制响应
            if (cmd.length() != 24) {
                return new EngineStatus("N20控制响应长度非24");
            }
            EngineStatus engineStatus = new EngineStatus("01", hexToDecimal(cmd.substring(4, 6)));
            engineStatus.setRunStatus(hexToDecimal(cmd.substring(6, 8)));
            engineStatus.setRunPulse(hexToDecimal(cmd.substring(8, 12)));
            return engineStatus;
        } else if (cmd.startsWith(RESPONSE_FRAME_HEADER + "02")) {
            // N20状态响应
            if (cmd.length() != 24) {
                return new EngineStatus("指令[" + cmd + "],N20状态响应长度非24");
            }
            EngineStatus engineStatus = new EngineStatus("02", hexToDecimal(cmd.substring(4, 6)));
            engineStatus.setRunStatus(hexToDecimal(cmd.substring(6, 8)));
            engineStatus.setRunPulse(hexToDecimal(cmd.substring(8, 12)));
            return engineStatus;
        } else if (cmd.startsWith(RESPONSE_FRAME_HEADER + "03")) {
            // N20绝对位置返回
            if (cmd.length() != 24) {
                return new EngineStatus("指令[" + cmd + "],N20绝对位置返回长度非24");
            }
            EngineStatus engineStatus = new EngineStatus("03", hexToDecimal(cmd.substring(4, 6)));
            engineStatus.setRunStatus(hexToDecimal(cmd.substring(6, 8)));
            return engineStatus;
        } else if (cmd.startsWith(RESPONSE_FRAME_HEADER + "11")) {
            // 24位置速度调整
            if (cmd.length() != 38) {
                return new EngineStatus("指令[" + cmd + "],24位置速度调整长度非38");
            }
            EngineStatus engineStatus = new EngineStatus("11", hexToDecimal(cmd.substring(4, 6)));
            engineStatus.setRunStatus(hexToDecimal(cmd.substring(8, 10)));
            engineStatus.setSpeed(hexToDecimal(cmd.substring(10, 18)));
            engineStatus.setLocation(hexToDecimal(cmd.substring(18, 26)));
            return engineStatus;
        } else if (cmd.startsWith(RESPONSE_FRAME_HEADER + "12")) {
            // 24清零相对位置
            if (cmd.length() != 18) {
                return new EngineStatus("指令[" + cmd + "],24清零相对位置长度非18");
            }
            return new EngineStatus("12", hexToDecimal(cmd.substring(4, 6)));
        } else if (cmd.startsWith(RESPONSE_FRAME_HEADER + "13")) {
            // 24读取电机状态
            if (cmd.length() != 36) {
                return new EngineStatus("指令[" + cmd + "],24读取电机状态长度非36");
            }
            EngineStatus engineStatus = new EngineStatus("13", hexToDecimal(cmd.substring(4, 6)));
            engineStatus.setRunStatus(hexToDecimal(cmd.substring(6, 8)));
            engineStatus.setLocationOne(hexToDecimal(cmd.substring(8, 16)));
            engineStatus.setLocation(hexToDecimal(cmd.substring(16, 24)));
            return engineStatus;
        } else if (cmd.startsWith(RESPONSE_FRAME_HEADER + "14")) {
            // 24往返指令
            if (cmd.length() != 36) {
                return new EngineStatus("指令[" + cmd + "],24往返指令长度非36");
            }
            EngineStatus engineStatus = new EngineStatus("14", hexToDecimal(cmd.substring(4, 6)));
            engineStatus.setReturnTimes(hexToDecimal(cmd.substring(6, 8)));
            engineStatus.setSpeed(hexToDecimal(cmd.substring(8, 16)));
            engineStatus.setLocation(hexToDecimal(cmd.substring(16, 24)));
            engineStatus.setIntervalTime(hexToDecimal(cmd.substring(24, 28)));
            return engineStatus;
        } else if (cmd.startsWith(RESPONSE_FRAME_HEADER + "15")) {
            // 24停止指令
            if (cmd.length() != 36) {
                return new EngineStatus("指令[" + cmd + "],24停止指令长度非36");
            }
            EngineStatus engineStatus = new EngineStatus("15", hexToDecimal(cmd.substring(4, 6)));
            return engineStatus;
        } else {
            return new EngineStatus("指令[" + cmd + "]功能码不在范围内");
        }
    }

    /**
     * 十进制转十六进制
     */
    public static String intToByte(int value, int size) {
        return StringUtils.leftPad(Integer.toHexString(value).toUpperCase(), size, '0');
    }

    /**
     * 十六进制转十进制
     *
     * @param hex
     * @return
     */
    public static int hexToDecimal(String hex) {
        int decimalValue = 0;
        for (int i = 0; i < hex.length(); i++) {
            char hexChar = hex.charAt(i);
            decimalValue = decimalValue * 16 + hexCharToDecimal(hexChar);
        }
        return decimalValue;
    }

    public static int hexCharToDecimal(char hexChar) {
        if (hexChar >= 'A' && hexChar <= 'F')
            return 10 + hexChar - 'A';
        else
            // 切记不能写成int类型的0，因为字符'0'转换为int时值为48
            return hexChar - '0';
    }

    public static void main(String[] args) {
        System.out.println(intToByte(65534, 4));
        System.out.println(controlN20Cmd(1, 2, 171));
        System.out.println(statue24Cmd(1));
        System.out.println(reset24Cmd(1));
        System.out.println(statue24Cmd(1));
        System.out.println(return24Cmd(1, 1, 40, 120, 0));
    }

    /**
     * 校验和算法
     * 校验位前所有数据的和的低8位
     *
     * @param data
     * @return
     */
    public static String checkSum(String data) {
        if (StringUtils.isEmpty(data)) {
            return "";
        }
        int total = 0;
        int len = data.length();
        int num = 0;
        while (num < len) {
            String s = data.substring(num, num + 2);
            total += Integer.parseInt(s, 16);
            num = num + 2;
        }
        /**
         * 用256求余最大是255，即16进制的FF
         */
        int mod = total % 256;
        String hex = Integer.toHexString(mod);
        len = hex.length();
        // 如果不够校验位的长度，补0,这里用的是两位校验
        if (len < 2) {
            hex = "0" + hex;
        }
        return hex.toUpperCase();
    }

}
