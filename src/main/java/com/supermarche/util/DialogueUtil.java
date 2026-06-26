package com.supermarche.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;

import java.util.Optional;

/**
 * Centralise la creation des boites de dialogue pour garder un style et
 * un comportement homogenes (titres, boutons) partout dans l'application.
 */
public final class DialogueUtil {

    private DialogueUtil() {
    }

    public static void afficherErreur(String titre, String message) {
        Alert alerte = new Alert(Alert.AlertType.ERROR);
        alerte.setTitle(titre);
        alerte.setHeaderText(null);
        alerte.setContentText(message);
        alerte.showAndWait();
    }

    public static void afficherInfo(String titre, String message) {
        Alert alerte = new Alert(Alert.AlertType.INFORMATION);
        alerte.setTitle(titre);
        alerte.setHeaderText(null);
        alerte.setContentText(message);
        alerte.showAndWait();
    }

    public static void afficherAvertissement(String titre, String message) {
        Alert alerte = new Alert(Alert.AlertType.WARNING);
        alerte.setTitle(titre);
        alerte.setHeaderText(null);
        alerte.setContentText(message);
        alerte.showAndWait();
    }

    public static boolean confirmer(String titre, String message) {
        Alert alerte = new Alert(Alert.AlertType.CONFIRMATION);
        alerte.setTitle(titre);
        alerte.setHeaderText(null);
        alerte.setContentText(message);
        Optional<ButtonType> resultat = alerte.showAndWait();
        return resultat.isPresent() && resultat.get() == ButtonType.OK;
    }

    public static Optional<String> demanderSaisie(String titre, String message, String valeurParDefaut) {
        TextInputDialog dialogue = new TextInputDialog(valeurParDefaut == null ? "" : valeurParDefaut);
        dialogue.setTitle(titre);
        dialogue.setHeaderText(null);
        dialogue.setContentText(message);
        return dialogue.showAndWait();
    }
}
