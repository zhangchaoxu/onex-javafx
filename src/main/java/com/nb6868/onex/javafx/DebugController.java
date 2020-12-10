package com.mellonrobot.faceunlockfx;

import com.mellonrobot.faceunlockfx.utils.IniHelper;
import de.felixroske.jfxsupport.FXMLController;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import jssc.SerialPort;
import jssc.SerialPortException;
import jssc.SerialPortList;
import org.ini4j.Wini;

import java.net.URL;
import java.util.Date;
import java.util.ResourceBundle;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 调试
 *
 * @author Charles
 */
@FXMLController
public class DebugController extends BaseController implements Initializable {

    // 串口
    SerialPort serialPort = null;
    // 配置文件
    Wini ini;

    @FXML
    ChoiceBox serPort;
    @FXML
    ChoiceBox serPortBaudRate;
    @FXML
    ChoiceBox serPortParity;
    @FXML
    ChoiceBox serPortDataBits;
    @FXML
    ChoiceBox serPortStopBits;

    @FXML
    Button serPortOpenBtn;
    @FXML
    Button serPortRefreshBtn;
    @FXML
    CheckBox recvShowHex;
    @FXML
    CheckBox recvShowTime;
    @FXML
    CheckBox recvStopShow;
    @FXML
    Button recvClear;
    @FXML
    CheckBox sendHex;
    @FXML
    CheckBox sendCycle;
    @FXML
    TextField sendCycleRap;
    @FXML
    Button sendClear;
    @FXML
    Label sendCount;
    @FXML
    Label recvCount;
    @FXML
    Button CountReset;
    @FXML
    TextArea sendTextAear;
    @FXML
    TextArea recvTextAear;
    @FXML
    Button sendBtn;

    Timer t;

    /**
     * 返回首页
     */
    public void goMain() {
        // 关闭串口
        if (serialPort != null && serialPort.isOpened()) {
            try {
                serialPort.closePort();
                serPort.setDisable(false);
            } catch (SerialPortException e) {
                showExceptionDialog("关闭串口错误", e);
            }
        }
        // 返回首页
        App.showView(MainView.class);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        fillSerialPortParam();
        refreshSerialPort();

        serPortOpenBtn.setOnAction(event-> {
            if (serialPort != null && serialPort.isOpened()) try {
                serialPort.closePort();
                serPortOpenBtn.setText("打开");
                serPort.setDisable(false);
                serPortRefreshBtn.setDisable(false);
                serPortBaudRate.setDisable(false);
                serPortParity.setDisable(false);
                serPortDataBits.setDisable(false);
                serPortStopBits.setDisable(false);
                return;
            } catch (SerialPortException e) {
                showExceptionDialog("关闭串口错误", e);
            }
            serialPort = new SerialPort((String) serPort.getValue());
            try {
                int baudRateDefault = ini.get("SerialPort", "baudRateDefault", Integer.class);
                int dataBitsDefault = ini.get("SerialPort", "dataBitsDefault", Integer.class);
                int stopBitsDefault = ini.get("SerialPort", "stopBitsDefault", Integer.class);
                int parityDefault = ini.get("SerialPort", "parityDefault", Integer.class);

                serialPort.openPort();
                serialPort.setParams(baudRateDefault, dataBitsDefault, stopBitsDefault, parityDefault);
                serialPort.purgePort(SerialPort.PURGE_RXCLEAR);
                serialPort.purgePort(SerialPort.PURGE_TXCLEAR);
                serialPort.setEventsMask(SerialPort.MASK_RXCHAR);
                UsartRXEven();
                serPortOpenBtn.setText("关闭");
                serPort.setDisable(true);
                serPortRefreshBtn.setDisable(true);
                serPortBaudRate.setDisable(true);
                serPortParity.setDisable(true);
                serPortDataBits.setDisable(true);
                serPortStopBits.setDisable(true);
            } catch (SerialPortException e) {
                showExceptionDialog("打开串口错误", e);
            }
        });

        sendBtn.setOnAction(event -> {
            if (null == serialPort || (!serialPort.isOpened())) {
                showDialog(Alert.AlertType.WARNING, "请先打开串口");
                return;
            }
            try {
                if (sendHex.isSelected()) {
                    serialPort.writeBytes(hexStringToBytes(sendTextAear.getText()));
                    sendCount.setText(String.valueOf((Integer.parseInt(sendCount.getText()) + hexStringToBytes(sendTextAear.getText()).length)));
                } else {
                    serialPort.writeBytes(sendTextAear.getText().getBytes());
                    sendCount.setText(String.valueOf((Integer.parseInt(sendCount.getText()) + sendTextAear.getText().getBytes().length)));
                }

            } catch (Exception e) {
                showExceptionDialog("发送数据错误", e);
            }
        });

        recvClear.setOnAction(event -> {
            recvTextAear.setText("");
        });
        sendHex.setOnAction(event -> {
            if (!sendHex.isSelected())
                try {
                    sendTextAear.setText(new String(hexStringToBytes(sendTextAear.getText())));
                } catch (Exception e) {
                    showExceptionDialog("非法16进制字符", e);
                }
            else
                sendTextAear.setText(bytesToHexString(sendTextAear.getText().getBytes()));
        });
        sendClear.setOnAction(event -> {
            sendTextAear.setText("");
        });
        CountReset.setOnAction(event -> {
            sendCount.setText("0");
            recvCount.setText("0");
        });
        sendCycle.setOnAction(event -> {
            if (null == serialPort || (!serialPort.isOpened())) {
                showDialog(Alert.AlertType.WARNING, "请先打开串口");
                sendCycle.setSelected(false);
                return;
            }
            try {
                if (sendCycle.isSelected()) {
                    sendBtn.setDisable(true);
                    sendCycleRap.setDisable(true);
                    t = new Timer();

                    byte[] sendData = sendHex.isSelected() ? hexStringToBytes(sendTextAear.getText()) : sendTextAear.getText().getBytes();

                    TimerTask task = new TimerTask() {
                        public void run() {
                            // task to run goes here
                            //System.out.println("Hello !!!");
                            try {
                                serialPort.writeBytes(sendData);
                                Platform.runLater(() -> {
                                    sendCount.setText(String.valueOf((Integer.parseInt(sendCount.getText()) + sendData.length)));
                                });
                            } catch (SerialPortException e) {
                                showExceptionDialog("循环发送错误", e);
                            }
                        }
                    };
                    t.schedule(task, 0, new Long(sendCycleRap.getText()));
                } else {
                    t.cancel();
                    sendBtn.setDisable(false);
                    sendCycleRap.setDisable(false);
                }
            } catch (Exception e) {
                showExceptionDialog("循环发送错误", e);
            }
        });
    }

    /**
     * 刷新串口号
     */
    public void refreshSerialPort() {
        if (serialPort != null && serialPort.isOpened()) {
            showDialog(Alert.AlertType.INFORMATION, "请先关闭串口连接");
            return;
        }
        String[] ports = SerialPortList.getPortNames();
        serPort.getItems().remove(0, serPort.getItems().size());
        if (ports.length != 0) {
            for (String s : ports) {
                serPort.getItems().add(s);
            }
            serPort.setValue(ports[0]);
        }
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

    public void UsartRXEven() {
        try {
            serialPort.addEventListener(serialPortEvent -> {
                try {
                    if (recvStopShow.isSelected()) {
                        serialPort.readHexString();
                        return;
                    }
                    byte[] bytes = serialPort.readBytes();
                    if (bytes != null) {
                        String str = recvShowHex.isSelected() ? bytesToHexString(bytes) : (new String((bytes)));
                        if (recvShowTime.isSelected()) str = new Date().toString() + ": " + str;
                        String newStr = recvTextAear.getText().isEmpty() ? ("" + str) : (recvTextAear.getText() + "\n" + str);
                        recvTextAear.setText(newStr);
                        recvTextAear.setScrollTop(recvTextAear.getMaxHeight());
                        Platform.runLater(() -> {
                            recvCount.setText(String.valueOf((Integer.parseInt(recvCount.getText()) + bytes.length)));
                        });
                    }
                } catch (SerialPortException e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static byte[] hexStringToBytes(String hexString) throws Exception {
        if (hexString == null || hexString.equals("")) {
            return null;
        }
        hexString = hexString.replace(" ", "");
        if (hexString.length() % 2 != 0) hexString = "0" + hexString;
        hexString = hexString.toUpperCase();
        int length = hexString.length() / 2;
        char[] hexChars = hexString.toCharArray();
        byte[] d = new byte[length];
        for (int i = 0; i < length; i++) {
            int pos = i * 2;
            d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
        }
        return d;
    }

    /**
     * Convert char to byte
     *
     * @param c char
     * @return byte
     */
    private static byte charToByte(char c) throws Exception {
        int i = "0123456789ABCDEF".indexOf(c);
        if (i == -1) throw new Exception("非法的十六进制值");
        return (byte) i;
    }

    public static String bytesToHexString(byte[] bArray) {
        if (bArray == null) return null;
        StringBuilder sb = new StringBuilder(bArray.length);
        String sTemp;
        for (byte b : bArray) {
            sTemp = Integer.toHexString(0xFF & b);
            if (sTemp.length() < 2)
                sb.append(0);
            sb.append(sTemp.toUpperCase());
        }
        return sb.toString();
    }

}
