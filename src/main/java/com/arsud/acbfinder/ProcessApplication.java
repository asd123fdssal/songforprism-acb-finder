package com.arsud.acbfinder;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Stream;

public class ProcessApplication extends Application {
    private static final int LOG_LIMIT = 50;
    private static final String SEARCH_PATTERN = "ACB Format/PC Ver.1.40.4 Build:";
    private static final String WORK_DIR = Paths.get(System.getProperty("user.dir"), "process").toString();
    private static final List<String> CHARACTER_NAMES = List.of(
            "mano", "hiori", "meguru", "kogane", "mamimi", "sakuya", "yuika", "kiriko", "kaho",
            "chiyoko", "juri", "rinze", "natsuha", "amana", "tenka", "chiyuki", "asahi", "fuyuko",
            "mei", "toru", "madoka", "koito", "hinana", "nichika", "mikoto", "luca", "hana", "haruki"
    );

    private TextField gameFolderField;
    private TextArea logArea;
    private ProgressBar progressBar;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Shinycolors Songforprism ACB Finder");

        gameFolderField = new TextField();
        gameFolderField.setPrefWidth(400);
        gameFolderField.setPromptText("Game folder path");

        Button gameFolderButton = new Button("Select Game Folder");
        gameFolderButton.setOnAction(_ -> selectFolder(gameFolderField, primaryStage));

        VBox folderBox = new VBox(10, new HBox(10, gameFolderField, gameFolderButton));

        Button decryptButton = new Button("Decrypt");
        decryptButton.setOnAction(_ -> executeTask(this::processFiles));

        Button extractAcbButton = new Button("Extract ACB Files");
        extractAcbButton.setOnAction(e -> executeTask(this::extractAcbFiles));

        Button categorizeWavButton = new Button("Categorize WAV Files");
        categorizeWavButton.setOnAction(e -> executeTask(this::categorizeWavFiles));

        HBox buttonBox = new HBox(10, decryptButton, extractAcbButton, categorizeWavButton);

        logArea = new TextArea();
        logArea.setEditable(false);
        logArea.setPrefHeight(200);

        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(580);

        VBox root = new VBox(10, folderBox, buttonBox, logArea, progressBar);
        root.setPadding(new Insets(10));

        Scene scene = new Scene(root, 600, 320);
        scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void decryptFiles(String workFolderPath) {
        try (Stream<Path> paths = Files.walk(Paths.get(workFolderPath))) {
            List<Path> files = paths.filter(Files::isRegularFile).toList();
            for (int i = 0; i < files.size(); i++) {
                Path targetPath = files.get(i);
                byte[] data = Files.readAllBytes(targetPath);
                if (data.length >= 4 && data[0] == (byte) 0xBA && data[1] == (byte) 0x01) {
                    Files.write(targetPath, Arrays.copyOfRange(data, 4, data.length), StandardOpenOption.TRUNCATE_EXISTING);
                    appendLog("Decrypted: " + targetPath);
                }
                updateProgress(i + 1, files.size());
            }
            appendLog("Decrypt Done!");
        } catch (IOException e) {
            appendLog("Decrypt Error: " + e.getMessage());
        }
    }

    private void extractAcbFiles() {
        String dateFolder = new SimpleDateFormat("yyyyMMdd").format(new Date());
        Path originPath = Paths.get(WORK_DIR, dateFolder, "origin");
        Path acbPath = Paths.get(WORK_DIR, dateFolder, "origin", "acb");

        renameAcbFiles(originPath.toFile(), acbPath.toFile());
        renameAcbFiles2(acbPath.toFile());
    }

    private void renameAcbFiles(File baseDir, File targetDir) {
        if (!baseDir.exists() || !baseDir.isDirectory()) return;
        if (!targetDir.exists() && !targetDir.mkdirs()) {
            appendLog("Failed to create ACB directory: " + targetDir.getAbsolutePath());
            return;
        }

        File[] files = baseDir.listFiles();
        if (files == null) return;

        int totalFiles = files.length;
        for (int i = 0; i < totalFiles; i++) {
            File file = files[i];
            if (file.isDirectory()) {
                renameAcbFiles(file, targetDir);
            } else {
                try (FileInputStream fis = new FileInputStream(file)) {
                    byte[] header = new byte[4];
                    if (fis.read(header) != 4) continue;
                    fis.close();
                    if (new String(header).equals("@UTF")) {
                        File newFile = new File(targetDir, file.getName() + ".acb");
                        if (file.renameTo(newFile)) {
                            appendLog("Renamed and moved: " + file.getAbsolutePath() + " -> " + newFile.getAbsolutePath());
                        }
                    }
                } catch (IOException e) {
                    appendLog("Error processing " + file.getAbsolutePath() + ": " + e.getMessage());
                }
            }
            updateProgress(i + 1, totalFiles);
        }
    }

    private void renameAcbFiles2(File baseDir) {
        if (!baseDir.exists() || !baseDir.isDirectory()) return;

        File[] files = baseDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".acb"));
        if (files == null) return;

        int totalFiles = files.length;
        for (int i = 0; i < totalFiles; i++) {
            File file = files[i];
            try (FileInputStream fis = new FileInputStream(file)) {
                fis.close();
                byte[] data = Files.readAllBytes(file.toPath());
                String content = new String(data, StandardCharsets.UTF_8);

                int startIndex = content.indexOf(SEARCH_PATTERN);
                if (startIndex == -1) {
                    appendLog("Pattern not found in: " + file.getName());
                    continue;
                }

                int titleStart = content.indexOf("\0", startIndex) + 1;
                int titleEnd = content.indexOf("\0", titleStart);
                if (titleStart > 0 && titleEnd > titleStart) {
                    String newFileName = content.substring(titleStart, titleEnd).trim().replaceAll("[^a-zA-Z0-9_ ]", "_");
                    File newFile = new File(file.getParent(), newFileName + ".acb");
                    if (file.renameTo(newFile)) {
                        appendLog("Renamed: " + file.getAbsolutePath() + " -> " + newFile.getAbsolutePath());
                    }
                }
            } catch (IOException e) {
                appendLog("Error reading file: " + file.getAbsolutePath());
            }
            updateProgress(i + 1, totalFiles);
        }
    }

    private void categorizeWavFiles() {
        String workFolderPath = Paths.get(WORK_DIR, new SimpleDateFormat("yyyyMMdd").format(new Date()), "origin").toString();
        File baseDir = new File(workFolderPath, "wav");
        if (!baseDir.exists() || !baseDir.isDirectory()) return;

        File[] files = baseDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".wav"));
        if (files == null) return;

        for (File file : files) {
            String folderName = "etc";
            for (String character : CHARACTER_NAMES) {
                if (file.getName().toLowerCase().contains(character)) {
                    folderName = character;
                    break;
                }
            }

            File newFolder = new File(file.getParent(), folderName);
            if (!newFolder.exists()) newFolder.mkdir();

            File newFileLocation = new File(newFolder, file.getName());
            try {
                Files.move(file.toPath(), newFileLocation.toPath(), StandardCopyOption.REPLACE_EXISTING);
                appendLog("Moved: " + file.getAbsolutePath() + " -> " + newFileLocation.getAbsolutePath());
            } catch (IOException e) {
                appendLog("Failed to move: " + file.getAbsolutePath() + " - " + e.getMessage());
            }
        }
    }

    private void selectFolder(TextField targetField, Stage stage) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedFolder = directoryChooser.showDialog(stage);
        if (selectedFolder != null) {
            targetField.setText(selectedFolder.getAbsolutePath());
        }
    }

    private void executeTask(Runnable task) {
        new Thread(task).start();
    }

    private void processFiles() {
        String workFolderPath = moveFiles();
        if (workFolderPath != null) {
            decryptFiles(workFolderPath);
        }
    }

    private String moveFiles() {
        String gameFolderPath = gameFolderField.getText();
        Path path = Paths.get(gameFolderPath);
        if (gameFolderPath.isEmpty() || !Files.exists(path)) {
            appendLog("Resource folder does not exist.");
            return null;
        }

        String dateFolder = new SimpleDateFormat("yyyyMMdd").format(new Date());
        String workFolderPath = Paths.get(WORK_DIR, dateFolder, "origin").toString();

        try {
            Files.createDirectories(Paths.get(workFolderPath));
            List<Path> files;
            try (Stream<Path> paths = Files.walk(path)) {
                files = paths.filter(Files::isRegularFile).toList();
            }

            for (int i = 0; i < files.size(); i++) {
                Path targetPath = Paths.get(workFolderPath, files.get(i).getFileName().toString());
                Files.copy(files.get(i), targetPath, StandardCopyOption.REPLACE_EXISTING);
                appendLog("Moved: " + targetPath);
                updateProgress(i + 1, files.size());
            }
            return workFolderPath;
        } catch (IOException e) {
            appendLog("File move error: " + e.getMessage());
            return null;
        }
    }

    private void appendLog(String message) {
        Platform.runLater(() -> {
            String[] lines = logArea.getText().split("\n");
            if (lines.length >= LOG_LIMIT) {
                logArea.setText(String.join("\n", Arrays.copyOfRange(lines, lines.length - LOG_LIMIT + 1, lines.length)));
            }
            logArea.appendText(message + "\n");
        });
    }

    private void updateProgress(int current, int total) {
        Platform.runLater(() -> progressBar.setProgress((double) current / total));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
