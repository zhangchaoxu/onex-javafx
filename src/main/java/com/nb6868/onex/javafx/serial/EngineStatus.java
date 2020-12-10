package com.mellonrobot.faceunlockfx.serial;

import java.io.Serializable;

public class EngineStatus implements Serializable {

    private String msg = "ok";

    public EngineStatus(String msg) {
        this.msg = msg;
    }

    public EngineStatus(String funCode, int engineNo) {
        this.funCode = funCode;
        this.engineNo = engineNo;
    }

    @Override
    public String toString() {
        if (!"ok".equalsIgnoreCase(msg)) {
            return msg;
        } else {
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
            if ("01".equalsIgnoreCase(funCode)) {
                return "N20控制响应=>[" + engineNo + "]号电机=>状态[" + (runStatus == 0 ? "完成命令" : (runStatus == 1 ? "运行中" : "")) + "]/脉冲数[" + runPulse + "]";
            } else if ("02".equalsIgnoreCase(funCode)) {
                return "N20状态返回=>[" + engineNo + "]号电机=>状态[" + (runStatus == 0 ? "完成命令" : (runStatus == 1 ? "运行中" : "")) + "]/脉冲数[" + runPulse + "]";
            } else if ("03".equalsIgnoreCase(funCode)) {
                return "N20绝对位置返回=>[" + engineNo + "]号电机=>状态[" + (runStatus == 0 ? "未到达" : (runStatus == 1 ? "已到达" : "")) + "]";
            } else if ("11".equalsIgnoreCase(funCode)) {
                return "24位置速度调整返回=>[" + engineNo + "]号电机=>状态[" + runStatus + "]/速度[" + getSpeedRpm() + "]/位置[" + location + ']';
            } else if ("12".equalsIgnoreCase(funCode)) {
                return "24清零相对位置返回=>[" + engineNo + "]号电机";
            } else if ("13".equalsIgnoreCase(funCode)) {
                return "24读取电机状态返回=>[" + engineNo + "]号电机=>状态["
                        + (runStatus == 0 ? "停止" : (runStatus == 1 ? "正转" : (runStatus == 2 ? "反转" : "")))
                        + "]/一圈绝对位置[" + getLocationOneRpm() + "]"
                        + "/位置[" + getLocationRpm() + "]";
            } else if ("14".equalsIgnoreCase(funCode)) {
                return "24往返指令返回=>[" + engineNo + "]号电机=>"
                        + "往返次数[" + returnTimes + "]"
                        + "/速度[" + getSpeedRpm() + "]"
                        + "/位置[" + getLocationRpm() + "]"
                        + "/间隔时间[" + intervalTime + "]";
            } else if ("15".equalsIgnoreCase(funCode)) {
                return "24停止指令返回=>[" + engineNo + "]号电机";
            } else {
                return "未定义的功能码[" + funCode + "]";
            }
        }
    }

    /**
     * 功能码
     * 01 N20控制返回
     * 02 N20状态返回
     * 11 24位置速度调整
     * 12 24清零相对位置
     * 13 24读取电机状态
     * 14 24往返指令
     */
    private String funCode;

    /**
     * 电机编号；
     */
    private int engineNo;

    /**
     * 运行状态：0=完成命令，1=运行中；
     */
    private int runStatus;

    /**
     * 运行脉冲数；
     */
    private int runPulse;

    /**
     * 往返次数；
     */
    private int returnTimes;

    /**
     * 时间间隔；
     */
    private int intervalTime;

    /**
     * 时间间隔；
     */
    private long speed;

    /**
     * 位置；
     */
    private long location;

    /**
     * 一圈绝对位置
     */
    private long locationOne;

    public int getRunStatus() {
        return runStatus;
    }

    public void setRunStatus(int runStatus) {
        this.runStatus = runStatus;
    }

    public int getRunPulse() {
        return runPulse;
    }

    public void setRunPulse(int runPulse) {
        this.runPulse = runPulse;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getFunCode() {
        return funCode;
    }

    public void setFunCode(String funCode) {
        this.funCode = funCode;
    }

    public int getEngineNo() {
        return engineNo;
    }

    public void setEngineNo(int engineNo) {
        this.engineNo = engineNo;
    }

    public int getReturnTimes() {
        return returnTimes;
    }

    public void setReturnTimes(int returnTimes) {
        this.returnTimes = returnTimes;
    }

    public int getIntervalTime() {
        return intervalTime;
    }

    public void setIntervalTime(int intervalTime) {
        this.intervalTime = intervalTime;
    }

    public long getSpeed() {
        return speed;
    }

    public int getSpeedRpm() {
        return (int) (speed * 6000 / 16384);
    }

    public void setSpeed(long speed) {
        this.speed = speed;
    }

    public long getLocation() {
        return location;
    }

    public int getLocationRpm() {
        return (int) (location * 360 / 16384);
    }

    public void setLocation(long location) {
        this.location = location;
    }

    public long getLocationOne() {
        return locationOne;
    }

    public int getLocationOneRpm() {
        return (int) (locationOne * 360 / 16384);
    }


    public void setLocationOne(long locationOne) {
        this.locationOne = locationOne;
    }
}
