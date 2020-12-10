package com.mellonrobot.faceunlockfx.utils;

import org.ini4j.Wini;

import java.io.File;
import java.io.IOException;
import java.util.InvalidPropertiesFormatException;

/**
 * 配置文件
 *
 * @author Charles
 */
public class IniHelper {

    // 打包后为app目录,开发过程为项目根目录
    private static final String path = System.getProperty("user.dir");

    public static Wini loadIni(String filePath) {
        try {
            return new Wini(new File(path + "/db/" + filePath));
        } catch (IOException ioe) {
            System.out.println("找不到配置文件");
            return null;
        }
    }

    // ini file save method
    public static boolean saveIni(Wini iniFile) {
        try {
            iniFile.store();
            return true;
        } catch (InvalidPropertiesFormatException e) {
            System.out.println("Invalid file format.");
            return false;
        } catch (IOException e) {
            System.out.println("Problem reading file.");
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static String getPath() {
        return path;
    }

}
