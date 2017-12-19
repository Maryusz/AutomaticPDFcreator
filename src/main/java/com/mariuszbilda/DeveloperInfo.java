package com.mariuszbilda;

import javafx.animation.RotateTransition;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

import java.net.URL;
import java.util.ResourceBundle;

public class DeveloperInfo implements Initializable {

    @FXML
    private ImageView programIcon;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        RotateTransition rotateTransition = new RotateTransition(Duration.seconds(2), programIcon);
        rotateTransition.setFromAngle(45);
        rotateTransition.setToAngle(0);
        rotateTransition.play();
    }
}
