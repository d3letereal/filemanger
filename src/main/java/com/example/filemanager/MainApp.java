package com.example.filemanager;

import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.*;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class MainApp extends Application {

    private ListView<File> fileListView;
    private TreeView<File> fileTree;
    private File currentDirectory;
    private Set<File> pinnedFolders;
    private double xOffset = 0;
    private double yOffset = 0;
    private VBox topBar;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        pinnedFolders = new HashSet<>();
        BorderPane root = new BorderPane();

        fileTree = createFileTree();
        fileListView = new ListView<>();
        fileListView.setPrefWidth(400);

        fileListView.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                File selected = fileListView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    if (selected.isDirectory()) {
                        currentDirectory = selected;
                        updateFileList(currentDirectory);
                    } else {
                        openFileViewer(selected);
                    }
                }
            }
        });

        topBar = new VBox();
        HBox windowControls = new HBox(10);
        windowControls.setPadding(new Insets(5));
        windowControls.setAlignment(Pos.TOP_RIGHT);

        Button minimize = new Button("–");
        Button close = new Button("×");

        minimize.setOnAction(e -> stage.setIconified(true));
        close.setOnAction(e -> stage.close());

        styleWindowControl(minimize);
        styleWindowControl(close);

        windowControls.getChildren().addAll(minimize, close);
        topBar.getChildren().add(windowControls);

        HBox actionBar = new HBox(10);
        actionBar.setPadding(new Insets(10));
        actionBar.setStyle("-fx-background-color: transparent;");

        Button addFileButton = new Button("Add File");
        Button deleteFileButton = new Button("Delete File");
        Button renameFileButton = new Button("Rename File");
        Button listViewButton = new Button("List View");
        Button gridViewButton = new Button("Grid View");

        addFileButton.setOnAction(e -> addFile());
        deleteFileButton.setOnAction(e -> deleteFile());
        renameFileButton.setOnAction(e -> renameFile());
        listViewButton.setOnAction(e -> setListView());
        gridViewButton.setOnAction(e -> setGridView());

        styleButton(addFileButton);
        styleButton(deleteFileButton);
        styleButton(renameFileButton);
        styleButton(listViewButton);
        styleButton(gridViewButton);

        actionBar.getChildren().addAll(addFileButton, deleteFileButton, renameFileButton, listViewButton, gridViewButton);
        topBar.getChildren().add(actionBar);

        VBox pinnedSection = createPinnedFoldersSection();

        SplitPane splitPane = new SplitPane();
        splitPane.getItems().addAll(pinnedSection, fileTree, fileListView);
        splitPane.setDividerPositions(0.2);

        root.setTop(topBar);
        root.setCenter(splitPane);

        Scene scene = new Scene(root, 900, 550);
        scene.setFill(Color.TRANSPARENT);
        applyGlobalStyle(scene);

        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setScene(scene);
        stage.setTitle("Apollo File Explorer");

        // Make the window draggable
        addCustomDrag(stage, topBar);

        stage.show();
    }

    private void updateFileList(File directory) {
        fileListView.getItems().clear();
        File[] files = directory.listFiles();
        if (files != null) {
            fileListView.getItems().addAll(files);
        }
    }

    private TreeView<File> createFileTree() {
        File rootFile = File.listRoots()[0];
        TreeItem<File> rootItem = new TreeItem<>(rootFile);
        rootItem.setExpanded(true);
        loadChildren(rootItem);

        TreeView<File> tree = new TreeView<>(rootItem);
        tree.setShowRoot(true);

        tree.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                currentDirectory = newVal.getValue();
                updateFileList(currentDirectory);
            }
        });

        return tree;
    }

    private void loadChildren(TreeItem<File> item) {
        File dir = item.getValue();
        File[] children = dir.listFiles(File::isDirectory);
        if (children != null) {
            for (File child : children) {
                TreeItem<File> childItem = new TreeItem<>(child);
                item.getChildren().add(childItem);
            }
        }
    }

    private VBox createPinnedFoldersSection() {
        VBox pinnedSection = new VBox(10);
        pinnedSection.setPadding(new Insets(10));
        pinnedSection.setStyle("-fx-background-color: transparent;");

        File downloads = new File(System.getProperty("user.home") + "/Downloads");
        File desktop = new File(System.getProperty("user.home") + "/Desktop");
        File pictures = new File(System.getProperty("user.home") + "/Pictures");

        Button pinDownloadsButton = new Button(downloads.getAbsolutePath());
        Button pinDesktopButton = new Button(desktop.getAbsolutePath());
        Button pinPicturesButton = new Button(pictures.getAbsolutePath());

        pinDownloadsButton.setOnAction(e -> updateFileList(downloads));
        pinDesktopButton.setOnAction(e -> updateFileList(desktop));
        pinPicturesButton.setOnAction(e -> updateFileList(pictures));

        styleButton(pinDownloadsButton);
        styleButton(pinDesktopButton);
        styleButton(pinPicturesButton);

        pinnedSection.getChildren().addAll(pinDownloadsButton, pinDesktopButton, pinPicturesButton);
        return pinnedSection;
    }

    private void addFile() {
        if (currentDirectory == null || !currentDirectory.isDirectory()) return;

        TextInputDialog dialog = new TextInputDialog("newfile.txt");
        dialog.setHeaderText("Create New File");
        dialog.setContentText("File name:");
        dialog.showAndWait().ifPresent(name -> {
            if (name.trim().isEmpty()) {
                showError("File name cannot be empty.");
                return;
            }

            File newFile = new File(currentDirectory, name);
            try {
                if (newFile.exists()) {
                    showError("File already exists.");
                } else if (newFile.createNewFile()) {
                    updateFileList(currentDirectory);
                } else {
                    showError("Unknown error creating file.");
                }
            } catch (IOException ex) {
                showError("Error: " + ex.getMessage());
            }
        });
    }

    private void deleteFile() {
        File selected = fileListView.getSelectionModel().getSelectedItem();
        if (selected != null && selected.exists()) {
            boolean deleted = selected.delete();
            if (deleted) {
                updateFileList(currentDirectory);
            } else {
                showError("Could not delete file.");
            }
        }
    }

    private void renameFile() {
        File selected = fileListView.getSelectionModel().getSelectedItem();
        if (selected != null && selected.exists()) {
            TextInputDialog dialog = new TextInputDialog(selected.getName());
            dialog.setHeaderText("Rename File");
            dialog.setContentText("New name:");

            dialog.showAndWait().ifPresent(newName -> {
                if (!newName.trim().isEmpty()) {
                    File renamedFile = new File(selected.getParent(), newName);
                    boolean renamed = selected.renameTo(renamedFile);
                    if (renamed) {
                        updateFileList(currentDirectory);
                    } else {
                        showError("Error renaming file.");
                    }
                }
            });
        }
    }

    private void openFileViewer(File file) {
        Stage viewer = new Stage();
        viewer.setTitle("Viewing: " + file.getName());

        BorderPane layout = new BorderPane();
        layout.setStyle("-fx-background-color: #1e1e1e;");

        if (file.getName().endsWith(".png") || file.getName().endsWith(".jpg")) {
            ImageView imageView = new ImageView(new javafx.scene.image.Image(file.toURI().toString()));
            imageView.setPreserveRatio(true);
            imageView.setFitWidth(800);
            imageView.setFitHeight(600);
            imageView.setSmooth(true);
            layout.setCenter(imageView);
        } else {
            try {
                String content = java.nio.file.Files.readString(file.toPath());
                TextArea area = new TextArea(content);
                area.setEditable(false);
                area.setWrapText(true);
                layout.setCenter(area);
            } catch (IOException e) {
                showError("Can't open file.");
            }
        }

        Scene scene = new Scene(layout, 800, 600);
        viewer.setScene(scene);
        viewer.show();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.showAndWait();
    }

    private void styleButton(Button button) {
        button.setStyle("-fx-background-color: #61dafb; -fx-text-fill: black; -fx-font-weight: bold; -fx-background-radius: 6;");
    }

    private void styleWindowControl(Button button) {
        button.setStyle("-fx-background-color: #444; -fx-text-fill: white; -fx-background-radius: 10; -fx-font-size: 14px; -fx-padding: 4 8;");
    }

    private void applyGlobalStyle(Scene scene) {
        scene.getRoot().setStyle("-fx-font-family: 'Segoe UI'; -fx-background-color: transparent; -fx-text-fill: white;");
        fileListView.setStyle("-fx-control-inner-background: #2c2f33; -fx-text-fill: white;");
        fileTree.setStyle("-fx-control-inner-background: #2c2f33; -fx-text-fill: white;");
    }

    private void setListView() {
        // Switch to ListView
        fileListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(File file, boolean empty) {
                super.updateItem(file, empty);
                if (empty || file == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(file.getName());
                }
            }
        });
    }

    private void setGridView() {
        // Switch to GridView (add icons for files)
        fileListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(File file, boolean empty) {
                super.updateItem(file, empty);
                if (empty || file == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox hbox = new HBox();
                    hbox.setSpacing(10);
                    Label label = new Label(file.getName());
                    label.setStyle("-fx-font-size: 14px;");
                    hbox.getChildren().add(label);
                    setGraphic(hbox);
                }
            }
        });
    }

    private void addCustomDrag(Stage stage, VBox dragArea) {
        // Make the window draggable
        dragArea.setOnMousePressed((MouseEvent e) -> {
            xOffset = e.getSceneX();
            yOffset = e.getSceneY();
            stage.setOpacity(0.8);  // Add some transparency while dragging
        });

        dragArea.setOnMouseDragged((MouseEvent e) -> {
            stage.setX(e.getScreenX() - xOffset);
            stage.setY(e.getScreenY() - yOffset);
        });

        dragArea.setOnMouseReleased((MouseEvent e) -> {
            stage.setOpacity(1.0);  // Reset opacity after dragging
        });
    }
}
