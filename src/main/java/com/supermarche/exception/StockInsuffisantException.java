package com.supermarche.exception;

/** Levee lorsqu'une operation (vente, sortie de stock) demande plus de quantite que disponible. */
public class StockInsuffisantException extends SupermarcheException {

    public StockInsuffisantException(String message) {
        super(message);
    }
}
