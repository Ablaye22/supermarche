package com.supermarche;

import com.supermarche.config.DatabaseConfig;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class MainApp extends Application {

    private static final Logger log = LoggerFactory.getLogger(MainApp.class);

    private static Stage stagePrincipal;

    @Override
    public void start(Stage stage) throws IOException {
        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        stage.setWidth(screen.getWidth());
        stage.setHeight(screen.getHeight());
        stagePrincipal = stage;
        stage.setTitle("Gestion Supermarche");
        stage.setFullScreen(true);
        stage.initStyle(StageStyle.UNDECORATED);

        afficherEcranConnexion();

        stage.setOnCloseRequest(evt -> {
            log.info("Fermeture de l'application demandee par l'utilisateur.");
            DatabaseConfig.fermer();
        });

        stage.show();
    }

    /** Change la racine de la scene tout en gardant la meme fenetre (evite le clignotement d'un nouveau Stage). */
    public static void changerVue(String cheminFxml) throws IOException {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource(cheminFxml));
        Parent racine = loader.load();
        Scene scene = stagePrincipal.getScene();
        if (scene == null) {
            scene = new Scene(racine, 1280, 800);
            scene.getStylesheets().add(MainApp.class.getResource("/css/theme.css").toExternalForm());
            stagePrincipal.setScene(scene);
        } else {
            scene.setRoot(racine);
        }
    }

    public static void afficherEcranConnexion() throws IOException {
        changerVue("/fxml/connexion.fxml");
        stagePrincipal.setMaximized(false);
        stagePrincipal.centerOnScreen();
    }

    public static void afficherApplicationPrincipale() throws IOException {
        changerVue("/fxml/principal.fxml");
    }

    public static Stage getStagePrincipal() {
        return stagePrincipal;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
