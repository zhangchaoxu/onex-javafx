package de.felixroske.jfxsupport;

import com.mellonrobot.faceunlockfx.utils.IniHelper;
import javafx.application.Application;
import javafx.application.HostServices;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.ini4j.Wini;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public abstract class AppSupport extends Application {

    private static Logger LOGGER = LoggerFactory.getLogger(AppSupport.class);
    private static String[] savedArgs = new String[0];
    static Class<? extends AbstractFxmlView> savedInitialView;
    static SplashScreen splashScreen;
    private static ConfigurableApplicationContext applicationContext;
    private static List<Image> icons = new ArrayList();
    private final List<Image> defaultIcons = new ArrayList();
    private final CompletableFuture<Runnable> splashIsShowing = new CompletableFuture();

    protected AppSupport() {
    }

    public static Stage getStage() {
        return GUIState.getStage();
    }

    public static Scene getScene() {
        return GUIState.getScene();
    }

    public static HostServices getAppHostServices() {
        return GUIState.getHostServices();
    }

    public static SystemTray getSystemTray() {
        return GUIState.getSystemTray();
    }

    public static void showView(Class<? extends AbstractFxmlView> window, Modality mode) {
        AbstractFxmlView view = (AbstractFxmlView) applicationContext.getBean(window);
        Stage newStage = new Stage();
        Scene newScene;
        if (view.getView().getScene() != null) {
            newScene = view.getView().getScene();
        } else {
            newScene = new Scene(view.getView());
            newScene.getStylesheets().addAll("org/kordamp/bootstrapfx/bootstrapfx.css");
        }

        newStage.setScene(newScene);
        newStage.initModality(mode);
        newStage.initOwner(getStage());
        newStage.setTitle(view.getDefaultTitle());
        newStage.initStyle(view.getDefaultStyle());
        newStage.showAndWait();
    }

    private void loadIcons(ConfigurableApplicationContext ctx) {
        try {
            List<String> fsImages = PropertyReaderHelper.get(ctx.getEnvironment(), "javafx.appicons");
            if (!fsImages.isEmpty()) {
                fsImages.forEach((s) -> {
                    Image img = new Image(this.getClass().getResource(s).toExternalForm());
                    icons.add(img);
                });
            } else {
                icons.addAll(this.defaultIcons);
            }
        } catch (Exception var3) {
            LOGGER.error("Failed to load icons: ", var3);
        }

    }

    public void init() throws Exception {
        this.defaultIcons.addAll(this.loadDefaultIcons());
        CompletableFuture.supplyAsync(() -> {
            return SpringApplication.run(this.getClass(), savedArgs);
        }).whenComplete((ctx, throwable) -> {
            if (throwable != null) {
                LOGGER.error("Failed to load spring application context: ", throwable);
                Platform.runLater(() -> {
                    showErrorAlert(throwable);
                });
            } else {
                Platform.runLater(() -> {
                    this.loadIcons(ctx);
                    this.launchApplicationView(ctx);
                });
            }

        }).thenAcceptBothAsync(this.splashIsShowing, (ctx, closeSplash) -> {
            Platform.runLater(closeSplash);
        });
    }

    @Override
    public void start(Stage stage) throws Exception {
        GUIState.setStage(stage);
        GUIState.setHostServices(this.getHostServices());
        Stage splashStage = new Stage(StageStyle.TRANSPARENT);
        if (splashScreen.visible()) {
            Scene splashScene = new Scene(splashScreen.getParent(), Color.TRANSPARENT);
            splashStage.setScene(splashScene);
            splashStage.getIcons().addAll(this.defaultIcons);
            splashStage.initStyle(StageStyle.TRANSPARENT);
            this.beforeShowingSplash(splashStage);
            splashStage.show();
        }

        this.splashIsShowing.complete(() -> {
            this.showInitialView();
            if (splashScreen.visible()) {
                splashStage.hide();
                splashStage.setScene((Scene) null);
            }
        });
    }

    private void showInitialView() {
        String stageStyle = applicationContext.getEnvironment().getProperty("javafx.stage.style");
        if (stageStyle != null) {
            GUIState.getStage().initStyle(StageStyle.valueOf(stageStyle.toUpperCase()));
        } else {
            GUIState.getStage().initStyle(StageStyle.DECORATED);
        }

        this.beforeInitialView(GUIState.getStage(), applicationContext);
        showView(savedInitialView);
    }

    private void launchApplicationView(ConfigurableApplicationContext ctx) {
        applicationContext = ctx;
    }

    public static void showView(Class<? extends AbstractFxmlView> newView) {
        try {
            AbstractFxmlView view = (AbstractFxmlView) applicationContext.getBean(newView);
            if (GUIState.getScene() == null) {
                Scene scene = new Scene(view.getView());
                scene.getStylesheets().addAll("org/kordamp/bootstrapfx/bootstrapfx.css");
                GUIState.setScene(scene);
            } else {
                GUIState.getScene().setRoot(view.getView());
            }

            GUIState.getStage().setScene(GUIState.getScene());
            //关闭UI线程时同时关闭各子线程
            GUIState.getStage().setOnCloseRequest(event -> {
                // 尝试关闭adb
                // ShellCommand.exec(new CommandLine("adb kill-server"));
                // 退出杀死子线程
                System.exit(0);
            });
            applyEnvPropsToView();
            GUIState.getStage().getIcons().addAll(icons);
            GUIState.getStage().show();
        } catch (Throwable var2) {
            LOGGER.error("Failed to load application: ", var2);
            showErrorAlert(var2);
        }

    }

    private static void showErrorAlert(Throwable throwable) {
        Alert alert = new Alert(AlertType.ERROR, "Oops! An unrecoverable error occurred.\nPlease contact your software vendor.\n\nThe application will stop now.\n\nError: " + throwable.getMessage(), new ButtonType[0]);
        alert.showAndWait().ifPresent((response) -> {
            Platform.exit();
        });
    }

    private static void applyEnvPropsToView() {
        Wini ini = IniHelper.loadIni("default.ini");
        if (null == ini) {
            ConfigurableEnvironment var10000 = applicationContext.getEnvironment();
            Stage var10003 = GUIState.getStage();
            ini.get("SerialPort", "BAUD", int.class);
            PropertyReaderHelper.setIfPresent(var10000, "javafx.title", String.class, var10003::setTitle);
            var10000 = applicationContext.getEnvironment();
            var10003 = GUIState.getStage();
            PropertyReaderHelper.setIfPresent(var10000, "javafx.stage.width", Double.class, var10003::setWidth);
            var10000 = applicationContext.getEnvironment();
            var10003 = GUIState.getStage();
            PropertyReaderHelper.setIfPresent(var10000, "javafx.stage.height", Double.class, var10003::setHeight);
            var10000 = applicationContext.getEnvironment();
            var10003 = GUIState.getStage();
            PropertyReaderHelper.setIfPresent(var10000, "javafx.stage.resizable", Boolean.class, var10003::setResizable);
        } else {
            String title = ini.get("App", "title", String.class);
            Double width = ini.get("App", "width", Double.class);
            Double height = ini.get("App", "height", Double.class);
            Boolean resizable = ini.get("App", "resizable", Boolean.class);
            Boolean maximized = ini.get("App", "maximized", Boolean.class);
            Stage stage = GUIState.getStage();
            stage.setTitle(title);
            stage.setWidth(width);
            stage.setHeight(height);
            stage.setResizable(resizable);
            stage.setMaximized(maximized);
        }
    }

    public void stop() throws Exception {
        super.stop();
        if (applicationContext != null) {
            applicationContext.close();
        }

    }

    protected static void setTitle(String title) {
        GUIState.getStage().setTitle(title);
    }

    public static void launch(Class<? extends Application> appClass, Class<? extends AbstractFxmlView> view, String[] args) {
        launch(appClass, view, new SplashScreen(), args);
    }

    /**
     * @deprecated
     */
    @Deprecated
    public static void launchApp(Class<? extends Application> appClass, Class<? extends AbstractFxmlView> view, String[] args) {
        launch(appClass, view, new SplashScreen(), args);
    }

    public static void launch(Class<? extends Application> appClass, Class<? extends AbstractFxmlView> view, SplashScreen splashScreen, String[] args) {
        savedInitialView = view;
        savedArgs = args;
        if (splashScreen != null) {
            AppSupport.splashScreen = splashScreen;
        } else {
            AppSupport.splashScreen = new SplashScreen();
        }

        if (SystemTray.isSupported()) {
            GUIState.setSystemTray(SystemTray.getSystemTray());
        }

        Application.launch(appClass, args);
    }

    /**
     * @deprecated
     */
    @Deprecated
    public static void launchApp(Class<? extends Application> appClass, Class<? extends AbstractFxmlView> view, SplashScreen splashScreen, String[] args) {
        launch(appClass, view, splashScreen, args);
    }

    public void beforeInitialView(Stage stage, ConfigurableApplicationContext ctx) {
    }

    public void beforeShowingSplash(Stage splashStage) {
    }

    public Collection<Image> loadDefaultIcons() {
        return Arrays.asList(new Image(this.getClass().getResource("/icons/gear_16x16.png").toExternalForm()), new Image(this.getClass().getResource("/icons/gear_24x24.png").toExternalForm()), new Image(this.getClass().getResource("/icons/gear_36x36.png").toExternalForm()), new Image(this.getClass().getResource("/icons/gear_42x42.png").toExternalForm()), new Image(this.getClass().getResource("/icons/gear_64x64.png").toExternalForm()));
    }

}
