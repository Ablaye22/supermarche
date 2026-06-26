package com.supermarche.util;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Centralise le formatage des montants et dates affiches dans l'IHM. */
public final class FormatUtil {

    private static final NumberFormat FORMAT_MONETAIRE = NumberFormat.getCurrencyInstance(Locale.FRANCE);
    public static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public static final DateTimeFormatter FORMAT_DATE_HEURE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private FormatUtil() {
    }

    public static String montant(BigDecimal valeur) {
        return FORMAT_MONETAIRE.format(valeur == null ? BigDecimal.ZERO : valeur);
    }
}
