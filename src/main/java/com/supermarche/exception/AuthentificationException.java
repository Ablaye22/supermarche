package com.supermarche.exception;

/** Levee lors d'un echec d'authentification (identifiants invalides, compte verrouille/inactif). */
public class AuthentificationException extends SupermarcheException {

    public AuthentificationException(String message) {
        super(message);
    }
}
