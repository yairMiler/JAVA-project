package frontEnd;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import backEnd.TicTacToeBackend;
import DB.DatabaseHandler;
import network.Client;


import java.io.File;
import java.sql.SQLException;
import java.util.List;

public class MainApp extends Application {
    private final TicTacToeBackend backend = new TicTacToeBackend();
    private final Button[] cells = new Button[9];
    private Label status = new Label("Welcome!");
    private DatabaseHandler db;
    private boolean onlineSaveMode = false;

    // Scenes
    private Scene menuScene;
    private Scene gameScene;

    // Music
    private MediaPlayer mediaPlayer;
    private boolean musicOn = true;

    private Client client;

    @Override
    public void start(Stage primaryStage) {
        // ==== DB init ====
        try {
            db = new DatabaseHandler("C:/Games/TicTacToe/tictactoe.db");
        } catch (SQLException e) {
            db = null;
            System.err.println("DB init failed: " + e.getMessage());
        }

        // ==== MUSIC init ====
        try {
            Media media = new Media(new File("C:/Games/TicTacToe/music.mp3").toURI().toString()); // put music.mp3 in project root
            mediaPlayer = new MediaPlayer(media);
            mediaPlayer.setCycleCount(MediaPlayer.INDEFINITE); // loop
            mediaPlayer.setVolume(0.3); // default 30%
            mediaPlayer.play();
        } catch (Exception ex) {
            System.err.println("Music failed: " + ex.getMessage());
        }

        // ==== MENU screen ====
        Button offlineBtn = new Button("🎮 Play (offline vs CPU)");
        Button onlineBtn = new Button("🌐 Play (online-save mode)");
        Button leaderboardBtn = new Button("🏆 Show Leaderboard");
        Button musicBtn = new Button("🔊 Music ON");
        Button settingsBtn = new Button("⚙️ Settings");

        // Big top buttons
        Font bigFont = Font.font("Arial", 22);
        offlineBtn.setFont(bigFont);
        onlineBtn.setFont(bigFont);
        leaderboardBtn.setFont(bigFont);

        // Smaller bottom buttons
        Font smallFont = Font.font("Arial", 10);
        musicBtn.setFont(smallFont);
        settingsBtn.setFont(smallFont);

        // Actions
        offlineBtn.setOnAction(e -> {
            onlineSaveMode = false;
            backend.reset();
            status.setText("Offline: you are X");
            updateUI();
            primaryStage.setScene(gameScene);
        });
        onlineBtn.setOnAction(e -> {
            onlineSaveMode = true;
            backend.reset();
            status.setText("Connecting to server...");
            updateUI();
            primaryStage.setScene(gameScene);

            // connect asynchronously
            new Thread(() -> {
                try {
                    client = new Client("localhost", 5000, (board, winner) -> {
                        // callback comes from network thread
                        Platform.runLater(() -> {
                            // sync board + winner into backend
                            backend.setState(board, winner);

                            // update UI with styling
                            updateUI();

                            // check if game ended
                            if (winner != ' ') {
                                handleEnd();  // reuse existing logic!
                            }
                        });
                    });
                    Platform.runLater(() -> status.setText("Connected to server!"));
                } catch (Exception ex) {
                    ex.printStackTrace();
                    Platform.runLater(() -> {
                        status.setText("⚠️ Could not connect to server");
                        onlineSaveMode = false;
                    });
                }
            }).start();
        });

        leaderboardBtn.setOnAction(e -> showLeaderboard());

        musicBtn.setOnAction(e -> toggleMusic(musicBtn));
        settingsBtn.setOnAction(e -> showSettings());

        // Bottom row: music + settings horizontally
        HBox bottomRow = new HBox(10, musicBtn, settingsBtn);
        bottomRow.setPadding(new Insets(80));

        // Menu layout
        VBox menuLayout = new VBox(20, offlineBtn, onlineBtn, leaderboardBtn, bottomRow);
        menuLayout.setPadding(new Insets(40));
        menuLayout.setStyle("-fx-background-color: linear-gradient(lightblue, lightgreen);");
        menuScene = new Scene(menuLayout, 400, 400);

        // ==== GAME screen ====
        GridPane grid = new GridPane();
        grid.setHgap(5);  // horizontal gap between cells
        grid.setVgap(5);  // vertical gap between cells
        grid.setPadding(new Insets(10));
        Font cellFont = Font.font("Arial Black", 28);

        for (int i = 0; i < 9; i++) {
            Button b = new Button("-");
            b.setFont(cellFont);
            b.setTextFill(Color.DARKBLUE);
            b.setPrefSize(90, 90);
            b.setStyle("-fx-background-color: #e0f7fa; -fx-border-color: black; -fx-border-width: 2px;");

            final int pos = i;
            b.setOnAction(ev -> {
                if (onlineSaveMode && client != null) {
                    client.sendMove(pos); // send to server
                } else {
                    onCellClick(pos); // offline mode
                }
            });
            cells[i] = b;
            grid.add(b, i % 3, i / 3);
        }

        Button backBtn = new Button("⬅️ Back to Menu");
        backBtn.setOnAction(e -> primaryStage.setScene(menuScene));

        status.setFont(Font.font("Verdana", 16));
        status.setTextFill(Color.DARKRED);

        VBox gameRoot = new VBox(15, backBtn, grid, status);
        gameRoot.setPadding(new Insets(15));
        gameRoot.setStyle("-fx-background-color: linear-gradient(lightblue, lightgreen);");
        gameScene = new Scene(gameRoot, 350, 420);

        // ==== Start on Menu ====
        primaryStage.setScene(menuScene);
        primaryStage.setTitle("TicTacToe");
        primaryStage.show();
    }

    private void toggleMusic(Button musicBtn) {
        if (mediaPlayer == null) return;
        if (musicOn) {
            mediaPlayer.pause();
            musicBtn.setText("🔇 Music OFF");
        } else {
            mediaPlayer.play();
            musicBtn.setText("🔊 Music ON");
        }
        musicOn = !musicOn;
    }

    private void showSettings() {
        if (mediaPlayer == null) return;
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Settings");
        Slider slider = new Slider(0, 1, mediaPlayer.getVolume());
        slider.setShowTickMarks(true);
        slider.setShowTickLabels(true);
        slider.setBlockIncrement(0.1);
        slider.valueProperty().addListener((obs, oldVal, newVal) -> mediaPlayer.setVolume(newVal.doubleValue()));

        VBox box = new VBox(10, new Label("Music Volume:"), slider);
        box.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(box);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    // ==== Game logic methods remain unchanged ====
    private void onCellClick(int pos) {
        if (backend.isGameOver()) {
            status.setText("Game over: start a new game.");
            return;
        }
        boolean ok = backend.playerMove(pos);
        if (!ok) {
            status.setText("Invalid move");
            return;
        }
        updateUI();
        if (!backend.isGameOver()) {
            backend.computerMove();
            updateUI();
        }
        if (backend.isGameOver()) handleEnd();
    }

    private void handleEnd() {
        char w = backend.getWinner();
        if (w == 'X') status.setText("🎉 You win!");
        else if (w == 'O') status.setText("💻 CPU wins.");
        else status.setText("🤝 Draw.");

        if (onlineSaveMode && w == 'X' && db != null) {
            TextInputDialog d = new TextInputDialog("player");
            d.setHeaderText("You won! Enter name to save score:");
            d.setContentText("Name:");
            d.showAndWait().ifPresent(name -> {
                try {
                    db.addWin(name.trim());
                } catch (SQLException ex) {
                    ex.printStackTrace();
                    status.setText("DB save failed");
                }
            });
        }
    }

    private void updateUI() {
        char[] b = backend.getBoard();
        for (int i = 0; i < 9; i++) {
            cells[i].setText(String.valueOf(b[i]));
            if (b[i] == 'X')
                cells[i].setStyle("-fx-background-color: lightgreen; -fx-border-color: black; -fx-border-width: 2px;");
            else if (b[i] == 'O')
                cells[i].setStyle("-fx-background-color: lightcoral; -fx-border-color: black; -fx-border-width: 2px;");
            else cells[i].setStyle("-fx-background-color: #e0f7fa; -fx-border-color: black; -fx-border-width: 2px;");
        }
    }

    private void showLeaderboard() {
        if (db == null) {
            status.setText("DB unavailable");
            return;
        }
        try {
            List<String> board = db.getLeaderboard();
            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.setTitle("Leaderboard");
            a.setHeaderText("Top players");
            a.setContentText(String.join("\n", board.isEmpty() ? List.of("(no records)") : board));
            a.showAndWait();
        } catch (SQLException e) {
            e.printStackTrace();
            status.setText("DB read error");
        }
    }

    @Override
    public void stop() throws Exception {
        if (db != null) db.close();
        if (mediaPlayer != null) mediaPlayer.stop();
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

