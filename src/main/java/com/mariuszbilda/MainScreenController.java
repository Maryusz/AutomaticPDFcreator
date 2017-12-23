package com.mariuszbilda;

import com.jfoenix.controls.JFXCheckBox;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MainScreenController implements Initializable{

    private Properties properties;
    private Logger logger;
    private String pathToObserve;
    private String saveDirectory;
    private Map<File, ImageView> listOfFiles;
    private DirectoryChooser directoryChooser;

    private IntegerProperty pageCounter;
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
        //Essential initializations
        imageBox.setCache(true);
        logger = Logger.getLogger(getClass().getName());
        listOfFiles = new LinkedHashMap<>();
        properties = new Properties();

        // Counter of pages and its binding
        pageCounter = new SimpleIntegerProperty(0);
        labelNumberOfPages.textProperty().bind(pageCounter.asString());

        // Setting loading, and absence of a setting file managed
        try {
            logger.log(Level.INFO, "Trying to load properties...");
            properties.loadFromXML(new FileInputStream("settings.xml"));

            pathToObserve = properties.getProperty("directoryToWatch");
            saveDirectory = properties.getProperty("saveDirectory");
        } catch (IOException ioe) {
            logger.log(Level.SEVERE, ioe.toString());
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

        // Setting of folders and start of watch service
        labelWatchedDirectory.setText(pathToObserve);
        labelSaveDir.setText(saveDirectory);

        checkboxDelete.selectedProperty().addListener(observable -> {
            if (checkboxDelete.isSelected()) {
                String content = "Abilitando questa impostazione i file immagine originali che verranno utilizzati per creare il PDF verranno eliminati NESSUNA senza possibilità di recupero!";
                Alert a = new Alert(Alert.AlertType.WARNING, content);
                a.showAndWait();
            }
        });

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

                                        // TODO: Creare un metodo per verificare se viene aggiunto un File o una Directory e se è una directory, chiedere se si vuole osservare i file creati in questa sottocartella


                                        Image image = new Image("file:" + pathToObserve + "\\" + event.context(), 200, 300, true, false);
                                        ImageView imageView = new ImageView(image);

                                        listOfFiles.put(new File(pathToObserve + "\\" + event.context()), imageView);

                                        imageBox.getChildren().add(imageView);
                                        logger.log(Level.INFO, "Adding image to HBox");
                                        pageCounter.setValue(pageCounter.getValue() + 1);


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

        if (listOfFiles.keySet().size() == 0) {
            // If there's no images to transform in PDF, an alert its showed
            showNoImageAlert();

        } else {

            pageCounter.set(0);
            PDFManager pdfManager = new PDFManager();
            for (File f : listOfFiles.keySet()) {
                pdfManager.addPage(f);
                logger.log(Level.INFO, "Page added.");
            }

            pdfManager.savePDF(saveDirectory);


            //TODO: Enable autoreset and autodelet of the processed files.
            if (checkboxDelete.isSelected()) {
                for (File f : listOfFiles.keySet()) {
                    logger.log(Level.WARNING, String.format("%s deleted.", f));

                    System.out.println(f.delete());
                }

            }

            imageBox.getChildren().clear();
            listOfFiles.clear();
        }

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

    private void showNoImageAlert() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setHeaderText("Nessuna immagine rilevata!");
        alert.setTitle("Nessuna immagine da trasformare in PDF.");
        alert.setContentText("Non è stata rilevata nessuna immagine da trasformare in PDF, " +
                "verifica che ci sia almeno un'immagine (presente nella schermata o rilevata tramite numero)" +
                " altrimenti non sarà possibile creare il file PDF!");
        alert.showAndWait();
    }
}
