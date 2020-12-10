package com.mellonrobot.faceunlockfx;

import com.mellonrobot.faceunlockfx.utils.IniHelper;
import de.felixroske.jfxsupport.FXMLController;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceBox;
import org.ini4j.Wini;

import java.net.URL;
import java.util.ResourceBundle;

@FXMLController
public class SettingsController extends BaseController implements Initializable {

    // 配置文件
    Wini ini;

    // 串口设置的4个参数
    @FXML
    ChoiceBox serPortBaudRate;
    @FXML
    ChoiceBox serPortParity;
    @FXML
    ChoiceBox serPortDataBits;
    @FXML
    ChoiceBox serPortStopBits;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        fillSerialPortParam();
    }

    /**
     * 将配置信息塞入ui
     */
    private void fillSerialPortParam() {
        if (null == ini) {
            ini = IniHelper.loadIni("default.ini");
        }
        if (null == ini) {
            showDialog(Alert.AlertType.ERROR, "找不到配置文件");
            return;
        }

        //串口波特率
        String[] baudRate = ini.get("SerialPort", "baudRate", String.class).split(",");
        for (String s : baudRate) {
            serPortBaudRate.getItems().add(s);
        }
        serPortBaudRate.setValue(ini.get("SerialPort", "baudRateDefault", String.class));

        //串口检验位设置
        String[] parity = ini.get("SerialPort", "parity", String.class).split(",");
        for (String s : parity) {
            serPortParity.getItems().add(s);
        }
        serPortParity.setValue(ini.get("SerialPort", "parityDefault", String.class));

        //数据位设置
        String[] dataBits = ini.get("SerialPort", "dataBits", String.class).split(",");
        for (String s : dataBits) {
            serPortDataBits.getItems().add(s);
        }
        serPortDataBits.setValue(ini.get("SerialPort", "dataBitsDefault", String.class));

        //停止位设置
        String[] stopBits = ini.get("SerialPort", "stopBits", String.class).split(",");
        for (String s : stopBits) {
            serPortStopBits.getItems().add(s);
        }
        serPortStopBits.setValue(ini.get("SerialPort", "stopBitsDefault", String.class));
    }

    public void saveSerialPortParam() {
        if (null == ini) {
            ini = IniHelper.loadIni("default.ini");
        }
        if (null == ini) {
            showDialog(Alert.AlertType.ERROR, "找不到配置文件");
            return;
        }

        ini.put("SerialPort", "baudRateDefault", serPortBaudRate.getValue());
        ini.put("SerialPort", "parityDefault", serPortParity.getValue());
        ini.put("SerialPort", "dataBitsDefault", serPortDataBits.getValue());
        ini.put("SerialPort", "stopBitsDefault", serPortStopBits.getValue());
        try {
            ini.store();
            showDialog(Alert.AlertType.INFORMATION, "保存成功");
        } catch (Exception e) {
            showExceptionDialog("配置文件保存失败", e);
        }
    }

    /**
     * 返回首页
     */
    public void goMain() {
        // 返回首页
        App.showView(MainView.class);
    }
}
