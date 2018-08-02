package com.mariuszbilda;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.controlsfx.control.Notifications;

import java.util.Scanner;


public class AutomaticPDFCreator extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        primaryStage.getIcons().add(new Image("/icons/icons8_Parchment_96px.png"));
        Parent root = FXMLLoader.load(getClass().getResource("/fxml/MainScreen.fxml"));
        primaryStage.setTitle("Automatic PDF Creator - v. 0.6.6 Mariusz A. Bilda");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();


        showNewFeatures();
    }


    public static void main(String[] args) {
        launch(args);
    }

    private void showNewFeatures() {
        Scanner sc = new Scanner(getClass().getResourceAsStream("/changes/changes.txt"));
        String text = "";
        while (sc.hasNext()) {
            text += "\n" + sc.nextLine();
        }

        Notifications.create()
                .title("Aggiornamenti e note sulla nuova versione di Automatic PDF Creator!")
                .graphic(new ImageView(new Image(getClass().getResourceAsStream("/icons/icons8_Parchment_96px.png"))))
                .text(text)
                .hideAfter(Duration.seconds(30.0))
                .show();
    }
}