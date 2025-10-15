package org.example;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.Duration;

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
        sidebar.getStyleClass().add("-fx-background-color: #181818");

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

        Label welcomeLabel = new Label("Welcome " + username + "!");
        welcomeLabel.getStyleClass().add("title-label");
        contentArea.getChildren().add(welcomeLabel);

        root.setLeft(sidebar);
        root.setCenter(contentArea);

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
        Label placeholder = new Label("Discover songs");
        placeholder.setStyle("-fx-text-fill: #b3b3b3");
        content.getChildren().addAll(title, placeholder);
        animateContent(content);
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
