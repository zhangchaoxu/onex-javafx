package com.mellonrobot.faceunlockfx;

import cn.afterturn.easypoi.excel.ExcelExportUtil;
import cn.afterturn.easypoi.excel.entity.ExportParams;
import com.github.cosysoft.device.android.AndroidDevice;
import com.github.cosysoft.device.android.impl.AndroidDeviceStore;
import com.jacob.activeX.ActiveXComponent;
import com.jacob.com.ComThread;
import com.jacob.com.Dispatch;
import com.jacob.com.Variant;
import com.mellonrobot.faceunlockfx.report.CheckItemEntity;
import com.mellonrobot.faceunlockfx.report.ReportEntity;
import com.mellonrobot.faceunlockfx.utils.IniHelper;
import com.mellonrobot.faceunlockfx.utils.NumberUtils;
import com.mellonrobot.faceunlockfx.utils.TimeUtils;
import de.felixroske.jfxsupport.FXMLController;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.StageStyle;
import javafx.util.Pair;
import jssc.SerialPortList;
import org.apache.poi.ss.usermodel.Workbook;
import org.ini4j.Wini;
import org.springframework.util.StringUtils;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.URL;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

@FXMLController
public class MainController extends BaseController implements Initializable {

    // 当前选中的报告
    ReportEntity report;

    // 导轨
    ActiveXComponent electricControlActiveX;
    // 机械臂
    ActiveXComponent mechanicalActiveX;
    // 导轨连接状态
    boolean electricControlLinked = false;
    // 机械臂连接状态
    boolean mechanicalLinked = false;
    // 安卓文件路径
    String screenFilePath;
    // 导轨设备id
    int electricControlId = 1;
    // 导轨波特率和COM口
    int electricControlButo = 9600;
    // 排除的com口
    String electricControlNotCom = "";
    // 角度转转弧度
    double angleToRadian = 57.3;
    // 导轨脉冲和距离换算,等价于1mm
    double electricControlPulsesToLength;
    // 移动测试水平角测试范围
    int[] horizontalAngle;
    // 水平角测试范围
    int[] moveHorizontalAngle;
    // 水平角机械臂转轴位置
    int horizontalAngleRotationPosition;
    // 仰俯角测试范围
    int[] pitchAngle;
    // 仰俯角测试范围机械臂转轴位置
    int pitchAngleRotationPosition;
    // 倾斜角测试范围
    int[] inclinationAngle;
    // 倾斜角机械臂转轴位置
    int inclinationAngleRotationPosition;
    // 测试间隔时间
    long sleepTime;
    // 移动测试间隔时间
    long moveSleepTime;
    // 移动测试间隔时间
    long moveMoveSleepTime;
    // 导轨初始位置
    int reportElectricControlInitDistance;
    // 导轨默认移动方向
    String reportElectricControlDirect;
    // 导轨测试范围
    int[] reportElectricControlDistance;
    // 初始位置
    double[] mechanicalInitRotation;
    // 机械臂中心位置
    double[] mechanicalPosZ;
    // 是否截图
    boolean takeScreenshot;
    // 移动监测模式
    String reportMoveType = "Rotation";

    // 配置文件
    Wini ini = null;

    // 安卓设备
    @FXML
    Button androidOpenBtn;
    @FXML
    Label androidName;
    @FXML
    Label mechanicalName;
    @FXML
    Label electricControlName;

    // 记录
    @FXML
    org.kordamp.bootstrapfx.scene.layout.Panel logPanel;
    @FXML
    TextArea logTextArea;
    @FXML
    Text logMsg;
    @FXML
    TextFlow logMsgFlow;

    // 检测表格
    @FXML
    TableView<CheckItemEntity> reportTable;
    @FXML
    TableColumn<CheckItemEntity, String> tableID;
    @FXML
    TableColumn<CheckItemEntity, String> tableTYPE;
    @FXML
    TableColumn<CheckItemEntity, String> tableTIME;
    @FXML
    TableColumn<CheckItemEntity, String> tableDISTANCE;
    @FXML
    TableColumn<CheckItemEntity, String> tablePOS;
    @FXML
    TableColumn<CheckItemEntity, String> tableHORIZONTAL;
    @FXML
    TableColumn<CheckItemEntity, String> tablePITCH;
    @FXML
    TableColumn<CheckItemEntity, String> tableINCLINATION;
    @FXML
    TableColumn<CheckItemEntity, String> tableRESULT;
    @FXML
    TableColumn<CheckItemEntity, String> tableBATTERY;
    // 项目
    @FXML
    Button reportAddBtn;
    @FXML
    ChoiceBox<ReportEntity> reportChoice;
    @FXML
    ChoiceBox<String> reportDeviceChoice;
    // 导轨
    @FXML
    TextField electricControlDistance;
    @FXML
    ChoiceBox<String> electricControlDirect;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadIni();

        initSqlite();
        initViews();
        Platform.runLater(() -> {
            initCOM();
            // 打开导轨
            electricControlLink();
            // 打开android
            androidLink();
            // 打开机械臂,在异常情况下非常耗时
            new Thread(this::mechanicalLink).start();
        });
    }

    /**
     * 初始化部分显示控件
     */
    private void initViews() {
        // 控制右侧日志区
        logPanel.managedProperty().bind(logPanel.visibleProperty());
        // 初始化表格
        tableID.setCellValueFactory(new PropertyValueFactory<>("ID"));
        tableID.prefWidthProperty().bind(reportTable.widthProperty().multiply(0.05));
        tableTYPE.setCellValueFactory(new PropertyValueFactory<>("TYPE"));
        tableTYPE.prefWidthProperty().bind(reportTable.widthProperty().multiply(0.1));
        tableTIME.setCellValueFactory(new PropertyValueFactory<>("TIME"));
        tableTIME.prefWidthProperty().bind(reportTable.widthProperty().multiply(0.3));
        tableDISTANCE.setCellValueFactory(new PropertyValueFactory<>("DISTANCE"));
        tableDISTANCE.prefWidthProperty().bind(reportTable.widthProperty().multiply(0.1));
        tablePOS.setCellValueFactory(new PropertyValueFactory<>("POS"));
        tablePOS.prefWidthProperty().bind(reportTable.widthProperty().multiply(0.1));
        tableHORIZONTAL.setCellValueFactory(new PropertyValueFactory<>("HORIZONTAL"));
        tableHORIZONTAL.prefWidthProperty().bind(reportTable.widthProperty().multiply(0.1));
        tablePITCH.setCellValueFactory(new PropertyValueFactory<>("PITCH"));
        tablePITCH.prefWidthProperty().bind(reportTable.widthProperty().multiply(0.1));
        tableINCLINATION.setCellValueFactory(new PropertyValueFactory<>("INCLINATION"));
        tableINCLINATION.prefWidthProperty().bind(reportTable.widthProperty().multiply(0.1));
        tableRESULT.setCellValueFactory(new PropertyValueFactory<>("RESULT"));
        tableRESULT.prefWidthProperty().bind(reportTable.widthProperty().multiply(0.05));
        //tableBATTERY.setCellValueFactory(new PropertyValueFactory<>("BATTERY"));
        // 导轨
        electricControlDirect.getItems().add("正转");
        electricControlDirect.getItems().add("反转");
        electricControlDirect.setValue("正转");
        // 加载报告列表
        loadReportList(true);
        reportChoice.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
            List<ReportEntity> reportList = queryReportEntityListFromDb();
            if (newValue.intValue() >= 0 && reportList.size() > 0 && reportList.size() > newValue.intValue()) {
                report = reportList.get(newValue.intValue());
            } else {
                report = null;
            }
            loadCheckItemList();
        });
        reportDeviceChoice.getItems().add("连检测设备");
        reportDeviceChoice.getItems().add("不连检测设备");
        reportDeviceChoice.setValue("连检测设备");
    }

    /**
     * 加载项目列表
     */
    private void loadReportList(boolean refreshCheckItemTable) {
        List<ReportEntity> reportList = queryReportEntityListFromDb();
        reportChoice.getItems().remove(0, reportChoice.getItems().size());
        for (ReportEntity reportEntity : reportList) {
            reportChoice.getItems().add(reportEntity);
        }
        if (reportList.size() > 0) {
            report = reportList.get(0);
            reportChoice.setValue(report);
            loadCheckItemList();
        } else {
            report = null;
            reportTable.setItems(FXCollections.emptyObservableList());
        }
    }

    /**
     * 加载检测项列表
     */
    private void loadCheckItemList() {
        reportTable.setItems(FXCollections.observableList(queryCheckItemListFromDb("ASC")));
    }

    // sqlite state
    Statement sqliteStatement;

    private void initSqlite() {
        try {
            Connection sqliteConnection = DriverManager.getConnection("jdbc:sqlite:" + System.getProperty("user.dir") + "\\db\\data.db");
            sqliteStatement = sqliteConnection.createStatement();
        } catch (SQLException e) {
            e.printStackTrace();
            addToLog("初始化数据库连接=>异常=>[" + e.getMessage() + "]", "danger");
        }
    }

    private boolean saveCheckItemRecord(CheckItemEntity checkItem) {
        try {
            sqliteStatement.executeUpdate("insert into CHECKITEM(REPORT_ID, TYPE, NAME, TIME, DURATION, DISTANCE, POS, HORIZONTAL, PITCH, INCLINATION, ROTATION, POSITION, BATTERY, RESULT, DEL) " +
                    "values(" + checkItem.getREPORT_ID()
                    + ",'" + checkItem.getTYPE()
                    + "','" + checkItem.getNAME()
                    + "', '" + checkItem.getTIME()
                    + "', '" + checkItem.getDURATION()
                    + "', '" + checkItem.getDISTANCE()
                    + "', '" + checkItem.getPOS()
                    + "', '" + checkItem.getHORIZONTAL()
                    + "', '" + checkItem.getPITCH()
                    + "', '" + checkItem.getINCLINATION()
                    + "', '" + checkItem.getROTATION()
                    + "', '" + checkItem.getPOSITION()
                    + "', '" + checkItem.getBATTERY()
                    + "', '" + checkItem.getRESULT()
                    + "', 0)");
            addToLog("插入检测结果到数据库=>成功", "success");
            // 刷新表格
            // 加载报告列表
            Platform.runLater(() -> {
                if (reportTable.getItems().size() == 0) {
                    checkItem.setID("1");
                    reportTable.setItems(FXCollections.observableArrayList(checkItem));
                } else {
                    checkItem.setID(String.valueOf(reportTable.getItems().size() + 1));
                    reportTable.getItems().add(checkItem);
                    //reportTable.scrollTo(checkItem);
                }
            });
            return true;
        } catch (SQLException e) {
            addToLog("插入数据库记录=>异常=>[" + e.getMessage() + "]", "danger");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 保存项目
     */
    private String saveReport(String NAME, String TIME) {
        try {
            // 先检查一下是否存在该name
            ResultSet rs = sqliteStatement.executeQuery("SELECT * FROM REPORT WHERE NAME = '" + NAME + "' and DEL = 0");
            if (rs.next()) {
                return "该名字已存在";
            }
            sqliteStatement.executeUpdate("insert into REPORT(NAME, TIME, STATUS, DEL) " + "values('" + NAME + "', '" + TIME + "', " + 0 + "," + 0 + ")");
            return "ok";
        } catch (SQLException e) {
            e.printStackTrace();
            return "插入数据库=>报告记录=>异常=>[" + e.getMessage() + "]";
        }
    }

    /**
     * 初始化配置文件内容
     */
    private void loadIni() {
        ini = IniHelper.loadIni("default.ini");
        if (null == ini) {
            showDialog(Alert.AlertType.ERROR, "找不到配置文件");
        } else {
            // 安卓
            screenFilePath = ini.get("android", "screenFilePath");
            // 导轨设备id
            electricControlId = ini.get("electricControl", "electricControlId", Integer.class);
            electricControlButo = ini.get("electricControl", "electricControlButo", Integer.class);
            electricControlNotCom = ini.get("electricControl", "electricControlNotCom", String.class);
            // 角度转弧度
            angleToRadian = ini.get("mechanical", "angleToRadian", Double.class);
            electricControlPulsesToLength = ini.get("electricControl", "electricControlPulsesToLength", Double.class);
            // 报告参数
            horizontalAngle = NumberUtils.stringToIntList(ini.get("report", "horizontalAngle"), ",");
            moveHorizontalAngle = NumberUtils.stringToIntList(ini.get("report", "moveHorizontalAngle"), ",");
            horizontalAngleRotationPosition = ini.get("report", "horizontalAngleRotationPosition", Integer.class);
            pitchAngle = NumberUtils.stringToIntList(ini.get("report", "pitchAngle"), ",");
            pitchAngleRotationPosition = ini.get("report", "pitchAngleRotationPosition", Integer.class);
            inclinationAngle = NumberUtils.stringToIntList(ini.get("report", "inclinationAngle"), ",");
            inclinationAngleRotationPosition = ini.get("report", "inclinationAngleRotationPosition", Integer.class);
            // 测试间隔时间
            sleepTime = ini.get("report", "sleepTime", Long.class);
            moveSleepTime = ini.get("report", "moveSleepTime", Long.class);
            moveMoveSleepTime = ini.get("report", "moveMoveSleepTime", Long.class);
            reportElectricControlDirect = ini.get("report", "electricControlDirect");
            reportElectricControlDistance = NumberUtils.stringToIntList(ini.get("report", "electricControlDistance"), ",");
            reportElectricControlInitDistance = ini.get("report", "electricControlInitDistance", Integer.class);
            // 初始位置
            mechanicalInitRotation = NumberUtils.stringToDoubleList(ini.get("mechanical", "mechanicalInitRotation"), ",");
            mechanicalPosZ = NumberUtils.stringToDoubleListNoScale(ini.get("mechanical", "mechanicalPosZ"), ",");
            // 是否截图
            takeScreenshot = ini.get("report", "takeScreenshot", Boolean.class);
            reportMoveType = ini.get("report", "reportMoveType");
            if (StringUtils.isEmpty(reportMoveType)) {
                reportMoveType = "reportMoveType";
            }
        }
    }

    /**
     * 初始化导轨和机械臂
     */
    private void initCOM() {
        ComThread.InitSTA();
        try {
            // 导轨
            electricControlActiveX = new ActiveXComponent("MechanicalArm.ElectricControl");
            // 机械臂
            mechanicalActiveX = new ActiveXComponent("MechanicalArm.Mechanical");
        } catch (Exception e) {
            e.printStackTrace();
            addToLog("初始化导轨和机械臂COM组件失败", "danger");
        }
    }

    // [+]android

    private AndroidDevice getLinkedAndroidDevice() {
        AndroidDevice androidDevice = AndroidDeviceStore.getInstance().getDevices().pollFirst();
        if (null == androidDevice) {
            addToLog("未找到连接的安卓设备", "danger");
            setAndroidName("未找到连接的安卓设备", "danger");
            return null;
        } else {
            return androidDevice;
        }
    }

    /**
     * 连接android设备
     */
    public void androidLink() {
        AndroidDevice androidDevice = getLinkedAndroidDevice();
        if (null == androidDevice) {
            return;
        }

        addToLog("已连接安卓设备:" + androidDevice.getName());
        setAndroidName("已连接:" + androidDevice.getName(), "success");
        // 打开FaceUnlockApp
        androidDevice.start("cn.mellonrobot.faceunlock", "cn.mellonrobot.faceunlock.MainActivity");
    }

    /**
     * 初始化socket
     */
    private void initSocket() {
        int localPort = ini.get("android", "localPort", Integer.class);
        int remotePort = ini.get("android", "remotePort", Integer.class);
        String remoteHost = ini.get("android", "remoteHost", String.class);
        AndroidDevice androidDevice = getLinkedAndroidDevice();
        if (null == androidDevice) {
            return;
        }
        androidDevice.forwardPort(localPort, remotePort);
        try {
            Socket client = new Socket(remoteHost, localPort);
            // 得到socket管道中的输出流--------------像手机端写数据
            // final BufferedOutputStream out = new BufferedOutputStream(client.getOutputStream());
            // 得到socket管道中的输人流--------------读取手机端的数据
            final BufferedInputStream in = new BufferedInputStream(client.getInputStream());
            new Thread(() -> {
                while (true) {
                    if (!client.isConnected()) {
                        break;
                    }
                    readMsgFromAndroidSocket(in);
                }
            }).start();
            addToLog("已连接安卓设备:" + androidDevice.getName() + "=>socket连接完成");
            setAndroidName("已连接:" + androidDevice.getName(), "success");
        } catch (Exception e) {
            e.printStackTrace();
            addToLog("连接检测设备Socket失败,请确认设备已连接,设备上的FaceUnlock应用已启动", "danger");
            setAndroidName("Socket连接失败", "danger");
        }
    }

    /**
     * 锁屏
     */
    public void androidPowerKey() {
        AndroidDevice androidDevice = getLinkedAndroidDevice();
        if (null == androidDevice) {
            return;
        }

        androidDevice.inputKeyevent(26);
        addToLog("模拟安卓电源按键", "info");
    }

    /**
     * 获取设备发送过来的数据
     */
    private byte readMsgFromAndroidSocket(InputStream in) {
        byte[] tempbuffer = new byte[1024];
        try {
            int length = in.read(tempbuffer, 0, tempbuffer.length);
            if (length > 0) {
                byte result = tempbuffer[0];
                // androidScreenStatus = result;
                if (1 == result) {
                    addToLog("开屏");
                } else if (2 == result) {
                    addToLog("锁屏");
                } else if (3 == result) {
                    addToLog("解锁");
                }
                return tempbuffer[0];
            } else {
                return 0;
            }
        } catch (IOException e) {
            e.printStackTrace();
            addToLog("安卓Socket读取失败,请尝试重连安卓");
            return 0;
        }
    }

    /**
     * 设置安卓的显示内容
     */
    private void setAndroidName(String txt, String type) {
        androidName.setText(txt);
        androidName.getStyleClass().remove("text-default");
        androidName.getStyleClass().remove("text-success");
        androidName.getStyleClass().remove("text-danger");
        androidName.getStyleClass().remove("text-warning");
        androidName.getStyleClass().add("text-" + type);
    }
    // [-]android

    // [+]机械臂

    /**
     * 连接
     */
    public void mechanicalLink() {
        mechanicalLinked = false;
        try {
            Variant setMechanicalResult = Dispatch.call(mechanicalActiveX, "SetMechanical", ini.get("mechanical", "mechanicalIP"), ini.get("mechanical", "mechanicalPort"));
            if (setMechanicalResult.getBoolean()) {
                Variant linkResult = Dispatch.call(mechanicalActiveX, "Link");
                if (0 == linkResult.getInt()) {
                    mechanicalLinked = true;
                    Platform.runLater(() -> {
                        addToLog("机械臂连接=>[成功]");
                        setMechanicalName("连接成功", "success");
                    });
                } else {
                    Platform.runLater(() -> {
                        addToLog("机械臂连接=>[失败" + linkResult.getInt() + "]");
                        setMechanicalName("连接失败:" + linkResult.getInt(), "danger");
                    });
                }
            } else {
                Platform.runLater(() -> {
                    addToLog("机械臂设置IP端口=>[失败]");
                    setMechanicalName("机械臂设置IP端口=>[失败]", "danger");
                });
            }


        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> {
                addToLog("机械臂连接异常[" + e.getMessage() + "]", "danger");
                setMechanicalName("连接异常:" + e.getMessage(), "danger");
            });

        }
    }

    /**
     * 关闭
     */
    public void mechanicalClose() {
        try {
            Variant result = Dispatch.call(mechanicalActiveX, "Close");
            addToLog("机械臂=>关闭=>[" + result.getInt() + "]");
        } catch (Exception e) {
            e.printStackTrace();
            addToLog("机械臂=>关闭=>异常[" + e.getMessage() + "]", "danger");
        }
    }

    /**
     * 是否连接
     */
    public void mechanicalIsLink() {
        try {
            Variant result = Dispatch.call(mechanicalActiveX, "isLink");
            addToLog("机械臂=>连接状态=>[" + (result.getBoolean() ? "已连接" : "未连接") + "]");
        } catch (Exception e) {
            e.printStackTrace();
            addToLog("机械臂=>连接状态=>异常[" + e.getMessage() + "]", "danger");
        }
    }

    /**
     * 停止
     */
    public void mechanicalStop() {
        try {
            Variant result = Dispatch.call(mechanicalActiveX, "Stop");
            addToLog("机械臂=>停止=>[" + result.getInt() + "]");
        } catch (Exception e) {
            e.printStackTrace();
            addToLog("机械臂=>停止=>异常[" + e.getMessage() + "]", "danger");
        }
    }

    /**
     * 暂停
     */
    public void mechanicalPause() {
        try {
            Variant result = Dispatch.call(mechanicalActiveX, "Pause");
            addToLog("机械臂=>暂停=>[" + result.getInt() + "]");
        } catch (Exception e) {
            e.printStackTrace();
            addToLog("机械臂=>暂停=>异常[" + e.getMessage() + "]", "danger");
        }
    }

    /**
     * 获取当前位置
     */
    public void mechanicalGetPosition() {
        if (null == mechanicalActiveX) {
            showDialog(Alert.AlertType.ERROR, "未找到机械手组件");
            return;
        }
        Variant result = Dispatch.call(mechanicalActiveX, "GetPosition");
        addToLog("机械臂=>获取当前位置=>[" + result.getString() + "]");
    }

    /**
     * 回到初始角度
     */
    public void mechanicalGoInitRotation() {
        String value = ini.get("mechanical", "mechanicalInitRotation", String.class);
        mechanicalInitRotation(value);
    }

    private void mechanicalInitRotation(String mechanicalRotationValue) {
        double[] param = NumberUtils.stringToDoubleList(mechanicalRotationValue, ",", 1 / angleToRadian);
        if (param.length != 6) {
            addToLog("机械臂=>角度参数异常", "danger");
        } else {
            try {
                Variant result = Dispatch.call(mechanicalActiveX, "AxisRotation", param);
                addToLog("机械臂=>旋转=>[" + result.getInt() + "]");
            } catch (Exception e) {
                e.printStackTrace();
                addToLog("机械臂=>旋转异常=>[" + e.getMessage() + "]");
            }
        }
    }

    // 试跑一圈的进程
    private Thread mechanicalLoopMoveThread;

    public void mechanicalLoopMoveAngle() {
        if (mechanicalLoopMoveThread != null && mechanicalLoopMoveThread.isAlive()) {
            mechanicalLoopMoveThread.interrupt();
            addToLog("试跑一圈执行中,停止执行", "info");
        } else {
            // 停顿时间
            long loopSleepTime = ini.get("report", "loopSleepTime", Long.class);
            Variant setOffsetCenterResult = Dispatch.call(mechanicalActiveX, "SetOffsetCenter", mechanicalPosZ);
            addToLog("机械臂=>设置中心点=>[" + NumberUtils.listToString(mechanicalPosZ, ",") + "]=>[" + setOffsetCenterResult.getBoolean() + "]");
            // 获得当前的姿态
            Variant getEuleResult = Dispatch.call(mechanicalActiveX, "GetEuler");
            double[] getEuleResultDouble = NumberUtils.stringToDoubleListNoScale(getEuleResult.getString(), ",");
            addToLog("机械臂=>获取当前姿态=>[" + getEuleResult.getString() + "]");
            if (getEuleResultDouble.length != 3) {
                return;
            }
            // 获得当前的坐标
            Variant getPositionResult = Dispatch.call(mechanicalActiveX, "GetPosition");
            double[] getPositionResultDouble = NumberUtils.stringToDoubleListNoScale(getPositionResult.getString(), ",");
            addToLog("机械臂=>获取当前坐标=>[" + getPositionResult.getString() + "]");
            if (getPositionResultDouble.length != 3) {
                return;
            }
            double[] param = new double[]{getPositionResultDouble[0], getPositionResultDouble[1], getPositionResultDouble[2], getEuleResultDouble[0], getEuleResultDouble[1], getEuleResultDouble[2]};
            // 姿态1 倾斜角 2 俯仰角 3 水平角
            mechanicalLoopMoveThread = new Thread(() -> {
                try {
                    for (double horizontalAngleItem : horizontalAngle) {
                        // 水平角
                        for (double pitchAngleItem : pitchAngle) {
                            // 仰俯角
                            for (double inclinationAngleItem : inclinationAngle) {
                                // 倾斜角
                                /*Platform.runLater(() -> {
                                });*/
                                Variant result = Dispatch.call(mechanicalActiveX, "MoveWithAngle", NumberUtils.paramAddAngle(param, horizontalAngleRotationPosition, horizontalAngleItem / angleToRadian,
                                        pitchAngleRotationPosition, pitchAngleItem / angleToRadian,
                                        inclinationAngleRotationPosition, inclinationAngleItem / angleToRadian));
                                if (result.getString().contains("N")) {
                                    mechanicalLoopMoveThread.interrupt();
                                    addToLog("机械臂旋=>姿态移动=>出错" + result.getString() + ",停止执行", "danger");
                                }
                                addToLog("机械臂=>姿态移动到水平角[" + horizontalAngleItem + "]/仰俯角[" + pitchAngleItem + "]/倾斜角[" + inclinationAngleItem + "]=>[" + result.getString() + "]");
                                Thread.sleep(loopSleepTime);
                            }
                        }
                    }
                    addToLog("试跑结束");
                    mechanicalGoInitRotation();
                } catch (Exception e) {
                    // 被interrupt后Thread.sleep会抛异常,通过异常来终端thread
                    e.printStackTrace();
                }
            });
            mechanicalLoopMoveThread.start();
        }
    }

    /**
     * 机械臂循环移动
     */
    public void mechanicalLoopMove() {
        if (mechanicalLoopMoveThread != null && mechanicalLoopMoveThread.isAlive()) {
            mechanicalLoopMoveThread.interrupt();
            addToLog("试跑一圈执行中,停止执行", "info");
        } else {
            double[] param = NumberUtils.listMultFactor(mechanicalInitRotation, 1 / angleToRadian);
            mechanicalLoopMoveThread = new Thread(() -> {
                try {
                    for (double horizontalAngleItem : horizontalAngle) {
                        // 水平角
                        for (double pitchAngleItem : pitchAngle) {
                            // 仰俯角
                            for (double inclinationAngleItem : inclinationAngle) {
                                // 倾斜角
                                Variant result = Dispatch.call(mechanicalActiveX, "AxisRotation",
                                        NumberUtils.paramAddAngle(param, horizontalAngleRotationPosition, horizontalAngleItem / angleToRadian,
                                                pitchAngleRotationPosition, pitchAngleItem / angleToRadian,
                                                inclinationAngleRotationPosition, inclinationAngleItem / angleToRadian));
                                int rotationResult = result.getInt();
                                if (rotationResult != 0) {
                                    mechanicalLoopMoveThread.interrupt();
                                    addToLog("机械臂旋=>转移=>出错[" + rotationResult + "],停止执行", "danger");
                                }
                                addToLog("机械臂=>旋转到水平角[" + horizontalAngleItem + "]/仰俯角[" + pitchAngleItem + "]/倾斜角[" + inclinationAngleItem + "]=>[" + rotationResult + "]");
                                Thread.sleep(2000);
                            }
                        }
                    }
                } catch (Exception e) {
                    // 被interrupt后Thread.sleep会抛异常,通过异常来终端thread
                    e.printStackTrace();
                }
            });
            mechanicalLoopMoveThread.start();
        }
    }

    /**
     * 设置基坐标系是底座
     */
    public void mechanicalSetBaseCoord() {
        if (null == mechanicalActiveX) {
            showDialog(Alert.AlertType.ERROR, "未找到机械手组件");
            return;
        }
        Variant result = Dispatch.call(mechanicalActiveX, "SetBaseCoord");
        addToLog("机械臂=>设置基坐标系是底座=>[" + result.getInt() + "]");
    }

    /**
     * 设置机械臂的显示内容
     */
    private void setMechanicalName(String txt, String type) {
        mechanicalName.setText(txt);
        mechanicalName.getStyleClass().remove("text-default");
        mechanicalName.getStyleClass().remove("text-success");
        mechanicalName.getStyleClass().remove("text-danger");
        mechanicalName.getStyleClass().remove("text-warning");
        mechanicalName.getStyleClass().add("text-" + type);
        mechanicalLinked = "success".equalsIgnoreCase(type);
    }

    // [-]机械臂

    // [+]导轨

    /**
     * 设置导轨并打开
     */
    public void electricControlLink() {
        electricControlLinked = false;
        String electricControlCom = "";
        // 排除串口
        String electricControlNotCom = ini.get("electricControl", "electricControlNotCom");
        // 读取目前的串口
        String[] ports = SerialPortList.getPortNames();
        for (String port : ports) {
            if (!electricControlNotCom.toUpperCase().contains(port.toUpperCase())) {
                electricControlCom = port;
            }
        }
        if (StringUtils.isEmpty(electricControlCom)) {
            addToLog("导轨=>打开=>[失败]=>无有效串口", "danger");
            setElectricControlName("无有效串口", "danger");
        } else {
            try {
                Variant setElectricControlResult = Dispatch.call(electricControlActiveX, "SetElectricControl", electricControlButo, electricControlCom);
                if (setElectricControlResult.getBoolean()) {
                    addToLog("导轨=>设置波特率[" + electricControlButo + "]/串口[" + electricControlCom + "]=>[成功]");
                    Variant openResult = Dispatch.call(electricControlActiveX, "Open");
                    if (openResult.getBoolean()) {
                        addToLog("导轨=>打开=>[成功]", "success");
                        setElectricControlName("已连接:" + electricControlCom, "success");
                    } else {
                        addToLog("导轨=>打开=>[失败]", "danger");
                        setElectricControlName("导轨=>打开=>[失败]", "danger");
                    }
                } else {
                    addToLog("导轨=>设置波特率[" + electricControlButo + "]/串口[" + electricControlCom + "]=>[失败]", "danger");
                    setElectricControlName("设置波特率/串口失败", "danger");
                }
            } catch (Exception e) {
                e.printStackTrace();
                String errorMessage = e.getMessage().split("Description:").length == 2 ? e.getMessage().split("Description:")[1].replaceAll("\n", "") : e.getMessage();
                addToLog("导轨=>连接=>[异常]:" + errorMessage + "]");
                setElectricControlName("连接异常:" + errorMessage, "danger");
            }
        }
    }

    /**
     * 设置导轨的显示内容
     *
     * @param txt
     * @param type
     */
    private void setElectricControlName(String txt, String type) {
        electricControlName.setText(txt);
        electricControlName.getStyleClass().remove("text-default");
        electricControlName.getStyleClass().remove("text-success");
        electricControlName.getStyleClass().remove("text-danger");
        electricControlName.getStyleClass().remove("text-warning");
        electricControlName.getStyleClass().add("text-" + type);
        electricControlLinked = "success".equalsIgnoreCase(type);
    }

    /**
     * 转动
     */
    public void electricControlRotation() {
        // 先设定脉冲(距离)
        Integer distance = NumberUtils.stringToInt(electricControlDistance.getText());
        if (distance == null) {
            addToLog("距离需未整数数字", "danger");
        } else {
            try {
                Variant setNumberOfPulsesResult = Dispatch.call(electricControlActiveX, "SetNumberOfPulses", electricControlId, distance * electricControlPulsesToLength);
                if (setNumberOfPulsesResult.getBoolean()) {
                    addToLog("导轨=>设置距离[" + distance + "mm]=>[成功]", "success");
                    Thread.sleep(500);
                    Variant rotationResult = Dispatch.call(electricControlActiveX, ("正转".equalsIgnoreCase(electricControlDirect.getValue()) ? "ForwardRotation" : "ReversalRotation"), electricControlId);
                    addToLog("导轨=>" + electricControlDirect.getValue() + "=>[" + (rotationResult.getBoolean() ? "成功" : "失败") + "]");
                } else {
                    addToLog("导轨=>设置距离[" + distance + "mm]=>[失败]", "danger");
                }
            } catch (Exception e) {
                e.printStackTrace();
                addToLog("导轨=>设置距离异常=>[" + e.getMessage() + "]", "danger");
            }
        }
    }

    /**
     * 正转
     */
    public void electricControlForwardRotation() {
        // 先设定脉冲(距离)
        Integer distance = NumberUtils.stringToInt(electricControlDistance.getText());
        if (distance == null) {
            addToLog("距离需未整数数字", "danger");
            return;
        } else {
            try {
                Variant result = Dispatch.call(electricControlActiveX, "SetNumberOfPulses", electricControlId, distance * electricControlPulsesToLength);
                if (result.getBoolean()) {
                    addToLog("导轨=>设置距离[" + distance + "mm]=>[成功]", "success");
                } else {
                    addToLog("导轨=>设置距离[" + distance + "mm]=>[失败]", "danger");
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
                addToLog("导轨=>设置距离异常=>[" + e.getMessage() + "]", "danger");
                return;
            }
        }
        // 转动
        try {
            Variant result = Dispatch.call(electricControlActiveX, "ForwardRotation", electricControlId);
            addToLog("导轨=>正转=>[" + (result.getBoolean() ? "成功" : "失败") + "]");
        } catch (Exception e) {
            e.printStackTrace();
            addToLog("导轨=>正转异常=>[" + e.getMessage() + "]");
        }
    }

    /**
     * 反转
     */
    public void electricControlReversalRotation() {
        // 先设定脉冲(距离)
        Integer distance = NumberUtils.stringToInt(electricControlDistance.getText());
        if (distance == null) {
            addToLog("距离需未整数数字", "danger");
            return;
        } else {
            try {
                Variant result = Dispatch.call(electricControlActiveX, "SetNumberOfPulses", electricControlId, distance * electricControlPulsesToLength);
                if (result.getBoolean()) {
                    addToLog("导轨=>设置距离[" + distance + "mm]=>[成功]", "success");
                } else {
                    addToLog("导轨=>设置距离[" + distance + "mm]=>[失败]", "danger");
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
                addToLog("导轨=>设置距离异常=>[" + e.getMessage() + "]", "danger");
                return;
            }
        }
        // 转动
        try {
            Variant result = Dispatch.call(electricControlActiveX, "ReversalRotation", electricControlId);
            addToLog("导轨=>反转=>[" + (result.getBoolean() ? "成功" : "失败") + "]");
        } catch (Exception e) {
            e.printStackTrace();
            addToLog("导轨=>反转异常=>[" + e.getMessage() + "]");
        }
    }

    /**
     * 停止
     */
    public void electricControlStopRotation() {
        try {
            Variant result = Dispatch.call(electricControlActiveX, "StopRotation", electricControlId);
            addToLog("导轨=>停止=>[" + (result.getBoolean() ? "成功" : "失败") + "]");
        } catch (Exception e) {
            e.printStackTrace();
            addToLog("导轨=>停止异常=>[" + e.getMessage() + "]");
        }
    }

    /**
     * 查询速度
     */
    public void electricControlCheckSpeed() {
        try {
            Variant result = Dispatch.call(electricControlActiveX, "CheckSpeed", electricControlId);
            addToLog("导轨=>速度=>[" + result.getString() + "]");
        } catch (Exception e) {
            e.printStackTrace();
            addToLog("导轨=>查询速度异常=>[" + e.getMessage() + "]");
        }
    }

    // [-]导轨

    /**
     * 系统设置
     */
    public void appSetting() {
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("系统设置");
        dialog.setHeaderText("保存后请重启程序");
        dialog.initStyle(StageStyle.UTILITY);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField titleText = new TextField(ini.get("App", "title"));
        TextField widthText = new TextField(ini.get("App", "width"));
        TextField heightText = new TextField(ini.get("App", "height"));
        CheckBox resizableCheckbox = new CheckBox();
        resizableCheckbox.setSelected(ini.get("App", "resizable", Boolean.class));
        CheckBox maximizedCheckbox = new CheckBox();
        maximizedCheckbox.setSelected(ini.get("App", "maximized", Boolean.class));

        grid.add(new Label("窗口标题:"), 0, 0);
        grid.add(titleText, 1, 0);
        grid.add(new Label("窗口宽度:"), 0, 1);
        grid.add(widthText, 1, 1);
        grid.add(new Label("窗口高度:"), 0, 2);
        grid.add(heightText, 1, 2);
        grid.add(new Label("窗口缩放:"), 0, 3);
        grid.add(resizableCheckbox, 1, 3);
        grid.add(new Label("窗口全屏:"), 0, 4);
        grid.add(maximizedCheckbox, 1, 4);
        dialog.getDialogPane().setContent(grid);

        Button btOk = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        btOk.addEventFilter(ActionEvent.ACTION, event -> {
            if (StringUtils.isEmpty(titleText.getText())) {
                dialog.setHeaderText("标题不能为空");
                event.consume();
            } else {
                ini.put("App", "title", titleText.getText());
                ini.put("App", "width", widthText.getText());
                ini.put("App", "height", heightText.getText());
                ini.put("App", "resizable", resizableCheckbox.isSelected());
                ini.put("App", "maximized", maximizedCheckbox.isSelected());
                try {
                    ini.store();
                    dialog.setHeaderText("保存成功");
                    loadIni();
                } catch (IOException e) {
                    dialog.setHeaderText("配置文件保存失败");
                }
                event.consume();

                System.out.println("保存系统设置成功");
            }
        });
        dialog.show();
    }

    /**
     * 机械臂设置
     */
    public void mechanicalSetting() {
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("机械臂设置");
        dialog.setHeaderText("保存后请重启程序");
        dialog.initStyle(StageStyle.UTILITY);
        ButtonType buttonTypeOne = new ButtonType("设置IP/端口");
        dialog.getDialogPane().getButtonTypes().addAll(buttonTypeOne, ButtonType.OK, ButtonType.CLOSE);

        GridPane grid = new GridPane();
        grid.setPrefWidth(400);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField ipText = new TextField(ini.get("mechanical", "mechanicalIP"));
        TextField portText = new TextField(ini.get("mechanical", "mechanicalPort"));
        TextField zeroRotationText = new TextField(ini.get("mechanical", "mechanicalZeroRotation"));
        TextField angleToRadianText = new TextField(ini.get("mechanical", "angleToRadian"));

        grid.add(new Label("IP地址:"), 0, 0);
        grid.add(ipText, 1, 0);
        grid.add(new Label("端口:"), 0, 1);
        grid.add(portText, 1, 1);
        grid.add(new Label("角度转弧度:"), 0, 2);
        grid.add(angleToRadianText, 1, 2);
        dialog.getDialogPane().setContent(grid);

        dialog.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(ActionEvent.ACTION, event -> {
            String ip = ipText.getText();
            Integer port = NumberUtils.stringToInt(portText.getText());
            Double angleToRadian = NumberUtils.stringToDouble(angleToRadianText.getText());
            if (StringUtils.isEmpty(ip)) {
                dialog.setHeaderText("IP不能为空");
                event.consume();
            } else if (null == port) {
                dialog.setHeaderText("端口为数字");
                event.consume();
            } else if (angleToRadian == null || angleToRadian == 0) {
                dialog.setHeaderText("角度转弧度为非0数字");
                event.consume();
            } else {
                ini.put("mechanical", "mechanicalIP", ip);
                ini.put("mechanical", "mechanicalPort", port);
                ini.put("mechanical", "angleToRadian", angleToRadian);
                try {
                    ini.store();
                    dialog.setHeaderText("保存成功");
                    loadIni();
                } catch (IOException e) {
                    dialog.setHeaderText("配置文件保存失败");
                }
                event.consume();
            }
        });
        dialog.getDialogPane().lookupButton(buttonTypeOne).addEventFilter(ActionEvent.ACTION, event -> {
            String ip = ipText.getText();
            Integer port = NumberUtils.stringToInt(portText.getText());
            if (StringUtils.isEmpty(ip)) {
                dialog.setHeaderText("IP不能为空");
                event.consume();
            } else if (null == port) {
                dialog.setHeaderText("端口需为数字");
                event.consume();
            } else {
                // 设置ip和端口
                if (null == mechanicalActiveX) {
                    dialog.setHeaderText("未找到机械手COM组件");
                } else {
                    Variant result = Dispatch.call(mechanicalActiveX, "SetMechanical", ip, port);
                    if (result.getBoolean()) {
                        dialog.setHeaderText("设置成功");
                    } else {
                        dialog.setHeaderText("设置失败");
                    }
                }
                event.consume();
            }
        });
        dialog.show();
    }

    /**
     * 机械臂初始位置设定
     */
    public void mechanicalRotationSetting() {
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("机械臂初始角度设置");
        dialog.setHeaderText("角度为6个逗号分隔的数字");
        dialog.initStyle(StageStyle.UTILITY);
        ButtonType buttonTypeOne = new ButtonType("获取");
        ButtonType buttonTypeTwo = new ButtonType("移动");
        dialog.getDialogPane().getButtonTypes().addAll(buttonTypeOne, buttonTypeTwo, ButtonType.OK, ButtonType.CLOSE);

        GridPane grid = new GridPane();
        grid.setPrefWidth(400);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField initRotationText = new TextField(ini.get("mechanical", "mechanicalInitRotation"));

        grid.add(new Label("初始角度:"), 0, 0);
        grid.add(initRotationText, 1, 0);
        dialog.getDialogPane().setContent(grid);

        dialog.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(ActionEvent.ACTION, event -> {
            String initRotation = initRotationText.getText();
            double[] initRotationDoubles = NumberUtils.stringToDoubleList(initRotation, ",");
            if (initRotationDoubles.length != 6) {
                dialog.setHeaderText("角度需为6个逗号分隔的数字");
                event.consume();
            } else {
                ini.put("mechanical", "mechanicalInitRotation", initRotation);
                mechanicalInitRotation = initRotationDoubles;
                try {
                    ini.store();
                    dialog.setHeaderText("保存成功");
                    loadIni();
                } catch (IOException e) {
                    dialog.setHeaderText("配置文件保存失败");
                }
                event.consume();
            }
        });
        dialog.getDialogPane().lookupButton(buttonTypeOne).addEventFilter(ActionEvent.ACTION, event -> {
            // 获取当前角度
            try {
                Variant result = Dispatch.call(mechanicalActiveX, "GetRotation");
                double[] resultDouble = NumberUtils.stringToDoubleList(result.getString(), ",", angleToRadian);
                if (resultDouble.length == 0) {
                    dialog.setHeaderText("机械臂=>获取当前旋转角度失败=>[" + result.getString() + "]");
                    addToLog("机械臂=>获取当前旋转角度失败=>[" + result.getString() + "]");
                } else {
                    String resultWithRadian = NumberUtils.listToString(resultDouble, ",");
                    dialog.setHeaderText("机械臂=>获取当前旋转角度成功=>[" + resultWithRadian + "]");
                    addToLog("机械臂=>获取当前旋转角度成功=>[" + resultWithRadian + "]");
                    initRotationText.setText(resultWithRadian);
                }
            } catch (Exception e) {
                addToLog("机械臂=>获取当前旋转角度异常=>[" + e.getMessage() + "]", "danger");
            }
            event.consume();
        });
        dialog.getDialogPane().lookupButton(buttonTypeTwo).addEventFilter(ActionEvent.ACTION, event -> {
            // 移动到该角度
            double[] param = NumberUtils.stringToDoubleList(initRotationText.getText(), ",", 1 / angleToRadian);
            if (param.length != 6) {
                addToLog("机械臂=>角度参数异常", "danger");
            } else {
                try {
                    Variant result = Dispatch.call(mechanicalActiveX, "AxisRotation", param);
                    addToLog("机械臂=>旋转=>[" + result.getInt() + "]");
                    dialog.setHeaderText("机械臂=>旋转=>[" + result.getInt() + "]");
                } catch (Exception e) {
                    e.printStackTrace();
                    addToLog("机械臂=>旋转异常=>[" + e.getMessage() + "]");
                    dialog.setHeaderText("机械臂=>旋转异常=>[" + e.getMessage() + "]");
                }
            }
            event.consume();
        });
        dialog.show();
    }

    /**
     * 机械臂中心点位设定
     */
    public void mechanicalCenterPositionSetting() {
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("机械臂中心点位设置");
        dialog.setHeaderText("3个逗号分隔的数字");
        dialog.initStyle(StageStyle.UTILITY);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CLOSE);

        GridPane grid = new GridPane();
        grid.setPrefWidth(400);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField mechanicalPosZText = new TextField(ini.get("mechanical", "mechanicalPosZ"));

        grid.add(new Label("中心位置(米):"), 0, 0);
        grid.add(mechanicalPosZText, 1, 0);
        dialog.getDialogPane().setContent(grid);

        dialog.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(ActionEvent.ACTION, event -> {
            double[] mechanicalPosZDoubles = NumberUtils.stringToDoubleListNoScale(mechanicalPosZText.getText(), ",");
            if (mechanicalPosZDoubles.length != 3) {
                dialog.setHeaderText("需为3个逗号分隔的数字");
                event.consume();
            } else {
                ini.put("mechanical", "mechanicalPosZ", mechanicalPosZText.getText());
                try {
                    ini.store();
                    dialog.setHeaderText("保存成功");
                    loadIni();
                } catch (IOException e) {
                    dialog.setHeaderText("配置文件保存失败");
                }
                event.consume();
            }
        });
        dialog.show();
    }

    /**
     * 机械臂初始坐标设置
     */
    public void mechanicalPositionSetting() {
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("机械臂初始坐标设置");
        dialog.setHeaderText("坐标为3个逗号分隔的数字");
        dialog.initStyle(StageStyle.UTILITY);
        ButtonType buttonTypeOne = new ButtonType("获取");
        ButtonType buttonTypeTwo = new ButtonType("移动");
        dialog.getDialogPane().getButtonTypes().addAll(buttonTypeOne, buttonTypeTwo, ButtonType.OK, ButtonType.CLOSE);

        GridPane grid = new GridPane();
        grid.setPrefWidth(400);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField initPositionText = new TextField(ini.get("mechanical", "mechanicalInitPosition"));

        grid.add(new Label("初始坐标:"), 0, 0);
        grid.add(initPositionText, 1, 0);
        dialog.getDialogPane().setContent(grid);

        dialog.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(ActionEvent.ACTION, event -> {
            String initRotation = initPositionText.getText();
            double[] initRotationDoubles = NumberUtils.stringToDoubleList(initRotation, ",");
            if (initRotationDoubles.length != 3) {
                dialog.setHeaderText("坐标需为3个逗号分隔的数字");
                event.consume();
            } else {
                ini.put("mechanical", "mechanicalInitPosition", initRotation);
                try {
                    ini.store();
                    dialog.setHeaderText("保存成功");
                    loadIni();
                } catch (IOException e) {
                    dialog.setHeaderText("配置文件保存失败");
                }
                event.consume();
            }
        });
        dialog.getDialogPane().lookupButton(buttonTypeOne).addEventFilter(ActionEvent.ACTION, event -> {
            // 获取当前角度
            try {
                Variant result = Dispatch.call(mechanicalActiveX, "GetPosition");
                double[] resultDouble = NumberUtils.stringToDoubleList(result.getString(), ",");
                if (resultDouble.length == 0) {
                    dialog.setHeaderText("机械臂=>获取当前位置失败=>[" + result.getString() + "]");
                    addToLog("机械臂=>获取当前位置失败=>[" + result.getString() + "]");
                } else {
                    String resultWithRadian = NumberUtils.listToString(resultDouble, ",");
                    dialog.setHeaderText("机械臂=>获取当前位置成功=>[" + resultWithRadian + "]");
                    addToLog("机械臂=>获取当前位置成功=>[" + resultWithRadian + "]");
                    initPositionText.setText(resultWithRadian);
                }
            } catch (Exception e) {
                addToLog("机械臂=>获取当前旋转角度异常=>[" + e.getMessage() + "]", "danger");
            }
            event.consume();
        });
        dialog.getDialogPane().lookupButton(buttonTypeTwo).addEventFilter(ActionEvent.ACTION, event -> {
            // 移动到该坐标
            double[] param = NumberUtils.stringToDoubleList(initPositionText.getText(), ",");
            if (param.length != 3) {
                addToLog("机械臂=>坐标参数异常", "danger");
            } else {
                try {
                    Variant result = Dispatch.call(mechanicalActiveX, "Move", param);
                    addToLog("机械臂=>移动坐标=>[" + result.getInt() + "]");
                    dialog.setHeaderText("机械臂=>移动坐标=>[" + result.getInt() + "]");
                } catch (Exception e) {
                    e.printStackTrace();
                    addToLog("机械臂=>移动坐标异常=>[" + e.getMessage() + "]");
                    dialog.setHeaderText("机械臂=>移动坐标异常=>[" + e.getMessage() + "]");
                }
            }
            event.consume();
        });
        dialog.show();
    }

    /**
     * 机械臂姿态移动
     */
    public void mechanicalAngleSetting() {
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("机械臂姿态移动");
        dialog.setHeaderText("请按照以下顺序操作\n设置中心=>获取姿态=>姿态移动");
        dialog.initStyle(StageStyle.UTILITY);
        ButtonType buttonTypeOne = new ButtonType("获取姿态");
        ButtonType buttonTypeTwo = new ButtonType("设置中心");
        ButtonType buttonTypeThree = new ButtonType("姿态移动");
        dialog.getDialogPane().getButtonTypes().addAll(buttonTypeOne, buttonTypeTwo, buttonTypeThree, ButtonType.CLOSE);

        GridPane grid = new GridPane();
        grid.setPrefWidth(400);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField mechanicalPosZText = new TextField(ini.get("mechanical", "mechanicalPosZ"));
        TextField positionXText = new TextField();
        TextField positionYText = new TextField();
        TextField positionZText = new TextField();
        TextField angleXText = new TextField();
        TextField angleYText = new TextField();
        TextField angleZText = new TextField();

        grid.add(new Label("中心点位(米):"), 0, 0);
        grid.add(mechanicalPosZText, 1, 0);

        grid.add(new Label("X坐标:"), 0, 1);
        grid.add(positionXText, 1, 1);
        grid.add(new Label("Y坐标:"), 0, 2);
        grid.add(positionYText, 1, 2);
        grid.add(new Label("Z坐标:"), 0, 3);
        grid.add(positionZText, 1, 3);

        grid.add(new Label("姿态1:"), 0, 4);
        grid.add(angleXText, 1, 4);
        grid.add(new Label("姿态2:"), 0, 5);
        grid.add(angleYText, 1, 5);
        grid.add(new Label("姿态3:"), 0, 6);
        grid.add(angleZText, 1, 6);

        dialog.getDialogPane().setContent(grid);

        dialog.getDialogPane().lookupButton(buttonTypeOne).addEventFilter(ActionEvent.ACTION, event -> {
            // 获取姿态
            try {
                Variant result = Dispatch.call(mechanicalActiveX, "GetEuler");
                //double[] resultDouble = NumberUtils.stringToDoubleList(result.getString(), ",", angleToRadian);
                double[] resultDouble = NumberUtils.stringToDoubleListNoScale(result.getString(), ",");
                String resultWithRadian = NumberUtils.listToString(resultDouble, ",");
                dialog.setHeaderText("当前姿态=>[" + resultWithRadian + "]");
                if (resultDouble.length == 3) {
                    angleXText.setText(String.valueOf(resultDouble[0]));
                    angleYText.setText(String.valueOf(resultDouble[1]));
                    angleZText.setText(String.valueOf(resultDouble[2]));
                }
                addToLog("机械臂=>获取当前姿态原始值=>[" + result.getString() + "]");
                addToLog("机械臂=>获取当前姿态角度值=>[" + resultWithRadian + "]");

                Variant getPositionResult = Dispatch.call(mechanicalActiveX, "GetPosition");
                double[] resultPositionDouble = NumberUtils.stringToDoubleListNoScale(getPositionResult.getString(), ",");
                if (resultPositionDouble.length == 3) {
                    positionXText.setText(String.valueOf(resultPositionDouble[0]));
                    positionYText.setText(String.valueOf(resultPositionDouble[1]));
                    positionZText.setText(String.valueOf(resultPositionDouble[2]));
                }
                dialog.setHeaderText(dialog.getHeaderText() + "=>坐标[" + getPositionResult.getString() + "]");
                addToLog("机械臂=>获取当前坐标成功=>[" + getPositionResult.getString() + "]");
            } catch (Exception e) {
                addToLog("机械臂=>获取当前姿态异常=>[" + e.getMessage() + "]", "danger");
            }
            event.consume();
        });
        dialog.getDialogPane().lookupButton(buttonTypeTwo).addEventFilter(ActionEvent.ACTION, event -> {
            // 设置中心点
            double[] mechanicalPosZDouble = NumberUtils.stringToDoubleList(mechanicalPosZText.getText(), ",");
            if (mechanicalPosZDouble.length != 3) {
                dialog.setHeaderText("中心点为3个数字");
            } else {
                try {
                    Variant result = Dispatch.call(mechanicalActiveX, "SetOffsetCenter", mechanicalPosZDouble);
                    if (result.getBoolean()) {
                        dialog.setHeaderText("设置中心点=>[" + mechanicalPosZText.getText() + "]=>[成功]");
                        addToLog("机械臂=>设置中心点=>[" + mechanicalPosZText.getText() + "]=>[成功]");
                    } else {
                        dialog.setHeaderText("设置中心点=>[" + mechanicalPosZText.getText() + "]=>[失败]");
                        addToLog("机械臂=>设置中心点=>[" + mechanicalPosZText.getText() + "]=>[失败]", "danger");
                    }
                } catch (Exception e) {
                    addToLog("机械臂=>获取当前姿态异常=>[" + e.getMessage() + "]", "danger");
                }
            }
            event.consume();
        });
        dialog.getDialogPane().lookupButton(buttonTypeThree).addEventFilter(ActionEvent.ACTION, event -> {
            // 姿态移动
            Double positionX = NumberUtils.stringToDouble(positionXText.getText());
            Double positionY = NumberUtils.stringToDouble(positionYText.getText());
            Double positionZ = NumberUtils.stringToDouble(positionZText.getText());
            Double angleX = NumberUtils.stringToDouble(angleXText.getText());
            Double angleY = NumberUtils.stringToDouble(angleYText.getText());
            Double angleZ = NumberUtils.stringToDouble(angleZText.getText());
            if (positionX == null || positionY == null || positionZ == null || angleX == null || angleY == null || angleZ == null) {
                dialog.setHeaderText("机械臂=>姿态移动参数需为数字");
            } else {
                double[] param = new double[]{positionX, positionY, positionZ, angleX, angleY, angleZ};
                addToLog("机械臂=>姿态移动参数=>[" + NumberUtils.listToString(param, ",") + "]");
                try {
                    Variant result = Dispatch.call(mechanicalActiveX, "MoveWithAngle", param);
                    addToLog("机械臂=>姿态移动=>[" + result.getString() + "]");
                    dialog.setHeaderText("机械臂=>姿态移动=>[" + result.getString() + "]");
                } catch (Exception e) {
                    e.printStackTrace();
                    addToLog("机械臂=>姿态移动异常=>[" + e.getMessage() + "]", "danger");
                    dialog.setHeaderText("机械臂=>姿态移动异常=>[" + e.getMessage() + "]");
                }
            }
            event.consume();
        });
        dialog.show();
    }

    /**
     * 机械臂六轴加速度设置
     */
    public void mechanicalAxisMaxSpeedSetting() {
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("机械臂六轴加速度设置");
        dialog.setHeaderText("加速度为6个逗号分隔的数字");
        dialog.initStyle(StageStyle.UTILITY);
        ButtonType buttonTypeOne = new ButtonType("获取");
        dialog.getDialogPane().getButtonTypes().addAll(buttonTypeOne, ButtonType.OK, ButtonType.CLOSE);

        GridPane grid = new GridPane();
        grid.setPrefWidth(400);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField initPositionText = new TextField();

        grid.add(new Label("六轴加速度:"), 0, 0);
        grid.add(initPositionText, 1, 0);
        dialog.getDialogPane().setContent(grid);

        dialog.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(ActionEvent.ACTION, event -> {
            String initRotation = initPositionText.getText();
            double[] initRotationDoubles = NumberUtils.stringToDoubleList(initRotation, ",");
            if (initRotationDoubles.length != 3) {
                dialog.setHeaderText("坐标需为6个逗号分隔的数字");
                event.consume();
            } else {
                /*ini.put("mechanical", "mechanicalInitPosition", initRotation);
                try {
                    ini.store();
                    dialog.setHeaderText("保存成功");
                } catch (IOException e) {
                    dialog.setHeaderText("配置文件保存失败");
                }*/
                event.consume();
            }
        });
        dialog.getDialogPane().lookupButton(buttonTypeOne).addEventFilter(ActionEvent.ACTION, event -> {
            // 获取当前加速度
            /*try {
                Variant result = Dispatch.call(mechanicalActiveX, "GetPosition");
                double[] resultDouble = NumberUtils.stringToDoubleList(result.getString(), ",", angleToRadian);
                if (resultDouble.length == 0) {
                    dialog.setHeaderText("机械臂=>获取当前坐标失败=>[" + result.getString() + "]");
                    addToLog("机械臂=>获取当前坐标失败=>[" + result.getString() + "]");
                } else {
                    String resultWithRadian = NumberUtils.listToString(resultDouble, ",");
                    dialog.setHeaderText("机械臂=>获取当前旋转角度成功=>[" + resultWithRadian + "]");
                    addToLog("机械臂=>获取当前旋转角度成功=>[" + resultWithRadian + "]");
                    initPositionText.setText(resultWithRadian);
                }
            } catch (Exception e) {
                addToLog("机械臂=>获取当前旋转角度异常=>[" + e.getMessage() + "]", "danger");
            }*/
            event.consume();
        });
        dialog.show();
    }

    /**
     * 安卓设置
     */
    public void androidSetting() {
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("安卓设置");
        dialog.setHeaderText("保存后请重启程序");
        dialog.initStyle(StageStyle.UTILITY);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CLOSE);

        GridPane grid = new GridPane();
        grid.setPrefWidth(400);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField localPortText = new TextField(ini.get("android", "localPort"));
        TextField remotePortText = new TextField(ini.get("android", "remotePort"));
        TextField remoteHostText = new TextField(ini.get("android", "remoteHost"));

        grid.add(new Label("本地端口:"), 0, 0);
        grid.add(localPortText, 1, 0);
        grid.add(new Label("远程端口:"), 0, 1);
        grid.add(remotePortText, 1, 1);
        grid.add(new Label("远程IP:"), 0, 2);
        grid.add(remoteHostText, 1, 2);
        dialog.getDialogPane().setContent(grid);

        dialog.getDialogPane().setContent(grid);

        dialog.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(ActionEvent.ACTION, event -> {
            String remoteHost = remoteHostText.getText();
            Integer localPort = NumberUtils.stringToInt(localPortText.getText());
            Integer remotePort = NumberUtils.stringToInt(remotePortText.getText());
            if (StringUtils.isEmpty(remoteHost)) {
                dialog.setHeaderText("远程IP不能为空");
                event.consume();
            } else if (null == localPort) {
                dialog.setHeaderText("本地端口为数字");
                event.consume();
            } else if (null == remotePort) {
                dialog.setHeaderText("远程端口为数字");
                event.consume();
            } else {
                ini.put("android", "localPort", localPort);
                ini.put("android", "remotePort", remotePort);
                ini.put("android", "remoteHost", remoteHost);
                try {
                    ini.store();
                    dialog.setHeaderText("保存成功");
                    loadIni();
                } catch (IOException e) {
                    dialog.setHeaderText("配置文件保存失败");
                }
                event.consume();
            }
        });
        dialog.show();
    }

    /**
     * 安卓启动应用
     */
    public void androidOpenAppSetting() {
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("安卓启动应用");
        dialog.setHeaderText("点击可以启动应用");
        ButtonType buttonTypeOne = new ButtonType("查询屏幕");
        dialog.initStyle(StageStyle.UTILITY);

        dialog.getDialogPane().getButtonTypes().addAll(buttonTypeOne, ButtonType.OK, ButtonType.CLOSE);

        GridPane grid = new GridPane();
        grid.setPrefWidth(400);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        dialog.getDialogPane().setContent(grid);

        dialog.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();
            AndroidDevice androidDevice = getLinkedAndroidDevice();
            if (null == androidDevice) {
                return;
            }
            androidDevice.start("cn.mellonrobot.faceunlock", "cn.mellonrobot.faceunlock.MainActivity");
            dialog.setHeaderText("完成");
        });
        dialog.getDialogPane().lookupButton(buttonTypeOne).addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();
            AndroidDevice androidDevice = getLinkedAndroidDevice();
            if (null == androidDevice) {
                return;
            }
            String content = androidDevice.catFile(screenFilePath);
            dialog.setHeaderText("内容:" + content);
        });
        dialog.show();
    }

    /**
     * 安卓状态
     */
    public void androidBatterySetting() {
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("安卓状态");
        dialog.setHeaderText("支持修改是否充电");
        ButtonType buttonTypeOne = new ButtonType("电池");
        ButtonType buttonTypeTwo = new ButtonType("屏幕");
        ButtonType buttonTypeThree = new ButtonType("密码解锁");
        dialog.initStyle(StageStyle.UTILITY);

        dialog.getDialogPane().getButtonTypes().addAll(buttonTypeOne, buttonTypeTwo, buttonTypeThree, ButtonType.OK, ButtonType.CLOSE);

        GridPane grid = new GridPane();
        grid.setPrefWidth(400);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        CheckBox chargeCheckbox = new CheckBox();
        chargeCheckbox.setSelected(false);

        grid.add(new Label("连线充电:"), 0, 0);
        grid.add(chargeCheckbox, 1, 0);
        dialog.getDialogPane().setContent(grid);

        dialog.getDialogPane().lookupButton(buttonTypeOne).addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();
            AndroidDevice androidDevice = getLinkedAndroidDevice();
            if (null == androidDevice) {
                return;
            }
            try {
                dialog.setHeaderText("当前电池电量:" + androidDevice.getDevice().getBattery().get());
                addToLog("安卓[" + androidDevice.getName() + "]当前电量:" + androidDevice.getDevice().getBattery().get());
            } catch (Exception e) {
                e.printStackTrace();
                addToLog("安卓[" + androidDevice.getName() + "]当前电池电量获取失败");
            }
        });
        dialog.getDialogPane().lookupButton(buttonTypeTwo).addEventFilter(ActionEvent.ACTION, event -> {
            // 屏幕
            event.consume();
            AndroidDevice androidDevice = getLinkedAndroidDevice();
            if (null == androidDevice)
                return;

            String content = androidDevice.catFile(screenFilePath);
            if (StringUtils.isEmpty(content) || content.split(",").length != 2) {
                dialog.setHeaderText("获取屏幕状态异常");
            } else {
                String[] strings = content.split(",");
                if ("1".equalsIgnoreCase(strings[0])) {
                    dialog.setHeaderText("开屏@" + TimeUtils.format(NumberUtils.stringToLong(strings[1]), "yyyy-MM-dd HH:mm:ss:SSS"));
                } else if ("2".equalsIgnoreCase(strings[0])) {
                    dialog.setHeaderText("锁屏@" + TimeUtils.format(NumberUtils.stringToLong(strings[1]), "yyyy-MM-dd HH:mm:ss:SSS"));
                } else if ("3".equalsIgnoreCase(strings[0])) {
                    dialog.setHeaderText("解锁@" + TimeUtils.format(NumberUtils.stringToLong(strings[1]), "yyyy-MM-dd HH:mm:ss:SSS"));
                }
            }
        });
        dialog.getDialogPane().lookupButton(buttonTypeThree).addEventFilter(ActionEvent.ACTION, event -> {
            // 密码解锁
            event.consume();
            AndroidDevice androidDevice = getLinkedAndroidDevice();
            if (null == androidDevice) {
                return;
            }

            int[] unlockSwipeDouble = NumberUtils.stringToIntList(ini.get("android", "unlockSwipe"), ",");
            androidDevice.inputKeyevent(224);
            androidDevice.swipe(unlockSwipeDouble[0], unlockSwipeDouble[1], unlockSwipeDouble[2], unlockSwipeDouble[3]);
            androidDevice.inputText(ini.get("android", "unlockPassword"));
            dialog.setHeaderText("指令发送完成");
        });
        dialog.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();
            AndroidDevice androidDevice = getLinkedAndroidDevice();
            if (null == androidDevice) {
                return;
            }

            String result = androidDevice.runAdbCommand("shell dumpsys battery set status " + (chargeCheckbox.isSelected() ? "2" : "1"));
            dialog.setHeaderText("设置完成:" + result);
            addToLog("安卓[" + androidDevice.getName() + "]设置插线充电状态为" + (chargeCheckbox.isSelected() ? "2" : "1"));
        });
        dialog.show();
    }

    /**
     * 安卓模拟键
     */
    public void androidKeySetting() {
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("安卓模拟键");
        dialog.setHeaderText("模拟电源键");
        dialog.initStyle(StageStyle.UTILITY);

        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CLOSE);

        GridPane grid = new GridPane();
        grid.setPrefWidth(400);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField keyText = new TextField("26");

        grid.add(new Label("按键:"), 0, 0);
        grid.add(keyText, 1, 0);

        dialog.getDialogPane().setContent(grid);

        dialog.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();
            AndroidDevice androidDevice = getLinkedAndroidDevice();
            if (null == androidDevice) {
                return;
            }

            Integer key = NumberUtils.stringToInt(keyText.getText());
            if (null == key) {
                dialog.setHeaderText("请输入按键数字");
            } else {
                androidDevice.inputKeyevent(key);
                dialog.setHeaderText("发送成功");
            }
        });
        dialog.show();
    }

    /**
     * 安卓Shell
     */
    public void androidShellSetting() {
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("安卓shell指令");
        dialog.setHeaderText("请输入shell指令");
        dialog.initStyle(StageStyle.UTILITY);
        ButtonType buttonTypeOne = new ButtonType("重启ADB");
        dialog.getDialogPane().getButtonTypes().addAll(buttonTypeOne, ButtonType.OK, ButtonType.CLOSE);

        GridPane grid = new GridPane();
        grid.setPrefWidth(400);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField shellText = new TextField("shell am start -a android.media.action.STILL_IMAGE_CAMERA");
        shellText.setPrefWidth(300);
        grid.add(new Label("指令:"), 0, 0);
        grid.add(shellText, 1, 0);

        dialog.getDialogPane().setContent(grid);

        dialog.getDialogPane().lookupButton(buttonTypeOne).addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();
            AndroidDevice androidDevice = getLinkedAndroidDevice();
            if (null == androidDevice) {
                return;
            }

            androidDevice.restartADB();
            dialog.setHeaderText("重启后请重连设备");
        });
        dialog.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();
            AndroidDevice androidDevice = getLinkedAndroidDevice();
            if (null == androidDevice) {
                return;
            }

            if (StringUtils.isEmpty(shellText.getText())) {
                dialog.setHeaderText("指令不能为空");
            } else {
                androidDevice.runAdbCommand(shellText.getText());
                dialog.setHeaderText("发送成功");
            }
        });
        dialog.show();
    }

    public void hiddenLogSetting() {
        logPanel.setVisible(!logPanel.isVisible());
    }

    /**
     * 清空项目
     */
    public void deleteReport() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("提示");
        alert.initStyle(StageStyle.UTILITY);
        alert.setHeaderText("清空后无法恢复,确定清空项目及相关记录?");
        alert.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(ActionEvent.ACTION, event -> {
            try {
                sqliteStatement.executeUpdate("UPDATE CHECKITEM SET DEL = 1 WHERE 1 = 1");
                sqliteStatement.executeUpdate("UPDATE REPORT SET DEL = 1 WHERE 1 = 1");
                loadReportList(true);
                alert.setHeaderText("清空项目与检测记录成功");
                addToLog("清空所有项目与检测记录成功");
                reportTable.setItems(FXCollections.emptyObservableList());
            } catch (SQLException e) {
                e.printStackTrace();
                addToLog("清空所有项目与检测记录异常:" + e.getMessage());
            }
            event.consume();
        });
        alert.show();
    }

    /**
     * 检测项设置
     */
    public void reportSetting() {
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("检测项设置");
        dialog.setHeaderText("保存后请重启程序");
        dialog.initStyle(StageStyle.UTILITY);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CLOSE);

        GridPane grid = new GridPane();
        grid.setPrefWidth(600);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField electricControlInitDistanceText = new TextField(ini.get("report", "electricControlInitDistance"));
        electricControlInitDistanceText.setPrefWidth(400);
        TextField distanceText = new TextField(ini.get("report", "electricControlDistance"));
        distanceText.setPrefWidth(400);
        TextField posAngleText = new TextField(ini.get("report", "posAngle"));
        posAngleText.setPrefWidth(400);
        TextField horizontalAngleText = new TextField(ini.get("report", "horizontalAngle"));
        horizontalAngleText.setPrefWidth(400);
        TextField horizontalAngleRotationPositionText = new TextField(ini.get("report", "horizontalAngleRotationPosition"));
        horizontalAngleRotationPositionText.setPrefWidth(400);
        TextField pitchAngleText = new TextField(ini.get("report", "pitchAngle"));
        pitchAngleText.setPrefWidth(400);
        TextField pitchAngleRotationPositionText = new TextField(ini.get("report", "pitchAngleRotationPosition"));
        pitchAngleRotationPositionText.setPrefWidth(400);
        TextField inclinationAngleText = new TextField(ini.get("report", "inclinationAngle"));
        inclinationAngleText.setPrefWidth(400);
        TextField inclinationAngleRotationPositionText = new TextField(ini.get("report", "inclinationAngleRotationPosition"));
        inclinationAngleRotationPositionText.setPrefWidth(400);
        TextField sleepTimeText = new TextField(ini.get("report", "sleepTime"));
        sleepTimeText.setPrefWidth(400);
        TextField moveSleepTimeText = new TextField(ini.get("report", "moveSleepTime"));
        sleepTimeText.setPrefWidth(400);
        TextField moveHorizontalAngleText = new TextField(ini.get("report", "moveHorizontalAngle"));
        moveHorizontalAngleText.setPrefWidth(400);

        grid.add(new Label("初始距离(mm):"), 0, 0);
        grid.add(electricControlInitDistanceText, 1, 0);
        grid.add(new Label("测试距离(mm):"), 0, 1);
        grid.add(distanceText, 1, 1);
        grid.add(new Label("位置角范围:"), 0, 2);
        grid.add(posAngleText, 1, 2);
        grid.add(new Label("水平角范围:"), 0, 3);
        grid.add(horizontalAngleText, 1, 3);
        grid.add(new Label("水平角位置:"), 0, 4);
        grid.add(horizontalAngleRotationPositionText, 1, 4);
        grid.add(new Label("仰俯角范围:"), 0, 5);
        grid.add(pitchAngleText, 1, 5);
        grid.add(new Label("仰俯角位置:"), 0, 6);
        grid.add(pitchAngleRotationPositionText, 1, 6);
        grid.add(new Label("倾斜角范围:"), 0, 7);
        grid.add(inclinationAngleText, 1, 7);
        grid.add(new Label("倾斜角位置:"), 0, 8);
        grid.add(inclinationAngleRotationPositionText, 1, 8);
        grid.add(new Label("停顿时间(ms):"), 0, 9);
        grid.add(sleepTimeText, 1, 9);
        grid.add(new Label("移动检测停顿时间(ms):"), 0, 10);
        grid.add(moveSleepTimeText, 1, 10);
        grid.add(new Label("移动检测水平角度范围:"), 0, 11);
        grid.add(moveHorizontalAngleText, 1, 11);

        dialog.getDialogPane().setContent(grid);

        dialog.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(ActionEvent.ACTION, event -> {
            Integer electricControlInitDistance = NumberUtils.stringToInt(electricControlInitDistanceText.getText());
            int[] distance = NumberUtils.stringToIntList(distanceText.getText(), ",");
            int[] horizontalAngle = NumberUtils.stringToIntList(horizontalAngleText.getText(), ",");
            Integer horizontalAngleRotationPosition = NumberUtils.stringToInt(horizontalAngleRotationPositionText.getText());
            int[] pitchAngle = NumberUtils.stringToIntList(pitchAngleText.getText(), ",");
            Integer pitchAngleRotationPosition = NumberUtils.stringToInt(pitchAngleRotationPositionText.getText());
            int[] inclinationAngle = NumberUtils.stringToIntList(inclinationAngleText.getText(), ",");
            Integer inclinationAngleRotationPosition = NumberUtils.stringToInt(inclinationAngleRotationPositionText.getText());
            Integer sleepTime = NumberUtils.stringToInt(sleepTimeText.getText());
            // 移动检测参数
            Integer moveSleepTime = NumberUtils.stringToInt(moveSleepTimeText.getText());
            int[] moveHorizontalAngle = NumberUtils.stringToIntList(moveHorizontalAngleText.getText(), ",");
            if (electricControlInitDistance == null) {
                dialog.setHeaderText("初始距离为数字");
                event.consume();
            } else if (distance.length == 0) {
                dialog.setHeaderText("距离为逗号分隔的整数");
                event.consume();
            } else if (horizontalAngle.length == 0) {
                dialog.setHeaderText("水平角范围为逗号分隔的整数");
                event.consume();
            } else if (moveHorizontalAngle.length == 0) {
                dialog.setHeaderText("移动检测水平角范围为逗号分隔的整数");
                event.consume();
            } else if (horizontalAngleRotationPosition == null) {
                dialog.setHeaderText("水平角位置为1-6个关节位置");
                event.consume();
            } else if (pitchAngle.length == 0) {
                dialog.setHeaderText("仰俯角范围为逗号分隔的整数");
                event.consume();
            } else if (pitchAngleRotationPosition == null) {
                dialog.setHeaderText("仰俯角位置为1-6个关节位置");
                event.consume();
            } else if (inclinationAngle.length == 0) {
                dialog.setHeaderText("倾斜角范围为逗号分隔的整数");
                event.consume();
            } else if (inclinationAngleRotationPosition == null) {
                dialog.setHeaderText("倾斜角位置为1-6个关节位置");
                event.consume();
            } else if (sleepTime == null) {
                dialog.setHeaderText("停顿时间为数字");
                event.consume();
            } else {
                ini.put("report", "electricControlInitDistance", electricControlInitDistanceText.getText());
                ini.put("report", "electricControlDistance", distanceText.getText());
                ini.put("report", "horizontalAngle", horizontalAngleText.getText());
                ini.put("report", "horizontalAngleRotationPosition", horizontalAngleRotationPositionText.getText());
                ini.put("report", "pitchAngle", pitchAngleText.getText());
                ini.put("report", "pitchAngleRotationPosition", pitchAngleRotationPositionText.getText());
                ini.put("report", "inclinationAngle", inclinationAngleText.getText());
                ini.put("report", "inclinationAngleRotationPosition", inclinationAngleRotationPositionText.getText());
                ini.put("report", "sleepTime", sleepTimeText.getText());
                ini.put("report", "moveSleepTime", moveSleepTimeText.getText());
                ini.put("report", "moveHorizontalAngle", moveHorizontalAngleText.getText());
                try {
                    ini.store();
                    dialog.setHeaderText("保存成功");
                    loadIni();
                } catch (IOException e) {
                    dialog.setHeaderText("配置文件保存失败");
                }
                event.consume();
            }
        });
        dialog.show();
    }

    /**
     * 检测项设置
     */
    public void reportSetting2() {
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("检测项设置");
        dialog.setHeaderText("保存后请重启程序");
        dialog.initStyle(StageStyle.UTILITY);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CLOSE);

        GridPane grid = new GridPane();
        grid.setPrefWidth(600);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField distanceText = new TextField(ini.get("report", "electricControlDistance"));
        distanceText.setPrefWidth(400);
        TextField horizontalAngleText = new TextField(ini.get("report", "horizontalAngle"));
        horizontalAngleText.setPrefWidth(400);
        TextField horizontalAngleRotationPositionText = new TextField(ini.get("report", "horizontalAngleRotationPosition"));
        horizontalAngleRotationPositionText.setPrefWidth(400);
        TextField pitchAngleText = new TextField(ini.get("report", "pitchAngle"));
        pitchAngleText.setPrefWidth(400);
        TextField pitchAngleRotationPositionText = new TextField(ini.get("report", "pitchAngleRotationPosition"));
        pitchAngleRotationPositionText.setPrefWidth(400);
        TextField inclinationAngleText = new TextField(ini.get("report", "inclinationAngle"));
        inclinationAngleText.setPrefWidth(400);
        TextField inclinationAngleRotationPositionText = new TextField(ini.get("report", "inclinationAngleRotationPosition"));
        inclinationAngleRotationPositionText.setPrefWidth(400);
        TextField sleepTimeText = new TextField(ini.get("report", "sleepTime"));
        sleepTimeText.setPrefWidth(400);
        TextField moveSleepTimeText = new TextField(ini.get("report", "moveSleepTime"));
        sleepTimeText.setPrefWidth(400);
        TextField moveHorizontalAngleText = new TextField(ini.get("report", "moveHorizontalAngle"));
        moveHorizontalAngleText.setPrefWidth(400);

        grid.add(new Label("距离范围(mm):"), 0, 0);
        grid.add(distanceText, 1, 0);
        grid.add(new Label("水平角范围:"), 0, 1);
        grid.add(horizontalAngleText, 1, 1);
        grid.add(new Label("水平角位置:"), 0, 2);
        grid.add(horizontalAngleRotationPositionText, 1, 2);
        grid.add(new Label("仰俯角范围:"), 0, 3);
        grid.add(pitchAngleText, 1, 3);
        grid.add(new Label("仰俯角位置:"), 0, 4);
        grid.add(pitchAngleRotationPositionText, 1, 4);
        grid.add(new Label("倾斜角范围:"), 0, 5);
        grid.add(inclinationAngleText, 1, 5);
        grid.add(new Label("倾斜角位置:"), 0, 6);
        grid.add(inclinationAngleRotationPositionText, 1, 6);
        grid.add(new Label("停顿时间(ms):"), 0, 7);
        grid.add(sleepTimeText, 1, 7);
        grid.add(new Label("移动检测停顿时间(ms):"), 0, 8);
        grid.add(moveSleepTimeText, 1, 8);
        grid.add(new Label("移动检测水平角度范围:"), 0, 9);
        grid.add(moveHorizontalAngleText, 1, 9);

        dialog.getDialogPane().setContent(grid);

        dialog.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(ActionEvent.ACTION, event -> {
            int[] distance = NumberUtils.stringToIntList(distanceText.getText(), ",");
            int[] horizontalAngle = NumberUtils.stringToIntList(horizontalAngleText.getText(), ",");
            Integer horizontalAngleRotationPosition = NumberUtils.stringToInt(horizontalAngleRotationPositionText.getText());
            int[] pitchAngle = NumberUtils.stringToIntList(pitchAngleText.getText(), ",");
            Integer pitchAngleRotationPosition = NumberUtils.stringToInt(pitchAngleRotationPositionText.getText());
            int[] inclinationAngle = NumberUtils.stringToIntList(inclinationAngleText.getText(), ",");
            Integer inclinationAngleRotationPosition = NumberUtils.stringToInt(inclinationAngleRotationPositionText.getText());
            Integer sleepTime = NumberUtils.stringToInt(sleepTimeText.getText());
            // 移动检测参数
            Integer moveSleepTime = NumberUtils.stringToInt(moveSleepTimeText.getText());
            int[] moveHorizontalAngle = NumberUtils.stringToIntList(moveHorizontalAngleText.getText(), ",");
            if (distance.length == 0) {
                dialog.setHeaderText("距离为逗号分隔的整数");
                event.consume();
            } else if (horizontalAngle.length == 0) {
                dialog.setHeaderText("水平角范围为逗号分隔的整数");
                event.consume();
            } else if (moveHorizontalAngle.length == 0) {
                dialog.setHeaderText("移动检测水平角范围为逗号分隔的整数");
                event.consume();
            } else if (horizontalAngleRotationPosition == null) {
                dialog.setHeaderText("水平角位置为1-6个关节位置");
                event.consume();
            } else if (pitchAngle.length == 0) {
                dialog.setHeaderText("仰俯角范围为逗号分隔的整数");
                event.consume();
            } else if (pitchAngleRotationPosition == null) {
                dialog.setHeaderText("仰俯角位置为1-6个关节位置");
                event.consume();
            } else if (inclinationAngle.length == 0) {
                dialog.setHeaderText("倾斜角范围为逗号分隔的整数");
                event.consume();
            } else if (inclinationAngleRotationPosition == null) {
                dialog.setHeaderText("倾斜角位置为1-6个关节位置");
                event.consume();
            } else if (sleepTime == null) {
                dialog.setHeaderText("停顿时间为数字");
                event.consume();
            } else {
                ini.put("report", "distance", distanceText.getText());
                ini.put("report", "horizontalAngle", horizontalAngleText.getText());
                ini.put("report", "horizontalAngleRotationPosition", horizontalAngleRotationPositionText.getText());
                ini.put("report", "pitchAngle", pitchAngleText.getText());
                ini.put("report", "pitchAngleRotationPosition", pitchAngleRotationPositionText.getText());
                ini.put("report", "inclinationAngle", inclinationAngleText.getText());
                ini.put("report", "inclinationAngleRotationPosition", inclinationAngleRotationPositionText.getText());
                ini.put("report", "sleepTime", sleepTimeText.getText());
                ini.put("report", "moveSleepTime", moveSleepTimeText.getText());
                ini.put("report", "moveHorizontalAngle", moveHorizontalAngleText.getText());
                try {
                    ini.store();
                    dialog.setHeaderText("保存成功");
                    loadIni();
                } catch (IOException e) {
                    dialog.setHeaderText("配置文件保存失败");
                }
                event.consume();
            }
        });
        dialog.show();
    }

    /**
     * 导轨设置
     */
    public void electricControlSetting() {
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("导轨设置");
        dialog.setHeaderText("保存后请重启程序");
        dialog.initStyle(StageStyle.UTILITY);
        //ButtonType buttonTypeOne = new ButtonType("设置波特率/串口名");
        ButtonType buttonTypeTwo = new ButtonType("读取串口");
        dialog.getDialogPane().getButtonTypes().addAll(buttonTypeTwo, ButtonType.OK, ButtonType.CLOSE);

        GridPane grid = new GridPane();
        grid.setPrefWidth(400);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField electricControlButoText = new TextField(ini.get("electricControl", "electricControlButo"));
        //TextField electricControlComText = new TextField(ini.get("electricControl", "electricControlCom"));
        TextField electricControlPulsesToLengthText = new TextField(ini.get("electricControl", "electricControlPulsesToLength"));
        TextField electricControlIdText = new TextField(ini.get("electricControl", "electricControlId"));
        TextField electricControlNotComText = new TextField(ini.get("electricControl", "electricControlNotCom"));

        Label electricControlNotComLabel = new Label("屏蔽串口:");
        electricControlNotComLabel.setPrefWidth(150);
        grid.add(electricControlNotComLabel, 0, 0);
        grid.add(electricControlNotComText, 1, 0);
        dialog.getDialogPane().setContent(grid);
        Label electricControlButoLabel = new Label("波特率:");
        electricControlButoLabel.setPrefWidth(150);
        grid.add(electricControlButoLabel, 0, 1);
        grid.add(electricControlButoText, 1, 1);
        /*Label electricControlComLabel = new Label("串口名:");
        electricControlComLabel.setPrefWidth(150);
        grid.add(electricControlComLabel, 0, 1);
        grid.add(electricControlComText, 1, 1);*/
        Label electricControlIdLabel = new Label("设备ID:");
        electricControlIdLabel.setPrefWidth(150);
        grid.add(electricControlIdLabel, 0, 2);
        grid.add(electricControlIdText, 1, 2);
        Label electricControlPulsesToLengthLabel = new Label("脉冲距离比:");
        electricControlPulsesToLengthLabel.setPrefWidth(150);
        grid.add(electricControlPulsesToLengthLabel, 0, 3);
        grid.add(electricControlPulsesToLengthText, 1, 3);
        dialog.getDialogPane().setContent(grid);

        dialog.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(ActionEvent.ACTION, event -> {
            String electricControlButo = electricControlButoText.getText();
            //String electricControlCom = electricControlComText.getText();
            Integer electricControlId = NumberUtils.stringToInt(electricControlIdText.getText());
            Double electricControlPulsesToLength = NumberUtils.stringToDouble(electricControlPulsesToLengthText.getText());
            if (StringUtils.isEmpty(electricControlButo)) {
                dialog.setHeaderText("波特率不能为空");
                event.consume();
            } else if (null == electricControlId) {
                dialog.setHeaderText("设备ID为数字");
                event.consume();
            } else if (null == electricControlPulsesToLength) {
                dialog.setHeaderText("脉冲距离比例为数字");
                event.consume();
            } else {
                ini.put("electricControl", "electricControlButo", electricControlButo);
                ini.put("electricControl", "electricControlId", electricControlId);
                ini.put("electricControl", "electricControlPulsesToLength", electricControlPulsesToLength);
                ini.put("electricControl", "electricControlNotCom", electricControlNotComText.getText());
                try {
                    ini.store();
                    dialog.setHeaderText("保存成功");
                    loadIni();
                } catch (IOException e) {
                    dialog.setHeaderText("配置文件保存失败");
                }
                event.consume();
            }
        });
        dialog.getDialogPane().lookupButton(buttonTypeTwo).addEventFilter(ActionEvent.ACTION, event -> {
            // 读取串口
            String[] ports = SerialPortList.getPortNames();
            String postsString = NumberUtils.listToString(ports, ",");
            dialog.setHeaderText("连接串口[" + postsString + "]");
            event.consume();
        });
        dialog.show();
    }

    /**
     * 导轨移动设置
     */
    public void electricControlRotationSetting() {
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("导轨移动");
        dialog.setHeaderText("按脉冲数移动");
        dialog.initStyle(StageStyle.UTILITY);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CLOSE);

        GridPane grid = new GridPane();
        grid.setPrefWidth(400);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        ChoiceBox<String> directCheckBox = new ChoiceBox<>();
        directCheckBox.getItems().add("正转");
        directCheckBox.getItems().add("反转");
        directCheckBox.setValue("正转");
        TextField numberOfPulsesText = new TextField("1000");

        grid.add(new Label("方向:"), 0, 0);
        grid.add(directCheckBox, 1, 0);
        grid.add(new Label("脉冲数:"), 0, 1);
        grid.add(numberOfPulsesText, 1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();
            Integer numberOfPulses = NumberUtils.stringToInt(numberOfPulsesText.getText());
            if (numberOfPulses == null) {
                addToLog("脉冲数需为整数数字", "danger");
            } else {
                try {
                    Variant result = Dispatch.call(electricControlActiveX, "SetNumberOfPulses", electricControlId, numberOfPulses);
                    if (result.getBoolean()) {
                        addToLog("导轨=>设置脉冲[" + numberOfPulses + "]=>[成功]", "success");
                        // 移动
                        Thread.sleep(500);
                        String direct = directCheckBox.getValue();
                        Variant resultRotation = Dispatch.call(electricControlActiveX, ("正转".equalsIgnoreCase(direct) ? "ForwardRotation" : "ReversalRotation"), electricControlId);
                        addToLog("导轨=>" + direct + "=>[" + (resultRotation.getBoolean() ? "成功" : "失败") + "]");
                    } else {
                        addToLog("导轨=>设置脉冲[" + numberOfPulses + "]=>[失败]", "danger");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    addToLog("导轨=>移动异常=>[" + e.getMessage() + "]", "danger");
                }
            }
        });
        dialog.show();
    }

    /**
     * 导轨速度设置
     */
    public void electricControlSpeedSetting() {
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("导轨速度设置");
        dialog.setHeaderText("");
        dialog.initStyle(StageStyle.UTILITY);
        ButtonType buttonTypeOne = new ButtonType("获取");
        dialog.getDialogPane().getButtonTypes().addAll(buttonTypeOne, ButtonType.OK, ButtonType.CLOSE);

        GridPane grid = new GridPane();
        grid.setPrefWidth(400);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField speedText = new TextField();

        grid.add(new Label("速度:"), 0, 0);
        grid.add(speedText, 1, 0);

        dialog.getDialogPane().setContent(grid);

        dialog.getDialogPane().lookupButton(buttonTypeOne).addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();
            // 查询当前速度
            try {
                Variant result = Dispatch.call(electricControlActiveX, "CheckSpeed", electricControlId);
                addToLog("导轨=>速度=>[" + result.getString() + "]");
                dialog.setHeaderText("导轨=>速度=>[" + result.getString() + "]");
                speedText.setText(result.getString());
            } catch (Exception e) {
                e.printStackTrace();
                addToLog("导轨=>查询速度异常=>[" + e.getMessage() + "]", "danger");
            }
        });
        dialog.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();
            Integer electricControlSpeedValue = NumberUtils.stringToInt(speedText.getText());
            if (electricControlSpeedValue == null) {
                dialog.setHeaderText("速度需为数字");
            } else {
                try {
                    Variant result = Dispatch.call(electricControlActiveX, "SetSpeed", electricControlId, electricControlSpeedValue);
                    addToLog("导轨=>设置速度[" + electricControlSpeedValue + "]=>[" + (result.getBoolean() ? "成功" : "失败") + "]");
                } catch (Exception e) {
                    addToLog("导轨=>移动异常=>[" + e.getMessage() + "]", "danger");
                }
            }
        });
        dialog.show();
    }

    private void addToLog(String content, String type) {
        Platform.runLater(() -> {
            if (logTextArea.getText().split("\\n").length > 200) {
                logTextArea.setText(TimeUtils.now() + "@" + content + "\n");
            } else {
                logTextArea.setText(logTextArea.getText() + TimeUtils.now() + "@" + content + "\n");
            }
            logTextArea.setScrollTop(logTextArea.getMaxHeight());
            logMsg.setText(TimeUtils.now() + "@" + content);
            logMsgFlow.getStyleClass().remove("alert-success");
            logMsgFlow.getStyleClass().remove("alert-danger");
            logMsgFlow.getStyleClass().remove("alert-default");
            logMsgFlow.getStyleClass().add("alert-" + type);
        });
    }

    /**
     * 将文本加入日志框
     */
    private void addToLog(String content) {
        addToLog(content, "default");
    }

    /**
     * 清空记录
     */
    public void clearLogText() {
        logTextArea.setText("");
    }

    /**
     * 保存记录
     */
    public void saveLogText() {
        saveToTextFile(logTextArea.getText(), "记录_" + TimeUtils.todaySimple());
    }

    /**
     * 从数据加载报告
     */
    private List<ReportEntity> queryReportEntityListFromDb() {
        List<ReportEntity> checkItemList = new ArrayList<>();
        try {
            ResultSet rs = sqliteStatement.executeQuery("SELECT * FROM REPORT WHERE DEL = 0 ORDER BY TIME DESC");
            while (rs.next()) {
                ReportEntity item = new ReportEntity();
                item.setID(rs.getLong("ID"));
                item.setNAME(rs.getString("NAME"));
                item.setSTARTTIME(rs.getString("STARTTIME"));
                item.setENDTIME(rs.getString("ENDTIME"));
                item.setTIME(rs.getString("TIME"));
                item.setSTATUS(rs.getInt("STATUS"));
                checkItemList.add(item);
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
            addToLog("查询数据库=>报告列表=>异常=>" + e.getMessage(), "danger");
        }
        return checkItemList;
    }

    /**
     * 从数据加载检测项列表
     */
    private List<CheckItemEntity> queryCheckItemListFromDb(String idAsc) {
        List<CheckItemEntity> checkItemList = new ArrayList<>();
        if (report != null) {
            try {
                ResultSet rs = sqliteStatement.executeQuery("SELECT * FROM CHECKITEM WHERE DEL = 0 AND REPORT_ID = " + report.getID() + " ORDER BY ID " + idAsc);
                long idx = 1;
                while (rs.next()) {
                    CheckItemEntity item = new CheckItemEntity();
                    item.setID(String.valueOf(idx));
                    item.setTYPE(rs.getString("TYPE"));
                    item.setNAME(rs.getString("NAME"));
                    item.setPOS(rs.getString("POS"));
                    item.setDISTANCE(rs.getString("DISTANCE"));
                    item.setHORIZONTAL(rs.getString("HORIZONTAL"));
                    item.setPITCH(rs.getString("PITCH"));
                    item.setINCLINATION(rs.getString("INCLINATION"));
                    item.setRESULT(rs.getString("RESULT"));
                    item.setTIME(rs.getString("TIME"));
                    item.setROTATION(rs.getString("ROTATION"));
                    item.setPOSITION(rs.getString("POSITION"));
                    checkItemList.add(item);
                    idx++;
                }
                rs.close();
            } catch (SQLException e) {
                e.printStackTrace();
                addToLog("查询检测项=>数据库异常=>" + e.getMessage(), "danger");
            }
        }
        return checkItemList;
    }

    /**
     * 清空报告
     */
    public void clearReport() {
        if (report == null) {
            showDialog(Alert.AlertType.WARNING, "请先选择执行项目");
            return;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("提示");
        alert.initStyle(StageStyle.UTILITY);
        alert.setHeaderText("删除后无法恢复,确定删除该报告下所有检测记录?");
        alert.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(ActionEvent.ACTION, event -> {
            try {
                int rs = sqliteStatement.executeUpdate("UPDATE CHECKITEM SET DEL = 1 WHERE REPORT_ID = " + report.getID());
                alert.setHeaderText("清空项目检测记录成功");
                addToLog("清空项目[" + report.getNAME() + "]检测记录成功");
                reportTable.setItems(FXCollections.emptyObservableList());
            } catch (SQLException e) {
                e.printStackTrace();
                addToLog("清空项目[" + report.getNAME() + "]检测记录异常:" + e.getMessage());
            }
            event.consume();
        });
        alert.show();
    }

    public void exportReport() {
        if (report == null) {
            showDialog(Alert.AlertType.WARNING, "请先选择执行项目");
            return;
        }
        List<CheckItemEntity> checkItemList = queryCheckItemListFromDb("ASC");
        if (checkItemList.size() == 0) {
            showDialog(Alert.AlertType.INFORMATION, "未找到检测结果");
        } else {
            // excel导出参数
            ExportParams exportParams = new ExportParams();
            exportParams.setTitle(report.getNAME());
            exportParams.setSecondTitle("生成时间:" + TimeUtils.now());
            // exportParams.setAddIndex(true);
            Workbook workbook = ExcelExportUtil.exportExcel(exportParams, CheckItemEntity.class, checkItemList);
            saveToExcelFile(workbook, report.getNAME() + "_" + TimeUtils.todaySimple());
        }
    }

    /**
     * 添加报告
     */
    public void reportAdd() {
        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("添加项目");
        dialog.setHeaderText("请输入项目名称");
        dialog.initStyle(StageStyle.UTILITY);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CLOSE);

        GridPane grid = new GridPane();
        grid.setPrefWidth(400);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField nameText = new TextField();

        grid.add(new Label("项目名称:"), 0, 0);
        grid.add(nameText, 1, 0);
        dialog.getDialogPane().setContent(grid);

        dialog.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(ActionEvent.ACTION, event -> {
            String name = nameText.getText().trim();
            if (StringUtils.isEmpty(name)) {
                dialog.setHeaderText("名称不能为空");
                event.consume();
            } else {
                String saveResult = saveReport(name, TimeUtils.now());
                if ("ok".equalsIgnoreCase(saveResult)) {
                    dialog.setHeaderText("保存成功");
                    loadReportList(true);
                    addToLog("保存项目[" + name + "]成功");
                } else {
                    dialog.setHeaderText(saveResult);
                    addToLog("保存项目[" + name + "]失败:" + saveResult, "danger");
                }
                event.consume();
            }
        });
        dialog.show();
    }

    /**
     * 跳转关于
     */
    public void goAbout() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("关于");
        alert.setHeaderText("当前版本: v " + ini.get("App", "version", String.class));
        alert.setContentText("下载地址: " + ini.get("App", "downloadUrl", String.class));
        alert.getButtonTypes().setAll(new ButtonType("复制", ButtonBar.ButtonData.OK_DONE), new ButtonType("关闭", ButtonBar.ButtonData.CANCEL_CLOSE));
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get().getButtonData() == ButtonBar.ButtonData.OK_DONE) {
            Clipboard clip = Toolkit.getDefaultToolkit().getSystemClipboard();
            Transferable tText = new StringSelection(ini.get("App", "downloadUrl", String.class));
            clip.setContents(tText, null);
        }
    }

    // [+] report
    // 固定位检测进程
    private Thread reportThread;
    // 移动检测进程
    private Thread reportMoveThread;

    /**
     * 开始检测
     * 4个循环: 循环移动导轨=>循环水平角=>循环仰俯角=>循环倾斜角
     */
    public void reportStartAngle() {
        // 是否连接检测设备
        boolean linkAndroid = "连检测设备".equalsIgnoreCase(reportDeviceChoice.getValue());
        if (report == null) {
            showDialog(Alert.AlertType.WARNING, "请选择执行项目");
            return;
        }
        AndroidDevice androidDevice = getLinkedAndroidDevice();
        if (linkAndroid && null == androidDevice) {
            showDialog(Alert.AlertType.WARNING, "检测安卓设备未连接");
            return;
        }
       /* if (!mechanicalLinked) {
            showDialog(Alert.AlertType.WARNING, "机械臂未连接");
            return;
        }*/
        if (reportThread != null && reportThread.isAlive()) {
            reportThread.interrupt();
            addToLog("固定位检测执行中,停止执行", "info");
            return;
        }
        reportThread = new Thread(() -> {
            try {
                if (linkAndroid) {
                    // 开始之前先启动应用然后锁屏
                    androidDevice.start("cn.mellonrobot.faceunlock", "cn.mellonrobot.faceunlock.MainActivity");
                    androidDevice.inputKeyevent(223);
                }
                for (int i = 0; i < reportElectricControlDistance.length; i++) {
                    // 距离循环
                    final int distance = reportElectricControlDistance[i];
                    // 移动距离
                    int distanceMove = (i == 0 ? reportElectricControlDistance[i] - reportElectricControlInitDistance : reportElectricControlDistance[i] - reportElectricControlDistance[i - 1]);
                    // 默认在第一个位置,从第二个开始移动
                    if (distanceMove == 0) {
                        // 有可能是0的
                        addToLog("导轨=>位于要求位置=>无需移动");
                    } else {
                        Platform.runLater(() -> {
                            try {
                                Variant result = Dispatch.call(electricControlActiveX, "SetNumberOfPulses", electricControlId, Math.abs(distanceMove) * electricControlPulsesToLength);
                                if (result.getBoolean()) {
                                    addToLog("导轨=>设置距离[" + distanceMove + "mm]=>[成功]", "success");
                                    // 移动
                                    String moveDirect = reportElectricControlDirect;
                                    if (distanceMove < 0) {
                                        // 如果是负数，再反转一下
                                        if ("Forward".equalsIgnoreCase(reportElectricControlDirect)) {
                                            moveDirect = "Reversal";
                                        } else if ("Reversal".equalsIgnoreCase(reportElectricControlDirect)) {
                                            moveDirect = "Forward";
                                        }
                                    }
                                    Thread.sleep(500);
                                    Variant resultRotation = Dispatch.call(electricControlActiveX, moveDirect + "Rotation", electricControlId);
                                    addToLog("导轨=>" + moveDirect + "=>[" + (resultRotation.getBoolean() ? "成功" : "失败") + "]");
                                } else {
                                    addToLog("导轨=>设置距离[" + distanceMove + "mm]=>[失败]", "danger");
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                addToLog("导轨=>移动异常=>[" + e.getMessage() + "]", "danger");
                            }
                        });
                        Thread.sleep(10000);
                    }
                    // 开始姿势移动
                    Variant setOffsetCenterResult = Dispatch.call(mechanicalActiveX, "SetOffsetCenter", mechanicalPosZ);
                    addToLog("机械臂=>设置中心点=>[" + NumberUtils.listToString(mechanicalPosZ, ",") + "]=>[" + setOffsetCenterResult.getBoolean() + "]");
                    // 获得当前的姿态
                    Variant getEuleResult = Dispatch.call(mechanicalActiveX, "GetEuler");
                    double[] getEuleResultDouble = NumberUtils.stringToDoubleListNoScale(getEuleResult.getString(), ",");
                    addToLog("机械臂=>获取当前姿态=>[" + getEuleResult.getString() + "]");
                    if (getEuleResultDouble.length != 3) {
                        reportThread.interrupt();
                        addToLog("机械臂旋=>获取当前姿态=>出错,停止执行", "danger");
                    }
                    // 获得当前的坐标
                    Variant getPositionResult = Dispatch.call(mechanicalActiveX, "GetPosition");
                    double[] getPositionResultDouble = NumberUtils.stringToDoubleListNoScale(getPositionResult.getString(), ",");
                    addToLog("机械臂=>获取当前坐标=>[" + getPositionResult.getString() + "]");
                    if (getPositionResultDouble.length != 3) {
                        reportThread.interrupt();
                        addToLog("机械臂旋=>获取当前坐标=>出错,停止执行", "danger");
                    }
                    double[] param = new double[]{getPositionResultDouble[0], getPositionResultDouble[1], getPositionResultDouble[2], getEuleResultDouble[0], getEuleResultDouble[1], getEuleResultDouble[2]};
                    // 姿态1 倾斜角 2 俯仰角 3 水平角
                    for (double horizontalAngleItem : horizontalAngle) {
                        // 水平角
                        for (double pitchAngleItem : pitchAngle) {
                            // 仰俯角
                            for (double inclinationAngleItem : inclinationAngle) {
                                Variant result = Dispatch.call(mechanicalActiveX, "MoveWithAngle", NumberUtils.paramAddAngle(param, horizontalAngleRotationPosition, horizontalAngleItem / angleToRadian,
                                        pitchAngleRotationPosition, pitchAngleItem / angleToRadian,
                                        inclinationAngleRotationPosition, inclinationAngleItem / angleToRadian));
                                Platform.runLater(() -> {
                                    if (result.getString().contains("N")) {
                                        reportThread.interrupt();
                                        addToLog("机械臂旋=>姿态移动=>出错[" + result.getString() + "],停止执行", "danger");
                                    }
                                    addToLog("机械臂=>姿态移动到水平角[" + horizontalAngleItem + "]/仰俯角[" + pitchAngleItem + "]/倾斜角[" + inclinationAngleItem + "]=>[" + result.getString() + "]");
                                    // 位置移动到了,锁屏状态下，点亮屏幕
                                    if (linkAndroid) {
                                        androidDevice.inputKeyevent(224);
                                    }
                                });
                                Thread.sleep(sleepTime);
                                // 等待10s,等待拿到检测结果
                                Platform.runLater(() -> {
                                    // 保存记录
                                    CheckItemEntity checkItem = new CheckItemEntity();
                                    checkItem.setTYPE("FIXED");
                                    checkItem.setREPORT_ID(String.valueOf(report.getID()));
                                    checkItem.setTIME(TimeUtils.nowFull());
                                    checkItem.setDISTANCE(String.valueOf(distance));
                                    checkItem.setHORIZONTAL(String.valueOf(horizontalAngleItem));
                                    checkItem.setPITCH(String.valueOf(pitchAngleItem));
                                    checkItem.setINCLINATION(String.valueOf(inclinationAngleItem));
                                    if (linkAndroid) {
                                        // 获取电池和屏幕状态
                                        try {
                                            int battery = androidDevice.getDevice().getBattery().get();
                                            checkItem.setBATTERY(String.valueOf(battery));
                                        } catch (Exception e) {
                                            checkItem.setBATTERY("NA");
                                        }
                                        try {
                                            String screenStatus = androidDevice.catFile(screenFilePath);
                                            checkItem.setRESULT(screenStatus.startsWith("3") ? "Y" : "N");
                                        } catch (Exception e) {
                                            checkItem.setRESULT("NA");
                                        }
                                    } else {
                                        checkItem.setBATTERY("NA");
                                        checkItem.setRESULT("NA");
                                    }
                                    checkItem.setROTATION(NumberUtils.listToString(param, ","));
                                    checkItem.setPOS("0");
                                    saveCheckItemRecord(checkItem);
                                    // 等待解锁和亮屏幕，关闭屏幕
                                    if (linkAndroid) {
                                        androidDevice.inputKeyevent(223);
                                    }
                                });
                                Thread.sleep(1000);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // 被interrupt后Thread.sleep会抛异常,通过异常来终端thread
                e.printStackTrace();
                addToLog("执行=>出错,停止执行:" + e.getMessage(), "danger");
            } finally {
                trigViewsForReportThread(false);
            }
        });
        trigViewsForReportThread(true);
        reportThread.start();
    }

    /**
     * 开始移动检测检测
     * 4个循环: 循环移动导轨=>循环水平角=>循环仰俯角=>循环倾斜角
     */
    public void reportMoveStartAngle() {
        boolean linkAndroid = "连检测设备".equalsIgnoreCase(reportDeviceChoice.getValue());
        if (report == null) {
            showDialog(Alert.AlertType.WARNING, "请选择执行项目");
            return;
        }
        AndroidDevice androidDevice = getLinkedAndroidDevice();
        if (linkAndroid && null == androidDevice) {
            showDialog(Alert.AlertType.WARNING, "检测安卓设备未连接");
            return;
        }
       /* if (!mechanicalLinked) {
            showDialog(Alert.AlertType.WARNING, "机械臂未连接");
            return;
        }*/
        if (reportMoveThread != null && reportMoveThread.isAlive()) {
            reportMoveThread.interrupt();
            addToLog("移动检测执行中,停止执行", "info");
            return;
        }
        reportMoveThread = new Thread(() -> {
            try {
                if (linkAndroid) {
                    // 开始之前先启动应用然后锁屏
                    androidDevice.start("cn.mellonrobot.faceunlock", "cn.mellonrobot.faceunlock.MainActivity");
                    androidDevice.inputKeyevent(223);
                }
                for (int i = 0; i < reportElectricControlDistance.length; i++) {
                    // 距离循环
                    final int distance = reportElectricControlDistance[i];
                    // 移动距离
                    int distanceMove = (i == 0 ? reportElectricControlDistance[i] - reportElectricControlInitDistance : reportElectricControlDistance[i] - reportElectricControlDistance[i - 1]);
                    // 默认在第一个位置,从第二个开始移动
                    if (distanceMove == 0) {
                        // 有可能是0的
                        addToLog("导轨=>位于要求位置=>无需移动");
                    } else {
                        Platform.runLater(() -> {
                            try {
                                Variant result = Dispatch.call(electricControlActiveX, "SetNumberOfPulses", electricControlId, Math.abs(distanceMove) * electricControlPulsesToLength);
                                if (result.getBoolean()) {
                                    addToLog("导轨=>设置距离[" + distanceMove + "mm]=>[成功]", "success");
                                    // 移动
                                    String moveDirect = reportElectricControlDirect;
                                    if (distanceMove < 0) {
                                        // 如果是负数，再反转一下
                                        if ("Forward".equalsIgnoreCase(reportElectricControlDirect)) {
                                            moveDirect = "Reversal";
                                        } else if ("Reversal".equalsIgnoreCase(reportElectricControlDirect)) {
                                            moveDirect = "Forward";
                                        }
                                    }
                                    Thread.sleep(500);
                                    Variant resultRotation = Dispatch.call(electricControlActiveX, moveDirect + "Rotation", electricControlId);
                                    addToLog("导轨=>" + moveDirect + "=>[" + (resultRotation.getBoolean() ? "成功" : "失败") + "]");
                                } else {
                                    addToLog("导轨=>设置距离[" + distanceMove + "mm]=>[失败]", "danger");
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                addToLog("导轨=>移动异常=>[" + e.getMessage() + "]", "danger");
                            }
                        });
                        Thread.sleep(10000);
                    }
                    // 开始姿势移动
                    Variant setOffsetCenterResult = Dispatch.call(mechanicalActiveX, "SetOffsetCenter", mechanicalPosZ);
                    addToLog("机械臂=>设置中心点=>[" + NumberUtils.listToString(mechanicalPosZ, ",") + "]=>[" + setOffsetCenterResult.getBoolean() + "]");
                    // 获得当前的姿态
                    Variant getEuleResult = Dispatch.call(mechanicalActiveX, "GetEuler");
                    double[] getEuleResultDouble = NumberUtils.stringToDoubleListNoScale(getEuleResult.getString(), ",");
                    addToLog("机械臂=>获取当前姿态=>[" + getEuleResult.getString() + "]");
                    if (getEuleResultDouble.length != 3) {
                        reportMoveThread.interrupt();
                        addToLog("机械臂旋=>获取当前姿态=>出错,停止执行", "danger");
                    }
                    // 获得当前的坐标
                    Variant getPositionResult = Dispatch.call(mechanicalActiveX, "GetPosition");
                    double[] getPositionResultDouble = NumberUtils.stringToDoubleListNoScale(getPositionResult.getString(), ",");
                    addToLog("机械臂=>获取当前坐标=>[" + getPositionResult.getString() + "]");
                    if (getPositionResultDouble.length != 3) {
                        reportMoveThread.interrupt();
                        addToLog("机械臂旋=>获取当前坐标=>出错,停止执行", "danger");
                    }
                    double[] param = new double[]{getPositionResultDouble[0], getPositionResultDouble[1], getPositionResultDouble[2], getEuleResultDouble[0], getEuleResultDouble[1], getEuleResultDouble[2]};
                    // 姿态1 倾斜角 2 俯仰角 3 水平角
                    for (double pitchAngleItem : pitchAngle) {
                        // 仰俯角
                        for (double inclinationAngleItem : inclinationAngle) {
                            // 倾斜角
                            if ("Rotation".equalsIgnoreCase(reportMoveType)) {
                                // 1.先用MoveWithAngle移动到位置
                                Variant result = Dispatch.call(mechanicalActiveX, "MoveWithAngle", NumberUtils.paramAddAngle(param, horizontalAngleRotationPosition, moveHorizontalAngle[0] / angleToRadian,
                                        pitchAngleRotationPosition, pitchAngleItem / angleToRadian,
                                        inclinationAngleRotationPosition, inclinationAngleItem / angleToRadian));
                                if (result.getString().contains("N")) {
                                    reportMoveThread.interrupt();
                                    addToLog("机械臂旋=>姿态移动=>出错[" + result.getString() + "],停止执行", "danger");
                                }
                                addToLog("机械臂=>姿态移动到水平角[" + moveHorizontalAngle[0] + "]/仰俯角[" + pitchAngleItem + "]/倾斜角[" + inclinationAngleItem + "]=>[" + result.getString() + "]");
                            }
                            // 2.获取当前rotation
                            // 获取当前角度
                            Variant getRotationResult = Dispatch.call(mechanicalActiveX, "GetRotation");
                            double[] getRotationResultDouble = NumberUtils.stringToDoubleListNoScale(getRotationResult.getString(), ",");
                            if (getRotationResultDouble.length != 6) {
                                reportMoveThread.interrupt();
                                addToLog("机械臂=>获取当前旋转角度失败=>[" + getRotationResult.getString() + "]");
                            } else {
                                addToLog("机械臂=>获取当前旋转角度成功=>[" + getRotationResult.getString() + "]");
                            }
                            // 准备位置移动到了,点亮屏幕
                            for (int j = 1; j < moveHorizontalAngle.length; j++) {
                                double horizontalAngleItem = moveHorizontalAngle[j];
                                if (linkAndroid) {
                                    // 黑屏幕状态，点亮屏幕
                                    androidDevice.inputKeyevent(224);
                                }
                                // 水平角
                                // "正面脸-左侧脸-正面脸-右侧脸-正面脸
                                // 为一个运动轨迹，一次需要3个运动轨迹，即为一个周期
                                if ("Rotation".equalsIgnoreCase(reportMoveType)) {
                                    // 3. 再用原先的AxisRotation移动第6轴做运动
                                    Variant resultAxisRotation = Dispatch.call(mechanicalActiveX, "AxisRotation",
                                            NumberUtils.paramAddAngle(getRotationResultDouble, -1, 0,
                                                    -1, 0,
                                                    6, horizontalAngleItem / angleToRadian));
                                    int rotationResult = resultAxisRotation.getInt();
                                    if (rotationResult != 0) {
                                        reportMoveThread.interrupt();
                                        addToLog("机械臂旋=>旋转=>出错[" + rotationResult + "],停止执行", "danger");
                                    }
                                    addToLog("机械臂=>旋转到水平角[" + horizontalAngleItem + "]/仰俯角[" + pitchAngleItem + "]/倾斜角[" + inclinationAngleItem + "]=>[" + rotationResult + "]");
                                } else {
                                    Variant result = Dispatch.call(mechanicalActiveX, "MoveWithAngle", NumberUtils.paramAddAngle(param, horizontalAngleRotationPosition, horizontalAngleItem / angleToRadian,
                                            pitchAngleRotationPosition, pitchAngleItem / angleToRadian,
                                            inclinationAngleRotationPosition, inclinationAngleItem / angleToRadian));
                                    if (result.getString().contains("N")) {
                                        reportMoveThread.interrupt();
                                        addToLog("机械臂旋=>姿态移动=>出错[" + result.getString() + "],停止执行", "danger");
                                    }
                                    addToLog("机械臂=>姿态移动到水平角[" + horizontalAngleItem + "]/仰俯角[" + pitchAngleItem + "]/倾斜角[" + inclinationAngleItem + "]=>[" + result.getString() + "]");
                                }
                                if (moveMoveSleepTime > 0) {
                                    Thread.sleep(moveMoveSleepTime);
                                }
                            }
                            // 等待x秒，处理结果
                            Platform.runLater(() -> {
                                // 保存记录
                                CheckItemEntity checkItem = new CheckItemEntity();
                                checkItem.setTYPE("MOVE");
                                checkItem.setREPORT_ID(String.valueOf(report.getID()));
                                checkItem.setTIME(TimeUtils.nowFull());
                                checkItem.setDISTANCE(String.valueOf(distance));
                                checkItem.setHORIZONTAL(NumberUtils.listToString(moveHorizontalAngle, ","));
                                checkItem.setPITCH(String.valueOf(pitchAngleItem));
                                checkItem.setINCLINATION(String.valueOf(inclinationAngleItem));
                                if (linkAndroid) {
                                    // 获取电池和屏幕状态
                                    try {
                                        int battery = androidDevice.getDevice().getBattery().get();
                                        checkItem.setBATTERY(String.valueOf(battery));
                                    } catch (Exception e) {
                                        checkItem.setBATTERY("NA");
                                    }
                                    try {
                                        String screenStatus = androidDevice.catFile(screenFilePath);
                                        checkItem.setRESULT(screenStatus.startsWith("3") ? "Y" : "N");
                                    } catch (Exception e) {
                                        checkItem.setRESULT("NA");
                                    }
                                } else {
                                    checkItem.setBATTERY("NA");
                                    checkItem.setRESULT("NA");
                                }
                                checkItem.setROTATION("");
                                checkItem.setPOS("0");
                                saveCheckItemRecord(checkItem);
                                if (linkAndroid) {
                                    androidDevice.inputKeyevent(223);
                                }
                            });
                            Thread.sleep(moveSleepTime);
                        }
                    }
                }
            } catch (Exception e) {
                // 被interrupt后Thread.sleep会抛异常,通过异常来终端thread
                e.printStackTrace();
                addToLog("执行=>出错,停止执行:" + e.getMessage(), "danger");
            } finally {
                trigViewsForReportThread(false);
            }
        });
        trigViewsForReportThread(true);
        reportMoveThread.start();
    }

    /**
     * 在报告执行过程中锁定部分view
     *
     * @param start
     */
    private void trigViewsForReportThread(boolean start) {
        Platform.runLater(() -> {
            reportChoice.setDisable(start);
            reportAddBtn.setDisable(start);
            reportDeviceChoice.setDisable(start);
        });
    }

    public void reportStop() {
        if (reportThread != null && reportThread.isAlive()) {
            reportThread.interrupt();
            addToLog("固定位检测执行中,停止执行", "info");
        } else {
            addToLog("固定位检测未在执行中执行中", "info");
        }
    }

    public void reportMoveStop() {
        if (reportMoveThread != null && reportMoveThread.isAlive()) {
            reportMoveThread.interrupt();
            addToLog("移动检测执行中,停止执行", "info");
        } else {
            addToLog("移动检测未在执行中执行中", "info");
        }
    }
    // [-] report
}
