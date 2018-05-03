package com.taobaospider;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * <p>Title: SpiderApplication.java </p>
 * <p>Description 启动类 </p>
 * @author Wjj
 * @CreateDate 2018/4/20 16:59
 */
public class SpiderApplication extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("/view/MainWindow.fxml"));
        primaryStage.setTitle("Hello World");
        Scene scene = new Scene(root, 600, 275);
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
