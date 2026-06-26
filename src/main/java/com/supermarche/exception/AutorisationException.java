package com.supermarche.exception;

/** Levee lorsqu'un utilisateur authentifie tente une action pour laquelle il n'a pas la permission. */
public class AutorisationException extends SupermarcheException {

    public AutorisationException(String message) {
        super(message);
    }
}
