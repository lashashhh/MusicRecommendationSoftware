package org.example;

import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.animation.*;
import javafx.scene.layout.*;
import javafx.util.Duration;
import javafx.scene.paint.*;

import java.sql.Time;

public class LoginGUI extends Application{

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Music Recommender");

        VBox root = new VBox();
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));
        root.setSpacing(15);
        root.getStyleClass().add("root");

        Label title = new Label("Welcome to Music Recommender \uD83C\uDFB6"); //notes emoji
        title.getStyleClass().add("title-label");

        DropShadow glow = new DropShadow();
        glow.setColor(Color.web("#1db954", 0.8));
        glow.setRadius(15);
        title.setEffect(glow);

        FadeTransition fadeIn = new FadeTransition(Duration.seconds(0.8), title);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        Timeline glowPulse = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(glow.radiusProperty(), 10)),
                new KeyFrame(Duration.seconds(1), new KeyValue(glow.radiusProperty(), 25)),
                new KeyFrame(Duration.seconds(2), new KeyValue(glow.radiusProperty(), 10))
        );
        glowPulse.setCycleCount(Timeline.INDEFINITE);
        glowPulse.setAutoReverse(true);

        fadeIn.setOnFinished(e -> glowPulse.play());
        fadeIn.play();

        GridPane grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setPadding(new Insets(20));
        grid.setVgap(10);
        grid.setHgap(10);
        grid.getStyleClass().add("login-card");

        Label userLabel = new Label("Username:");
        userLabel.getStyleClass().add("form-label");
        TextField usernameField = new TextField();
        usernameField.getStyleClass().add("text-field");

        Label passLabel = new Label("Password:");
        passLabel.getStyleClass().add("form-label");
        PasswordField passwordField = new PasswordField();
        passwordField.getStyleClass().add("password-field");

        Button loginButton = new Button("Sign In");
        loginButton.getStyleClass().add("button-primary");
        Button signupButton = new Button("Sign Up");
        signupButton.getStyleClass().add("button-secondary");

        grid.add(userLabel, 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(passLabel, 0, 1);
        grid.add(passwordField, 1, 1);
        grid.add(loginButton, 0, 2);
        grid.add(signupButton, 1, 2);

        root.getChildren().addAll(title, grid);

        TranslateTransition slideIn = new TranslateTransition(Duration.seconds(0.9), grid);
        slideIn.setFromY(80);
        slideIn.setToY(0);
        slideIn.setInterpolator(Interpolator.EASE_OUT);
        slideIn.play();

        loginButton.setOnAction(e -> {
            String username = usernameField.getText().trim();
            String password = passwordField.getText().trim();
            if (username.isEmpty() || password.isEmpty()){
                showAlert(Alert.AlertType.WARNING, "Please fill in all fields.");
                return;
            }
            try {
                loginSystem system = new loginSystem();
                boolean success = system.tryLogin(username, password);
                if (success) {
                 //   showAlert(Alert.AlertType.INFORMATION, "Login successful!");
                    new HomeScreen(username).start(primaryStage);
                } else {
                    showAlert(Alert.AlertType.ERROR, "Invalid credentials.");
                }
            } catch (Exception ex) {
                System.err.println("[LOGIN_ERROR]" + ex.getMessage());
                ex.printStackTrace();
            }
        });
        signupButton.setOnAction(e -> {
            String username = usernameField.getText().trim();
            String password = passwordField.getText().trim();
            if (username.isEmpty() || password.isEmpty()){
                showAlert(Alert.AlertType.WARNING, "Please fill in all fields.");
                return;
            }
            try {
                loginSystem sys = new loginSystem();
                boolean registered = sys.trySignup(username, password);
                if (registered) {
                    showAlert(Alert.AlertType.INFORMATION, "Registration successful!");
                } else {
                    showAlert(Alert.AlertType.WARNING, "Username already exists!");
                }
            } catch (Exception ex) {
                System.err.println("[SIGNUP_ERROR]" + ex.getMessage());
                ex.printStackTrace();
            }
        });

     try {
         Scene scene = new Scene(root, 420, 300);
         var css = getClass().getResource("/org/example/style.css");
         if (css != null) {
             scene.getStylesheets().add(css.toExternalForm());
             System.out.println("[GUI_STYLE] style.css loaded successfully.");
         } else {
             System.err.println("[GUI_STYLE] style.css could not be loaded.");
         }

         Stop[] stops1 = new Stop[] {new Stop(0, Color.web("#0f0f0f")), new Stop(1, Color.web("#1e1e1e")) };
         Stop[] stops2 = new Stop[] {new Stop(0, Color.web("#1e1e1e")), new Stop(1, Color.web("#121212")) };
         Stop[] stops3 = new Stop[] {new Stop(0, Color.web("#121212")), new Stop(1, Color.web("#1a2b1d")) };
         Stop[] stops4 = new Stop[] {new Stop(0, Color.web("#1a2b1d")), new Stop(1, Color.web("#0f0f0f")) };

         LinearGradient gradient1 = new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE, stops1);
         LinearGradient gradient2 = new LinearGradient(1, 0, 0, 1, true, CycleMethod.NO_CYCLE, stops2);
         LinearGradient gradient3 = new LinearGradient(0, 1, 1, 0, true, CycleMethod.NO_CYCLE, stops3);
         LinearGradient gradient4 = new LinearGradient(1, 1, 0, 0, true, CycleMethod.NO_CYCLE, stops4);

         BackgroundFill fill1 = new BackgroundFill(gradient1, CornerRadii.EMPTY, Insets.EMPTY);
         BackgroundFill fill2 = new BackgroundFill(gradient2, CornerRadii.EMPTY, Insets.EMPTY);
         BackgroundFill fill3 = new BackgroundFill(gradient3, CornerRadii.EMPTY, Insets.EMPTY);
         BackgroundFill fill4 = new BackgroundFill(gradient4, CornerRadii.EMPTY, Insets.EMPTY);

         Background background = new Background(fill1);
         root.setBackground(background);

         Timeline timeLine = new Timeline(
             new KeyFrame(Duration.ZERO, new KeyValue(root.backgroundProperty(), new Background(fill1))),
             new KeyFrame(Duration.seconds(2), new KeyValue(root.backgroundProperty(), new Background(fill2))),
             new KeyFrame(Duration.seconds(4), new KeyValue(root.backgroundProperty(), new Background(fill3))),
             new KeyFrame(Duration.seconds(6), new KeyValue(root.backgroundProperty(), new Background(fill4))),
             new KeyFrame(Duration.seconds(8), new KeyValue(root.backgroundProperty(), new Background(fill1)))
         );
         timeLine.setCycleCount(Animation.INDEFINITE);
         timeLine.setAutoReverse(true);
         timeLine.play();

         primaryStage.setScene(scene);
         primaryStage.show();
     } catch (Exception ex) {
         System.err.println("[GUI_INIT_ERROR] Failed to load GUI scene" + ex.getMessage());
         ex.printStackTrace();
     }
    }

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String [] args){
        launch(args);
    }
}
