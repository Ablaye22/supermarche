package com.supermarche.exception;

/**
 * Exception racine pour toutes les erreurs metier de l'application.
 * Permet aux controleurs JavaFX d'attraper une seule famille d'exceptions
 * et d'afficher un message comprehensible a l'utilisateur, sans exposer
 * les details techniques (SQL, stacktrace) qui n'ont pas de sens pour lui.
 */
public class SupermarcheException extends RuntimeException {

    public SupermarcheException(String message) {
        super(message);
    }

    public SupermarcheException(String message, Throwable cause) {
        super(message, cause);
    }
}
