package com.mellonrobot.faceunlockfx;

import javafx.beans.NamedArg;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Optional;

/**
 * 基础Controller
 */
public class BaseController {

    protected void showExceptionDialog(String message, Exception e) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("错误");
        alert.setHeaderText(StringUtils.isEmpty(message) ? "程序发生错误" : message);
        alert.initStyle(StageStyle.UTILITY);
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        TextArea textArea = new TextArea(sw.toString());
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);
        GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(textArea, 0, 1);
        alert.getDialogPane().setExpandableContent(expContent);
    }

    protected Optional<ButtonType> showDialog(@NamedArg("alertType") Alert.AlertType alertType, String header) {
        return this.showDialog(alertType, header, null);
    }

    protected Optional<ButtonType> showDialog(@NamedArg("alertType") Alert.AlertType alertType, String header, String content) {
        Alert alert = new Alert(alertType);
        if (alertType == Alert.AlertType.ERROR) {
            alert.setTitle("错误");
        } else if (alertType == Alert.AlertType.WARNING) {
            alert.setTitle("警告");
        } else if (alertType == Alert.AlertType.CONFIRMATION) {
            alert.setTitle("提示");
        } else if (alertType == Alert.AlertType.INFORMATION) {
            alert.setTitle("信息");
        } else {
            alert.setTitle(null);
        }
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.initStyle(StageStyle.UTILITY);
        return alert.showAndWait();
    }

    /**
     * 保存文本到txt文件
     *
     * @param content
     */
    protected void saveToTextFile(String content, String fileName) {
        FileChooser fileChooser = new FileChooser();
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("TXT files (*.txt)", "*.txt");
        fileChooser.getExtensionFilters().add(extFilter);
        if (!StringUtils.isEmpty(fileName)) {
            fileChooser.setInitialFileName(fileName);
        }
        File file = fileChooser.showSaveDialog(new Stage());
        if (file == null)
            return;
        if (file.exists()) {
            file.delete();
        }
        try {
            FileOutputStream out = new FileOutputStream(file); //true表示追加
            out.write(content.getBytes("utf-8"));
            out.close();
        } catch (Exception e) {
            showExceptionDialog("Txt文件保存失败", e);
        }
    }

    /**
     * 保存文本到excel文件
     *
     * @param workbook
     */
    protected void saveToExcelFile(Workbook workbook, String fileName) {
        FileChooser fileChooser = new FileChooser();
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Excel files (*.xls)", "*.xls");
        fileChooser.getExtensionFilters().add(extFilter);
        if (!StringUtils.isEmpty(fileName)) {
            fileChooser.setInitialFileName(fileName);
        }
        File file = fileChooser.showSaveDialog(new Stage());
        if (file == null)
            return;
        if (file.exists()) {
            file.delete();
        }
        try {
            FileOutputStream out = new FileOutputStream(file);
            workbook.write(out);
            out.close();
        } catch (Exception e) {
            showExceptionDialog("Excel文件保存失败", e);
        }
    }

}
