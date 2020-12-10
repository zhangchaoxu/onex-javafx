package com.mellonrobot.faceunlockfx.report;

import cn.afterturn.easypoi.excel.annotation.Excel;
import javafx.beans.property.SimpleStringProperty;

/**
 * 检测项
 */
public class CheckItemEntity {

    @Excel(name = "序号", width = 10)
    private SimpleStringProperty ID = new SimpleStringProperty();
    @Excel(name = "类型", width = 10)
    private SimpleStringProperty TYPE = new SimpleStringProperty();
    @Excel(name = "时间", width = 30)
    private SimpleStringProperty TIME = new SimpleStringProperty();
    // @Excel(name = "耗时(ms)", width = 20)
    private SimpleStringProperty DURATION = new SimpleStringProperty();
    @Excel(name = "距离(mm)", width = 20)
    private SimpleStringProperty DISTANCE = new SimpleStringProperty();
    @Excel(name = "位置角")
    private SimpleStringProperty POS = new SimpleStringProperty();
    @Excel(name = "水平角")
    private SimpleStringProperty HORIZONTAL = new SimpleStringProperty();
    @Excel(name = "仰俯角")
    private SimpleStringProperty PITCH = new SimpleStringProperty();
    @Excel(name = "倾斜角")
    private SimpleStringProperty INCLINATION = new SimpleStringProperty();
    @Excel(name = "验证成功与否")
    private SimpleStringProperty RESULT = new SimpleStringProperty();
    @Excel(name = "备注")
    private SimpleStringProperty REMARK = new SimpleStringProperty();
    //@Excel(name = "角度")
    private SimpleStringProperty ROTATION = new SimpleStringProperty();
    //@Excel(name = "坐标")
    private SimpleStringProperty POSITION = new SimpleStringProperty();
    //@Excel(name = "名称", width = 30)
    private SimpleStringProperty NAME = new SimpleStringProperty();
    //@Excel(name = "报告ID", width = 30)
    private SimpleStringProperty REPORT_ID = new SimpleStringProperty();
    // @Excel(name = "电量", width = 30)
    private SimpleStringProperty BATTERY = new SimpleStringProperty();

    public String getID() {
        return ID.get();
    }

    public SimpleStringProperty IDProperty() {
        return ID;
    }

    public void setID(String ID) {
        this.ID.set(ID);
    }

    public String getREPORT_ID() {
        return REPORT_ID.get();
    }

    public SimpleStringProperty REPORT_IDProperty() {
        return REPORT_ID;
    }

    public void setREPORT_ID(String REPORT_ID) {
        this.REPORT_ID.set(REPORT_ID);
    }

    public String getNAME() {
        return NAME.get();
    }

    public SimpleStringProperty NAMEProperty() {
        return NAME;
    }

    public void setNAME(String NAME) {
        this.NAME.set(NAME);
    }

    public String getTIME() {
        return TIME.get();
    }

    public SimpleStringProperty TIMEProperty() {
        return TIME;
    }

    public void setTIME(String TIME) {
        this.TIME.set(TIME);
    }

    public String getDISTANCE() {
        return DISTANCE.get();
    }

    public SimpleStringProperty DISTANCEProperty() {
        return DISTANCE;
    }

    public void setDISTANCE(String DISTANCE) {
        this.DISTANCE.set(DISTANCE);
    }

    public String getHORIZONTAL() {
        return HORIZONTAL.get();
    }

    public SimpleStringProperty HORIZONTALProperty() {
        return HORIZONTAL;
    }

    public void setHORIZONTAL(String HORIZONTAL) {
        this.HORIZONTAL.set(HORIZONTAL);
    }

    public String getPITCH() {
        return PITCH.get();
    }

    public SimpleStringProperty PITCHProperty() {
        return PITCH;
    }

    public void setPITCH(String PITCH) {
        this.PITCH.set(PITCH);
    }

    public String getINCLINATION() {
        return INCLINATION.get();
    }

    public SimpleStringProperty INCLINATIONProperty() {
        return INCLINATION;
    }

    public void setINCLINATION(String INCLINATION) {
        this.INCLINATION.set(INCLINATION);
    }

    public String getRESULT() {
        return RESULT.get();
    }

    public SimpleStringProperty RESULTProperty() {
        return RESULT;
    }

    public void setRESULT(String RESULT) {
        this.RESULT.set(RESULT);
    }

    public String getROTATION() {
        return ROTATION.get();
    }

    public SimpleStringProperty ROTATIONProperty() {
        return ROTATION;
    }

    public void setROTATION(String ROTATION) {
        this.ROTATION.set(ROTATION);
    }

    public String getDURATION() {
        return DURATION.get();
    }

    public SimpleStringProperty DURATIONProperty() {
        return DURATION;
    }

    public void setDURATION(String DURATION) {
        this.DURATION.set(DURATION);
    }

    public String getREMARK() {
        return REMARK.get();
    }

    public SimpleStringProperty REMARKProperty() {
        return REMARK;
    }

    public void setREMARK(String REMARK) {
        this.REMARK.set(REMARK);
    }

    public String getPOSITION() {
        return POSITION.get();
    }

    public SimpleStringProperty POSITIONProperty() {
        return POSITION;
    }

    public void setPOSITION(String POSITION) {
        this.POSITION.set(POSITION);
    }

    public String getBATTERY() {
        return BATTERY.get();
    }

    public SimpleStringProperty BATTERYProperty() {
        return BATTERY;
    }

    public void setBATTERY(String BATTERY) {
        this.BATTERY.set(BATTERY);
    }

    public String getTYPE() {
        return TYPE.get();
    }

    public SimpleStringProperty TYPEProperty() {
        return TYPE;
    }

    public void setTYPE(String TYPE) {
        this.TYPE.set(TYPE);
    }

    public String getPOS() {
        return POS.get();
    }

    public SimpleStringProperty POSProperty() {
        return POS;
    }

    public void setPOS(String POS) {
        this.POS.set(POS);
    }
}

