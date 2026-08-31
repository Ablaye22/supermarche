package com.supermarche.exception;

/** Erreur survenue lors d'un acces a la base de donnees (SQL, connexion...). */
public class AccesDonneesException extends SupermarcheException {

    public AccesDonneesException(String message, Throwable cause) {
        super(message, cause);
    }
}
