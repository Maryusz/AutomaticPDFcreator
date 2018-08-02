package com.mariuszbilda;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class ImageItem extends AnchorPane implements Initializable {
    @FXML
    ImageView iv_image;

    @FXML
    private Label lb_num_page;

    private ObjectProperty<Image> mImageObjectProperty = new SimpleObjectProperty<>();
    private StringProperty pageNumber = new SimpleStringProperty();

    public ImageItem() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ImageItem.fxml"));
        loader.setController(this);
        loader.setRoot(this);
        try {
            loader.load();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        iv_image.imageProperty().bind(mImageObjectProperty);
        lb_num_page.textProperty().bind(pageNumberProperty());
    }

    public String getPageNumber() {
        return pageNumber.get();
    }

    public void setPageNumber(String pageNumber) {
        this.pageNumber.set(pageNumber);
    }

    public StringProperty pageNumberProperty() {
        return pageNumber;
    }

    public Image getImageObjectProperty() {
        return mImageObjectProperty.get();
    }

    public void setImageObjectProperty(Image imageObjectProperty) {
        this.mImageObjectProperty.set(imageObjectProperty);
    }

    public ObjectProperty<Image> imageObjectPropertyProperty() {
        return mImageObjectProperty;
    }


}
