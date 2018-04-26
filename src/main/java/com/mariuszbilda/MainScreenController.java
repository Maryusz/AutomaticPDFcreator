package com.mariuszbilda;

import com.jfoenix.controls.JFXCheckBox;
import com.jfoenix.controls.JFXProgressBar;
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
import javafx.scene.control.*;
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

    @FXML
    private JFXProgressBar progress;

    @FXML
    private MenuItem menuItemReset;


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

        //TODO: Impostare il tasto reset per resettare la creazione di un PDF.
        menuItemReset.setOnAction(e -> {
            pageCounter.setValue(0);
            for (File f : listOfFiles.keySet()) {
                f.delete();
                logger.log(Level.WARNING, String.format("%s deleted.", f));
            }

            listOfFiles.clear();

            Platform.runLater(() -> {
                imageBox.getChildren().clear();
            });

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
                protected Void call() {

                    WatchKey key;
                    try {
                        while ((key = ws.take()) != null) {
                            for (WatchEvent<?> event : key.pollEvents()) {

                                if (event.kind().equals(StandardWatchEventKinds.ENTRY_CREATE)) {
                                    //TODO: DA PROVARE
                                    // Messo lo sleep per dare all'altro processo il tempo di salvare l'immagine.
                                    Thread.sleep(300);

                                    Platform.runLater(() -> {

                                        File file = new File(pathToObserve + "\\" + event.context());

                                        /**
                                         * The WatcherService detects if the new file in the directory is a File or a Directory,
                                         * if its a file, it porcess it as normale, if its a directory, an alert is showed
                                         * and its asked if the user want to change the watched directory to the directory registered.
                                         */
                                        if (file.isFile()) {
                                            fileDetected(event, file);
                                        }
                                        if (file.isDirectory()) {
                                            directoryDetected(event);
                                        }
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

    private void fileDetected(WatchEvent<?> event, File file) {

        Image image = new Image("file:" + pathToObserve + "\\" + event.context(), 200, 300, true, false);
        ImageView imageView = new ImageView(image);
        imageView.setCache(true);
        addContextMenu(imageView);

        listOfFiles.put(file, imageView);

        imageBox.getChildren().add(imageView);
        logger.log(Level.INFO, "Adding image to HBox");
        pageCounter.setValue(pageCounter.getValue() + 1);
    }

    private void directoryDetected(WatchEvent<?> event) {
        logger.log(Level.INFO, "Directory creation detected...");
        String content = "E' stata rilveta la creazione di una sottocartella nella cartella da te osservata, " +
                "seleziona il tasto OK se non sei certo e prova a fare una scansione, altrimenti se la cartella l'hai creata tu per altri scopi," +
                "seleziona il tasto Annulla";
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, content);
        if (a.showAndWait().get() == ButtonType.OK) {
            pathToObserve += "\\" + event.context();


            logger.log(Level.INFO, "New path to observe: " + pathToObserve);

            // Restart WatchService
            watcherService();
        }
    }

    /**
     * This method instantiate com.mariuszbilda.PDFManager, and permits to add pages taking path from listOfFiles,
     * then clear the GUI, create the PDF file and delete or not the files used.
     *
     * A task is launched to not freeze the gui, and the progress bar is updated for the creation of single pages.
     * @param actionEvent
     */
    public void createPDFFile(ActionEvent actionEvent) {
        Task<Void> pdfCreationTask = new Task<Void>() {
            @Override
            protected Void call() {
                updateProgress(0.0, 1000.0);

                if (listOfFiles.keySet().size() == 0) {
                    // If there's no images to transform in PDF, an alert its showed
                    Platform.runLater(() -> {
                        showNoImageAlert();
                    });


                } else {
                    progress.setId("bar");
                    Platform.runLater(() -> {
                        pageCounter.set(0);
                    });

                    PDFManager pdfManager = new PDFManager();

                    // calculation for progress bar
                    double part = 1000.0 / listOfFiles.keySet().size();
                    double actual = 0;
                    for (File f : listOfFiles.keySet()) {
                        actual += part;
                        pdfManager.addPage(f);
                        updateProgress(actual, 1000.0);
                        logger.log(Level.INFO, "Page added.");
                    }

                    pdfManager.savePDF(saveDirectory);
                    updateProgress(1000.0, 1000.0);

                    if (checkboxDelete.isSelected()) {
                        for (File f : listOfFiles.keySet()) {
                            f.delete();
                            logger.log(Level.WARNING, String.format("%s deleted.", f));
                        }
                    }
                    Platform.runLater(() -> {
                        imageBox.getChildren().clear();
                        listOfFiles.clear();

                    });
                }

                return null;
            }
        };

        progress.progressProperty().bind(pdfCreationTask.progressProperty());

        Thread t = new Thread(pdfCreationTask);
        t.setDaemon(true);
        t.start();

    }

    public void showLogWindow(ActionEvent actionEvent) {
        //TODO: show log on another window...
    }

    private void addContextMenu(ImageView iv) {
        iv.setOnContextMenuRequested(event -> {
            MenuItem deleteImage = new MenuItem("Cancella");
            MenuItem changePageNumber = new MenuItem("Cambia numero pagina");
            MenuItem modify = new MenuItem("Modifica");
            ContextMenu cm = new ContextMenu(deleteImage, changePageNumber, modify);


            cm.show(iv, event.getScreenX(), event.getScreenY());
        });


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
