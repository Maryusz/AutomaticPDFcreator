package com.mariuszbilda;

import com.jfoenix.controls.JFXCheckBox;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.*;
import java.net.URL;
import java.nio.file.*;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainScreenController implements Initializable{

    private Properties properties;
    private Logger logger;
    private String pathToObserve;
    private String saveDirectory;
    private Map<String, ImageView> listOfFiles;
    private DirectoryChooser directoryChooser;

    private int pageCounter;
    @FXML
    private AnchorPane root;

    @FXML
    private HBox imageBox;

    @FXML
    private Label labelNumberOfPages;

    @FXML
    private Label labelWatchedDirectory;

    @FXML
    private Label labelSaveDir;

    @FXML
    private JFXCheckBox checkboxDelete;

    /**
     * Initialize instantiate all essential data structures and properties loader.
     * @param location
     * @param resources
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {



        imageBox.setCache(true);
        pageCounter = 0;
        logger = Logger.getLogger(getClass().getName());
        listOfFiles = new LinkedHashMap<>();
        properties = new Properties();

        try {
            logger.log(Level.INFO, "Trying to load properties...");
            properties.loadFromXML(new FileInputStream("settings.xml"));

            pathToObserve = properties.getProperty("directoryToWatch");
            saveDirectory = properties.getProperty("saveDirectory");
        } catch (IOException ioe ){
            logger.log(Level.SEVERE, ioe.toString());
        } finally {
            try {
                logger.log(Level.WARNING, "Creating a new empty setting file.");
                FileOutputStream newSettingsFile = new FileOutputStream("settings.xml");
                properties.setProperty("saveDirectory", "C:\\");
                properties.setProperty("directoryToWatch", "C:\\");
                properties.storeToXML(newSettingsFile, "");

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        labelWatchedDirectory.setText(pathToObserve);
        labelSaveDir.setText(saveDirectory);
        watcherService();
    }


    @FXML
    void chooseDirectory(ActionEvent event) {


        directoryChooser = new DirectoryChooser();
        File file = directoryChooser.showDialog(root.getScene().getWindow());
        properties.setProperty("directoryToWatch", file.getPath());
        pathToObserve = file.getPath();
        labelWatchedDirectory.setText(pathToObserve);

        watcherService();
    }

    public void chooseDestinationDir(ActionEvent actionEvent) {

        directoryChooser = new DirectoryChooser();
        File file = directoryChooser.showDialog(root.getScene().getWindow());
        properties.setProperty("saveDirectory", file.getPath());
        saveDirectory = file.getPath();
        labelSaveDir.setText(saveDirectory);
        try {
            properties.storeToXML(new FileOutputStream("settings.xml"), "");

            logger.log(Level.INFO, "Properties saved");
        } catch (IOException ioe ){
            logger.log(Level.SEVERE, ioe.toString());
        }
    }

    /**
     * This method launches a Task who observe the directory choosen and when an image is saved to that directoy
     * it adds it to the visual part (HBox) and to the data structure (in this case a LinkedHashMap, for order preservation)
     */
    private void watcherService(){
        try {
            WatchService ws = FileSystems.getDefault().newWatchService();
            Path path = Paths.get(pathToObserve);

            WatchKey watchKey = path.register(ws, StandardWatchEventKinds.ENTRY_CREATE);

            Task<Void> task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {

                    WatchKey key;
                    try {
                        while ((key = ws.take()) != null) {
                            for (WatchEvent<?> event : key.pollEvents()) {

                                if (event.kind().equals(StandardWatchEventKinds.ENTRY_CREATE)) {

                                    Platform.runLater(() -> {
                                        Image image = new Image("file:" + pathToObserve + "\\" + event.context(), 200, 300, true, false);
                                        ImageView imageView = new ImageView(image);

                                        listOfFiles.put(pathToObserve + "\\" + event.context(), imageView);

                                        imageBox.getChildren().add(listOfFiles.get(pathToObserve + "\\" + event.context()));
                                        logger.log(Level.INFO, "Adding " + listOfFiles.get(pathToObserve + "\\" + event.context()) + " to HBox");
                                        pageCounter++;
                                        labelNumberOfPages.setText(""+pageCounter);
                                    });
                                }
                            }
                            key.reset();
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    return null;
                }
            };

            Thread t = new Thread(task);
            t.setDaemon(true);
            t.start();


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * This method instantiate com.mariuszbilda.PDFManager, and permits to add pages taking path from listOfFiles,
     * then clear the GUI, create the PDF file and delete or not the files used.
     * @param actionEvent
     */
    public void createPDFFile(ActionEvent actionEvent) {

        PDFManager pdfManager = new PDFManager();
        for (String s : listOfFiles.keySet()){
            pdfManager.addPage(s);
           logger.log(Level.INFO, "Page added.");
        }

        pdfManager.savePDF(saveDirectory);


        //TODO: Enable autoreset and autodelet of the processed files.
        if (checkboxDelete.isSelected()) {
            for (String s : listOfFiles.keySet()){
                logger.log(Level.WARNING, String.format("%s deleted.", s));
                File file = new File(s);
                System.out.println(file.delete());
            }

        }

        pageCounter = 0;
        imageBox.getChildren().clear();
        listOfFiles.clear();

    }

    public void showLogWindow(ActionEvent actionEvent) {
        //TODO: Aggiungere la possibilita di visualizzare il log.
    }

    public void showDeveloperInfo(ActionEvent actionEvent) {

        try {
            Stage stage = new Stage();
            stage.getIcons().add(new Image("/icons/icons8_Parchment_96px.png"));
            Parent parent = FXMLLoader.load(getClass().getResource("/fxml/DeveloperInfo.fxml"));
            Scene scene = new Scene(parent);
            stage.setScene(scene);
            stage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}
