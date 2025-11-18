package org.example;

import javafx.animation.FadeTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.control.ScrollPane;

import javax.xml.crypto.Data;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class HomeScreen {

    private final String username;

    public HomeScreen (String username) {
        this.username = username;
    }

    public void start(Stage stage){
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root");

        VBox sidebar = new VBox(20);
        sidebar.setAlignment(Pos.TOP_CENTER);
        sidebar.setPadding(new Insets(30, 15, 30, 15));
        sidebar.setPrefWidth(180);
        sidebar.setStyle("-fx-background-color: #181818");

        Label title = new Label("Music Recommender");
        title.setStyle("-fx-font-size: 16px; -fx-font-fill: #1db954");

        Button libraryBtn = new Button("Your Library");
        Button discoverBtn = new Button("Discover New Music");
        Button statsBtn = new Button("Your Statistics");
        Button settingsBtn = new Button("Settings / Logout");

        libraryBtn.getStyleClass().add("button-secondary");
        discoverBtn.getStyleClass().add("button-secondary");
        statsBtn.getStyleClass().add("button-secondary");
        settingsBtn.getStyleClass().add("button-secondary");

        sidebar.getChildren().addAll(title, libraryBtn, discoverBtn, statsBtn, settingsBtn);

        VBox contentArea = new VBox(15);
        contentArea.setAlignment(Pos.CENTER);
        contentArea.setPadding(new Insets(40));

        ScrollPane scrollPane = new ScrollPane(contentArea);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setPannable(true);
        scrollPane.setStyle("-fx-background-color: transparent;");


        Label welcomeLabel = new Label("Welcome " + username + "!");
        welcomeLabel.getStyleClass().add("title-label");
        contentArea.getChildren().add(welcomeLabel);

        root.setLeft(sidebar);
        root.setCenter(scrollPane);

        Scene scene = new Scene(root, 900, 550);

        var css = getClass().getResource("/org/example/style.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
            System.out.println("[GUI_STYLE] Dashboard CSS loaded.");
        } else {
            System.err.println("[GUI_STYLE] Dashboard CSS not found.");
        }

        stage.setScene(scene);
        stage.show();

        FadeTransition fadeIn = new FadeTransition(Duration.seconds(2.2), welcomeLabel);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.setOnFinished(e -> showDiscover(contentArea));
        fadeIn.play();

        discoverBtn.setOnAction(e -> {
            highlightBtn(discoverBtn, libraryBtn, statsBtn, settingsBtn);
            showDiscover(contentArea);
        });
        libraryBtn.setOnAction(e -> {
            highlightBtn(libraryBtn, discoverBtn, statsBtn, settingsBtn);
            showLibrary(contentArea);
        });
        statsBtn.setOnAction(e -> {
            highlightBtn(statsBtn, libraryBtn, discoverBtn, settingsBtn);
            showStats(contentArea);
        });
        settingsBtn.setOnAction(e -> {
            highlightBtn(settingsBtn, libraryBtn, statsBtn, discoverBtn);
            showSettings(contentArea, stage);
        });
    }

    private void showDiscover(VBox content) {
        content.getChildren().clear();
        Label title = new Label("Discover New Music");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: bold");
        Label info = new Label("Fetch Top 20 of the Week");
        info.setStyle("-fx-text-fill: #b3b3b3;");
        Button fetchTopBtn = new Button("Fetch Top 20");
        fetchTopBtn.getStyleClass().add("button-primary");

        VBox songsBox = new VBox(12);
        songsBox.setAlignment(Pos.CENTER_LEFT);

        fetchTopBtn.setOnAction(e ->{
            songsBox.getChildren().clear();
            Label loading = new Label("Fetching...");
            loading.setStyle("-fx-text-fill: #b3b3b3;");
            songsBox.getChildren().add(loading);

            new Thread(() -> {
                try{
                    metadataScraper.fetchWeeklyTopTracks(20);

                    javafx.application.Platform.runLater(() -> {
                        songsBox.getChildren().clear();
                        Label done = new Label("Top 20 Songs of the Week:");
                        done.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold");
                        songsBox.getChildren().add(done);

                        try(Connection conn = Database.connect()){
                            var stmt = conn.prepareStatement(
                                    "SELECT id, track_name, artist_name, album_name, playcount, tags, album_image, cached_at " +
                                        "FROM weekly_top_cache ORDER BY playcount DESC LIMIT 20");
                            var rs = stmt.executeQuery();

                            while (rs.next()){
                                int songId = rs.getInt("id");
                                String track = rs.getString("track_name");
                                String artist = rs.getString("artist_name");
                                String album = rs.getString("album_name");
                                long plays = rs.getLong("playcount");
                                String tags = rs.getString("tags");
                                String albumImg = rs.getString("album_image");

                                HBox songRow = new HBox(10);
                                songRow.setAlignment(Pos.CENTER_LEFT);
                                songRow.setPadding(new Insets(5, 0, 5, 0));
                                songRow.setStyle("-fx-cursor: hand;");

                                javafx.scene.image.ImageView albumView = new javafx.scene.image.ImageView();
                                if (albumImg != null && !albumImg.isBlank() && !albumImg.equals("(N/A)") && albumImg.startsWith("http")){
                                    try{
                                        String safeUrl = albumImg.replace("http://", "https://");
                                        javafx.scene.image.Image image = new javafx.scene.image.Image(safeUrl, 50, 50, true, true);
                                        image.errorProperty().addListener((observable, oldValue, newValue) -> {
                                            if (newValue) System.err.println("[IMG_LOAD_ERROR]" + safeUrl);
                                        });
                                        albumView.setImage(image);
                                    } catch (Exception ex) {
                                        ex.printStackTrace();
                                    }
                                }
                                albumView.setFitWidth(50);
                                albumView.setFitHeight(50);
                                albumView.setStyle("-fx-background-color: #333; -fx-background-radius: 8;");

                                VBox songText = new VBox(2);
                                Label songLabel = new Label(artist + " _ " + track);
                                songLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px");
                                Label playLabel = new Label(plays + " plays");
                                playLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 12px");
                                songText.getChildren().addAll(songLabel, playLabel);

                                songRow.getChildren().addAll(albumView, songText);
                                songRow.setOnMouseClicked(ev -> showSongPopup(artist, track, album, tags, plays, albumImg));

                                songsBox.getChildren().add(songRow);
                            }

                            var tsStmt = conn.createStatement();
                            var tsRs = tsStmt.executeQuery("SELECT MAX(cached_at) AS last_cached FROM weekly_top_cache");
                            if (tsRs.next() && tsRs.getLong("last_cached") != 0){
                                Label updated = new Label("Last Updated " + tsRs.getTimestamp("last_cached)").toLocalDateTime());
                                updated.setStyle("-fx-text-fill: #666; -fx-font-size: 11px");
                                songsBox.getChildren().add(updated);
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            Label error = new Label("Error loading songs.");
                            error.setStyle("-fx-text-fill: red;");
                            songsBox.getChildren().add(error);
                        }
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                    javafx.application.Platform.runLater(() -> {
                        songsBox.getChildren().clear();
                        Label error = new Label("Failed to fetch songs: " + ex.getMessage());
                        error.setStyle("-fx-text-fill: red");
                        songsBox.getChildren().add(error);
                    });
                }
            }).start();
        });

        content.getChildren().addAll(title, info, fetchTopBtn, songsBox);
        animateContent(content);
/*        Label placeholder = new Label("Discover songs");
        placeholder.setStyle("-fx-text-fill: #b3b3b3");
        content.getChildren().addAll(title, placeholder);*/
    }

    private void showSongPopup(String artist, String track, String album, String tags, long plays, String albumImg){
        Stage popup = new Stage();
        popup.setTitle("Track Details");

        VBox box = new VBox(15);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(20));
        box.setStyle("-fx-background-color: #181818; -fx-background-radius: 15;");

        javafx.scene.image.ImageView imgView= new javafx.scene.image.ImageView();
        if (albumImg != null && !albumImg.isBlank() && !albumImg.equals("(N/A)") && albumImg.startsWith("http")){
            try{
                String safeUrl = albumImg.replace("http://", "https://");
                javafx.scene.image.Image img = new javafx.scene.image.Image(safeUrl, 200, 200, true, true);
                img.errorProperty().addListener((observable, oldValue, newValue) -> {
                    if (newValue) System.err.println("[IMG_LOAD_ERROR]" + safeUrl);
                });
                imgView.setImage(img);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        imgView.setFitWidth(200);
        imgView.setFitHeight(200);
        imgView.setStyle("-fx-background-color: #333; -fx-background-radius: 15;");

        javafx.scene.control.Tooltip genreTooltip = new javafx.scene.control.Tooltip(
                (tags != null && !tags.isBlank()) ? "Genres: " + tags : "No tags available"
        );
        genreTooltip.setStyle("-fx-background-color: #1e1e1e; -fx-text-fill: #1db954; -fx-font-size: 12px;");
        javafx.scene.control.Tooltip.install(imgView, genreTooltip);

        Label trackLabel = new Label(track);
        trackLabel.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: bold");
        Label artistLabel = new Label("Artist: " + artist);
        artistLabel.setStyle("-fx-text-fill: #b3b3b3; -fx-font-size: 14px");
        Label albumLabel = new Label("Album: " + album);
        albumLabel.setStyle("-fx-text-fill: #b3b3b3; -fx-font-size: 14px");
        Label playLabel = new Label("Playcount: " + plays);
        playLabel.setStyle("-fx-text-fill: #b3b3b3; -fx-font-size: 14px");
        Label tagLabel = new Label("Tags: " + (tags == null || tags.isBlank() ? "N/A" : tags));
        tagLabel.setStyle("-fx-text-fill: #1db954; -fx-font-size: 13px");

        Button likeBtn = new Button("♥ Like");
        likeBtn.getStyleClass().add("button-primary");
        Button excludeBtn = new Button("✖ Exclude");
        excludeBtn.getStyleClass().add("button-secondary");
        likeBtn.setOnAction(e -> {
            try (Connection conn = Database.connect()) {
                PreparedStatement getSong = conn.prepareStatement(
                        "SELECT id FROM songs WHERE track_name = ? AND artist name = ? LIMIT 1"
                );
                getSong.setString(1, track);
                getSong.setString(2, artist);
                ResultSet rs = getSong.executeQuery();

                if (rs.next()) {
                    int songId = rs.getInt("id");
                    PreparedStatement stmt = conn.prepareStatement(
                            "INSERT IGNORE INTO user_likes (user_id, song_id) VALUES (?, ?)"
                    );
                    stmt.setInt(1, loginSystem.getCurrentUserId());
                    stmt.setInt(2, songId);
                    stmt.executeUpdate();
                    likeBtn.setText("✔ Liked");
                    likeBtn.setDisable(true);
                    excludeBtn.setDisable(true);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                likeBtn.setText("Error");
            }
        });

        excludeBtn.setOnAction(e -> {
            try (Connection conn = Database.connect()) {
                PreparedStatement getSong = conn.prepareStatement(
                        "SELECT id FROM songs WHERE track_name = ? AND artist_name = ? LIMIT 1"
                );
                getSong.setString(1, track);
                getSong.setString(2, artist);
                ResultSet rs = getSong.executeQuery();
                if (rs.next()) {
                    int songId = rs.getInt("id");
                    PreparedStatement stmt = conn.prepareStatement(
                            "INSERT IGNORE INTO user_exclusions (user_id, song_id) VALUES (?, ?)"
                    );
                    stmt.setInt(1, loginSystem.getCurrentUserId());
                    stmt.setInt(2, songId);
                    stmt.executeUpdate();
                    excludeBtn.setText("✔ Excluded");
                    likeBtn.setDisable(true);
                    excludeBtn.setDisable(true);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                excludeBtn.setText("Error");
            }
        });

        Button closeBtn = new Button("Close");
        closeBtn.getStyleClass().add("button-secondary");
        closeBtn.setOnAction(e -> popup.close());

        box.getChildren().addAll(imgView, trackLabel, artistLabel, albumLabel, playLabel, tagLabel, closeBtn);

        Scene scene = new Scene(box, 340, 500);
        var css = getClass().getResource("/org/example/style.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());

        popup.setScene(scene);
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.show();

        FadeTransition fade = new FadeTransition(Duration.seconds(0.5), box);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }

    private void showLibrary(VBox content) {
        content.getChildren().clear();
        Label title = new Label("Your Library");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: bold");
        Label placeholder = new Label("Your saved songs");
        placeholder.setStyle("-fx-text-fill: #b3b3b3");
        content.getChildren().addAll(title, placeholder);
        animateContent(content);
    }

    private void showStats(VBox content) {
        content.getChildren().clear();
        Label title = new Label("Your Statistics");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: bold");
        Label placeholder = new Label("Your statistics and recommendations");
        placeholder.setStyle("-fx-text-fill: #b3b3b3");
        content.getChildren().addAll(title, placeholder);
        animateContent(content);
    }

    private void showSettings(VBox content, Stage stage) {
        content.getChildren().clear();
        Label title = new Label("Settings");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 22px; -fx-font-weight: bold");
        Label placeholder = new Label("Log out or adjust preferences");
        placeholder.setStyle("-fx-text-fill: #b3b3b3");

        Button logout = new Button("Log Out");
        logout.getStyleClass().add("button-primary");
        logout.setOnAction(e -> {
            try {
                new LoginGUI().start(stage);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        content.getChildren().addAll(title, placeholder, logout);
        animateContent (content);
    }

    private void animateContent(VBox content) {
        FadeTransition fade = new FadeTransition(Duration.seconds(0.5), content);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }

    private void highlightBtn(Button active, Button... others) {
        active.setStyle("-fx-background-color: #1db954; -fx-text-fill: black; -fx-font-weight: bold");
        for (Button b : others) {
            b.setStyle("");
            b.getStyleClass().add("button-secondary");
        }
    }
}
