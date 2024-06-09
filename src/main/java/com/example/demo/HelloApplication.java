package com.example.demo;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

public class HelloApplication extends Application {
    private static final String FOLDER_IMAGE_PATH = "C:\\Users\\vappyq\\Downloads\\style-plat-du-dossier\\FolderImage.jpg";
    private static final String DIRECTORY_PATH = "C:\\Users\\vappyq\\Documents\\ProjetM2\\ECPML\\modeles-in-XML";

    @Override
    public void start(Stage primaryStage) {
        // Create a FlowPane to hold all folder boxes
        FlowPane flowPane = new FlowPane();
        flowPane.setPadding(new Insets(20));
        flowPane.setHgap(20); // Horizontal gap between children
        flowPane.setVgap(20); // Vertical gap between children
        flowPane.setAlignment(Pos.TOP_LEFT);

        // Create the folder boxes dynamically based on the directory contents
        File directory = new File(DIRECTORY_PATH);
        if (directory.exists() && directory.isDirectory()) {
            for (File file : Objects.requireNonNull(directory.listFiles())) {
                if (file.isDirectory()) {
                    // Create a folder box for each directory
                    VBox folderBox = createFolderBox(file);
                    flowPane.getChildren().add(folderBox); // Add to FlowPane
                }
            }
        }

        // Create a ScrollPane to make the white container scrollable
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setContent(flowPane);
        scrollPane.setFitToHeight(true);
        scrollPane.setFitToWidth(true);
        scrollPane.setPannable(true);
        scrollPane.setStyle("-fx-background: white; -fx-background-color: transparent;");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); // Hide horizontal scroll bar
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); // Hide vertical scroll bar

        // Create a white container with rounded borders and shadow
        StackPane container = new StackPane(scrollPane);
        container.setStyle("-fx-background-color: white; -fx-border-radius: 20; -fx-background-radius: 20;");
        container.setPadding(new Insets(20)); // Padding inside the white container
        container.setEffect(new DropShadow(5, Color.GRAY));

        // Set equal margins for the white container
        StackPane.setMargin(container, new Insets(60));

        // Create a StackPane for centering the container
        StackPane root = new StackPane(container);
        root.setStyle("-fx-background-color: #f8f8d8;");

        // Create the scene and set it on the stage
        Scene scene = new Scene(root, 1024, 768); // Adjusted scene size
        primaryStage.setMaximized(true);
        primaryStage.setTitle("Model View");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private VBox createFolderBox(File file) {
        // Create an ImageView for the folder icon
        File folderImageFile = new File(FOLDER_IMAGE_PATH);
        Image folderImage = new Image(folderImageFile.toURI().toString(), 80, 80, true, true);
        ImageView folderImageView = new ImageView(folderImage);

        // Create a label for the folder name
        Label label = new Label(file.getName());
        label.setFont(new Font("Arial", 14));
        label.setAlignment(Pos.CENTER);

        // Create a VBox to hold the image and label
        VBox vBox = new VBox(5, folderImageView, label);
        vBox.setAlignment(Pos.CENTER); // Center the content

        // Add mouse click event handler to the VBox
        vBox.setOnMouseClicked(event -> openModel(file));

        return vBox;
    }

    private void openModel(File folder) {
        // Create a new stage for the new window
        Stage contentStage = new Stage();
        contentStage.setTitle("Folder: " + folder.getName());

        // Create a VBox for the layout
        VBox vBox = new VBox(20);
        vBox.setPadding(new Insets(20));
        vBox.setAlignment(Pos.TOP_CENTER);
        vBox.setStyle("-fx-background-color: #f8f8d8;");

        // Create a label for the title
        Label titleLabel = new Label("Representation XML du modèle choisi:");
        titleLabel.setFont(new Font("Arial", 20));
        titleLabel.setStyle("-fx-font-weight: bold;");
        vBox.getChildren().add(titleLabel);

        // Add ImageView if a .jpg file exists
        File imageFile = findFileByExtension(folder, ".jpg");
        if (imageFile != null) {
            ImageView imageView = new ImageView(new Image(imageFile.toURI().toString()));
            imageView.setFitWidth(600);
            imageView.setFitHeight(300);
            imageView.setPreserveRatio(true);
            imageView.setStyle("-fx-border-radius: 20; -fx-background-radius: 20;");
            vBox.getChildren().add(imageView);
        }

        // Add XML content if an XML file exists
        File xmlFile = findFileByExtension(folder, ".xml", "output.xml");
        if (xmlFile != null) {
            try {
                String xmlContent = new String(Files.readAllBytes(Paths.get(xmlFile.getAbsolutePath())));
                Label xmlLabel = new Label(xmlContent);
                xmlLabel.setFont(new Font("Arial", 14));
                xmlLabel.setWrapText(true);

                // Create a white container for the XML content
                VBox xmlContainer = new VBox(xmlLabel);
                xmlContainer.setPadding(new Insets(20));
                xmlContainer.setStyle("-fx-background-color: white; -fx-border-radius: 20; -fx-background-radius: 20;");
                xmlContainer.setEffect(new DropShadow(5, Color.GRAY));

                // Set horizontal margins to center the XML container
                VBox.setMargin(xmlContainer, new Insets(0, 200, 0, 200));

                vBox.getChildren().add(xmlContainer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Create a button for identifying the language
        Button identifyButton = new Button("Identify the language");
        identifyButton.setStyle("-fx-background-color: black; -fx-text-fill: white; -fx-font-size: 14px; -fx-border-radius: 10; -fx-background-radius: 10;");
        identifyButton.setPadding(new Insets(10, 20, 10, 20));

        // Create an HBox to hold the button
        HBox buttonContainer = new HBox(10); // Spacing of 10 between button and label
        buttonContainer.setAlignment(Pos.CENTER);
        //buttonContainer.setSpacing(20);

        // Add the button's action to call the identifyLanguage method and show the result in a custom pop-up
        identifyButton.setOnAction(e -> {
            reconnaissance detector = new reconnaissance();
            String detectedLanguage = detector.detectLanguage(xmlFile.getAbsolutePath());

            // Create labels for the results
            Label languageLabel = new Label(detectedLanguage);

            if (detectedLanguage.toLowerCase().contains("wrong")) {
                languageLabel.setTextFill(Color.RED);
                languageLabel.setStyle("-fx-font-weight: bold;");
            } else {
                languageLabel.setTextFill(Color.GREEN);
                languageLabel.setStyle("-fx-font-weight: bold;");
            }

            // Create a VBox to hold the labels
            VBox dialogVBox = new VBox(10, languageLabel);

            // Create a custom dialog to display the results
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Identification Result");
            dialog.getDialogPane().setContent(dialogVBox);
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK);

            // Add some spacing around the dialog content
            dialog.getDialogPane().setPadding(new Insets(20));
            dialog.getDialogPane().getScene().getWindow().setWidth(400); // Set dialog width
            dialog.getDialogPane().getScene().getWindow().setHeight(200); // Set dialog height

            // Show the dialog and handle the result
            dialog.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    // If OK is clicked, replace the identifyButton with verifyButton
                    buttonContainer.getChildren().remove(identifyButton);
                    Button verifyButton = new Button("Verify the model");
                    verifyButton.setStyle("-fx-background-color: black; -fx-text-fill: white; -fx-font-size: 14px; -fx-border-radius: 10; -fx-background-radius: 10;");
                    verifyButton.setPadding(new Insets(10, 20, 10, 20));

                    // Create a label for validation results
                    Label validationLabel = new Label();
                    validationLabel.setFont(new Font("Arial", 14));
                    buttonContainer.getChildren().add(verifyButton);

                    // Add the verifyButton's action to call the validateFile method
                    verifyButton.setOnAction(ev -> {
                        verif_ECPML verifier = new verif_ECPML();
                        String validationResult = verifier.validateFile(xmlFile.getAbsolutePath());

                        // Update the validationLabel with the validation results
                        validationLabel.setText(validationResult);
                        validationLabel.setStyle("-fx-font-weight: bold;");
                        System.out.println(validationResult);

                        if (validationResult.trim().equalsIgnoreCase("Ce modèle est CORRECT")) {
                            validationLabel.setTextFill(Color.GREEN);

                            Button translateButton = new Button("Traduire le modéle");

                            translateButton.setStyle("-fx-background-color: black; -fx-text-fill: white; -fx-font-size: 14px; -fx-border-radius: 10; -fx-background-radius: 10;");
                            translateButton.setPadding(new Insets(10, 20, 10, 20));
                            HBox.setMargin(translateButton, new Insets(0,0,0,80));

                            buttonContainer.getChildren().addAll(validationLabel, translateButton);

                            translateButton.setOnAction(translateEvent -> {
                                XMLParser traducteur = new XMLParser();
                                traducteur.processFile(xmlFile);

                                Scene translationScene = createTranslationPage(contentStage, folder);
                                contentStage.setScene(translationScene);

                            });

                            // Check if validationLabel is already added, if not add it to buttonContainer
                            if (!buttonContainer.getChildren().contains(validationLabel)) {
                                buttonContainer.getChildren().add(validationLabel);
                            }
                        } else {
                            // Add a new dialog
                            Dialog<ButtonType> resultDialog = new Dialog<>();
                            resultDialog.setTitle("Validation Result");

                            // Create a new label to display the validation result
                            Label resultLabel = new Label(validationResult);
                            resultLabel.setTextFill(Color.DARKRED);
                            resultLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

                            // Create a VBox to hold the label and button
                            VBox resultDialogVBox = new VBox();
                            resultDialogVBox.setPadding(new Insets(20));
                            resultDialogVBox.setSpacing(10); // Spacing between elements

                            // Create an HBox to hold the "More Information" button at the bottom
                            HBox moreInfoHBox = new HBox();
                            moreInfoHBox.setAlignment(Pos.BOTTOM_RIGHT);
                            moreInfoHBox.setPadding(new Insets(10, 0, 0, 0)); // Padding to add some space above the button

                            // Create the "More Information" button
                            Button moreInfoButton = new Button("More Information");
                            moreInfoButton.setStyle("-fx-background-color: black; -fx-text-fill: white; -fx-font-size: 12px; -fx-font-weight: bold;");
                            moreInfoButton.setPadding(new Insets(7, 7, 7, 7));

                            // Add action to the button
                            moreInfoButton.setOnAction(moreInfoEvent -> {
                                try {
                                    File file = new File("C:\\Users\\vappyq\\Documents\\ProjetM2\\ECPML\\modeles-in-XML\\Validation_ECPML.txt");
                                    if (Desktop.isDesktopSupported()) {
                                        Desktop.getDesktop().open(file);
                                    } else {
                                        System.out.println("Desktop is not supported");
                                    }
                                    resultDialog.close();
                                } catch (IOException ie) {
                                    ie.printStackTrace();
                                }
                            });

                            // Add the button to the HBox
                            moreInfoHBox.getChildren().add(moreInfoButton);

                            // Add the label and HBox to the VBox
                            resultDialogVBox.getChildren().addAll(resultLabel, moreInfoHBox);

                            // Set the content of the dialog
                            resultDialog.getDialogPane().setContent(resultDialogVBox);
                            resultDialog.getDialogPane().getButtonTypes().add(ButtonType.OK);

                            // Show the dialog
                            resultDialog.showAndWait();
                        }
                    });
                }
            });
        });

        // Add the button to the HBox
        buttonContainer.getChildren().addAll(identifyButton);
        // Add the HBox to the VBox
        vBox.getChildren().add(buttonContainer);

        // Create a ScrollPane for the main content
        ScrollPane mainScrollPane = new ScrollPane(vBox);
        mainScrollPane.setFitToWidth(true);
        mainScrollPane.setPannable(true);
        mainScrollPane.setStyle("-fx-background: white; -fx-background-color: transparent;");
        mainScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        mainScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        // Create the scene and set it on the new stage
        Scene scene = new Scene(mainScrollPane, 800, 600);
        contentStage.setScene(scene);
        contentStage.show();
    }

    private Scene createTranslationPage(Stage stage, File folder) {
        // Create a VBox for the new layout
        VBox translationPage = new VBox(20);
        translationPage.setPadding(new Insets(20));
        translationPage.setAlignment(Pos.CENTER);
        translationPage.setStyle("-fx-background-color: #f8f8d8;");

        // Add a label for the title
        Label titleLabel = new Label("Representation XML du modèle choisi:");
        titleLabel.setFont(new Font("Arial", 20));
        titleLabel.setStyle("-fx-font-weight: bold;");
        translationPage.getChildren().add(titleLabel);

        // Add ImageView if a .jpg file exists
        File imageFile = findFileByExtension(folder, ".jpg");
        if (imageFile != null) {
            ImageView imageView = new ImageView(new Image(imageFile.toURI().toString()));
            imageView.setFitWidth(600);
            imageView.setFitHeight(300);
            imageView.setPreserveRatio(true);
            imageView.setStyle("-fx-border-radius: 20; -fx-background-radius: 20;");
            translationPage.getChildren().add(imageView);
        }

        // Create an HBox to hold the two XML containers
        HBox xmlContainerHBox = new HBox(20);
        xmlContainerHBox.setStyle("-fx-background-color: #f8f8d8;");
        xmlContainerHBox.setSpacing(100);
        xmlContainerHBox.setAlignment(Pos.CENTER);
        //xmlContainerHBox.setLayoutY(400);
        //xmlContainerHBox.setPadding(new inset(20,0,0,0));

        // Create the original XML container
        VBox originalXMLContainer = createXMLContainer(folder, ".xml", "output.xml");
        if (originalXMLContainer != null) {
            HBox.setHgrow(originalXMLContainer, Priority.ALWAYS);
            originalXMLContainer.setMaxWidth(400);
            originalXMLContainer.setPadding(new Insets(20));
            xmlContainerHBox.getChildren().add(originalXMLContainer);
        }

        // Create the translated XML container
        VBox translatedXMLContainer = createXMLContainer(folder, "output.xml");
        if (translatedXMLContainer != null) {
            HBox.setHgrow(translatedXMLContainer, Priority.ALWAYS);
            translatedXMLContainer.setMaxWidth(400);
            translatedXMLContainer.setPadding(new Insets(20));
            xmlContainerHBox.getChildren().add(translatedXMLContainer);
        }

        // Add the HBox to the translationPage
        translationPage.getChildren().add(xmlContainerHBox);

        Button verifyButton = new Button("Verify the new model");
        verifyButton.setStyle("-fx-background-color: black; -fx-text-fill: white; -fx-font-size: 14px; -fx-border-radius: 10; -fx-background-radius: 10;");
        verifyButton.setPadding(new Insets(10, 20, 10, 20));

        translationPage.getChildren().add(verifyButton);

        // Add action to the verify button
        verifyButton.setOnAction(e -> {
            try {
                // Call the validateXML method from verifPOEML2
                String validationResult1 = verifPOEML2.validateXML(new File(folder, "output.xml"));

                // Create a dialog to display the validation result
                Dialog<ButtonType> dialog = new Dialog<>();
                dialog.setTitle("Validation Result");
                dialog.setHeaderText("Validation Result for the new model:");



                Label resultLabel = new Label(validationResult1);
                resultLabel.setWrapText(true);
                dialog.getDialogPane().setContent(resultLabel);

                // Create a VBox to hold the label and button
                VBox resultDialogVBox1 = new VBox();
                resultDialogVBox1.setPadding(new Insets(20));
                resultDialogVBox1.setSpacing(10);

                if (validationResult1.trim().equalsIgnoreCase("La traduction est CORRECT !")) {
                    resultLabel.setTextFill(Color.GREEN);
                    resultDialogVBox1.getChildren().add(resultLabel);
                }else {
                    // Create the "More Information" button
                    Button moreInfoButton1 = new Button("More Information");
                    moreInfoButton1.setStyle("-fx-background-color: black; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold;");
                    moreInfoButton1.setPadding(new Insets(5, 5, 5, 5));

                    // Add action to the button
                    moreInfoButton1.setOnAction(moreInfoEvent -> {
                        try {
                            File file = new File("C:\\Users\\vappyq\\Documents\\ProjetM2\\ECPML\\modeles-in-XML\\Validation_POEML.txt");
                            if (Desktop.isDesktopSupported()) {
                                Desktop.getDesktop().open(file);
                            } else {
                                System.out.println("Desktop is not supported");
                            }
                            dialog.close();
                        } catch (IOException ie) {
                            ie.printStackTrace();
                        }
                    });
                    resultDialogVBox1.getChildren().addAll(resultLabel, moreInfoButton1);
                }


                // Set the content of the dialog
                dialog.getDialogPane().setContent(resultDialogVBox1);
                dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);

                dialog.showAndWait();
            } catch (Exception ex) {
                ex.printStackTrace();

            }
        });

        // Create a ScrollPane for the main content
        ScrollPane mainScrollPane = new ScrollPane(translationPage);
        mainScrollPane.setFitToWidth(true);
        mainScrollPane.setPannable(true);
        mainScrollPane.setStyle("-fx-background: white; -fx-background-color: transparent;");
        mainScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        mainScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        // Create a new scene with the new layout
        Scene newScene = new Scene(mainScrollPane, 800, 600);
        return newScene;
    }


    // Overloaded method to handle two arguments
    private VBox createXMLContainer(File folder, String extension) {
        return createXMLContainer(folder, extension, null);
    }

    // Method to handle three arguments
    private VBox createXMLContainer(File folder, String extension, String excludeName) {
        File xmlFile = excludeName == null ? findFileByExtension(folder, extension) : findFileByExtension(folder, extension, excludeName);
        if (xmlFile != null) {
            try {
                String xmlContent = new String(Files.readAllBytes(Paths.get(xmlFile.getAbsolutePath())));
                Label xmlLabel = new Label(xmlContent);
                xmlLabel.setFont(new Font("Arial", 14));
                xmlLabel.setWrapText(true);

                // Create a white container for the XML content
                VBox xmlContainer = new VBox(xmlLabel);
                xmlContainer.setPadding(new Insets(50));
                xmlContainer.setStyle("-fx-background-color: white; -fx-border-radius: 20; -fx-background-radius: 20;");
                xmlContainer.setEffect(new DropShadow(5, Color.GRAY));
                // Increase preferred size for the container
               // xmlContainer.setPrefSize(800, 600);
                xmlContainer.setMinWidth(450);

                // Set larger horizontal margins to center the XML container
                VBox.setMargin(xmlContainer, new Insets(100, 100, 100, 100));
                return xmlContainer;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private File findFileByExtensions(File folder, String... extensions) {
        for (File file : Objects.requireNonNull(folder.listFiles())) {
            for (String extension : extensions) {
                if (file.getName().endsWith(extension)) {
                    return file;
                }
            }
        }
        return null;
    }

    private File findFileByExtension(File folder, String extension) {
        for (File file : Objects.requireNonNull(folder.listFiles())) {
            if (file.getName().endsWith(extension)) {
                return file;
            }
        }
        return null;
    }

    private File findFileByExtension(File folder, String extension, String excludeName) {
        for (File file : Objects.requireNonNull(folder.listFiles())) {
            if (file.getName().endsWith(extension) && !file.getName().equalsIgnoreCase(excludeName)) {
                return file;
            }
        }
        return null;
    }

    private File findFileByName(File folder, String fileName) {
        // Check if the folder is not null and is a directory
        if (folder != null && folder.isDirectory()) {
            // Iterate over the list of files in the directory
            for (File file : Objects.requireNonNull(folder.listFiles())) {
                // Check if the file name matches the specified file name
                if (file.getName().equalsIgnoreCase(fileName)) {
                    return file;
                }
            }
        }
        // Return null if no matching file is found
        return null;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
