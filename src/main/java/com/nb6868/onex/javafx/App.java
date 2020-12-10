package com.mellonrobot.faceunlockfx;

import de.felixroske.jfxsupport.AppSupport;
import javafx.scene.image.Image;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Collection;
import java.util.Collections;

/**
 * Application
 *
 * @author Charles zhangchaoxu@gmail.com
 */
@SpringBootApplication
public class App extends AppSupport {

    public static void main(String[] args) {
        launch(App.class, MainView.class, new AppSplashScreen(), args);
    }

    /**
     * 定义菜单图标
     */
    @Override
    public Collection<Image> loadDefaultIcons() {
        return Collections.singletonList(new Image(this.getClass().getResource("/images/icon.png").toExternalForm()));
    }

}
