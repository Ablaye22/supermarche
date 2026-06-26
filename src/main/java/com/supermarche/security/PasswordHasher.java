package com.supermarche.security;

import com.supermarche.config.AppConfig;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Encapsule le hachage et la verification des mots de passe avec BCrypt.
 * BCrypt integre automatiquement un sel aleatoire par mot de passe et un
 * facteur de cout configurable, ce qui le rend resistant aux attaques
 * par force brute meme si la base de donnees est compromise.
 *
 * IMPORTANT : ne jamais stocker, logger, ou afficher un mot de passe en clair.
 */
public final class PasswordHasher {

    private static final BCryptPasswordEncoder ENCODER =
            new BCryptPasswordEncoder(AppConfig.getInstance().getBcryptStrength());

    private PasswordHasher() {
    }

    /** Calcule le hash BCrypt d'un mot de passe en clair, a stocker en base. */
    public static String hacher(String motDePasseClair) {
        if (motDePasseClair == null || motDePasseClair.isBlank()) {
            throw new IllegalArgumentException("Le mot de passe ne peut pas etre vide");
        }
        return ENCODER.encode(motDePasseClair);
    }

    /** Verifie qu'un mot de passe en clair correspond au hash stocke en base. */
    public static boolean verifier(String motDePasseClair, String hashStocke) {
        if (motDePasseClair == null || hashStocke == null) {
            return false;
        }
        return ENCODER.matches(motDePasseClair, hashStocke);
    }

    /**
     * Petit utilitaire en ligne de commande pour generer le hash d'un mot
     * de passe administrateur initial, a inserer manuellement dans le
     * script SQL de donnees initiales (02_donnees_initiales.sql).
     *
     * Usage : mvn exec:java -Dexec.mainClass="com.supermarche.security.PasswordHasher" -Dexec.args="MonMotDePasse"
     */
    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage : fournir le mot de passe en clair en argument unique.");
            return;
        }
        System.out.println(hacher(args[0]));
    }
}
