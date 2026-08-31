package com.supermarche.service;

import com.supermarche.model.LigneVente;
import com.supermarche.model.Vente;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Logique de calcul des totaux d'une vente, extraite de VenteService pour
 * etre testable independamment de toute base de donnees : ce sont des
 * calculs purs (memes entrees -> memes sorties), sans effet de bord.
 */
public final class CalculateurVente {

    private CalculateurVente() {
    }

    /**
     * Recalcule HT/TVA/TTC d'une vente a partir de ses lignes. Chaque
     * ligne porte son propre taux de TVA (utile si le panier melange des
     * produits a taux reduit et a taux normal). Le TTC est la somme des
     * lignes (deja arrondies), et le HT/TVA en sont deduits par ligne
     * pour eviter les ecarts d'arrondi entre des taux differents.
     */
    public static void calculerTotaux(Vente vente) {
        BigDecimal totalTtc = BigDecimal.ZERO;
        BigDecimal totalHt = BigDecimal.ZERO;

        for (LigneVente ligne : vente.getLignes()) {
            ligne.recalculerTotal();
            totalTtc = totalTtc.add(ligne.getTotalLigneTtc());

            BigDecimal facteurTva = BigDecimal.ONE.add(
                    ligne.getTauxTva().divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP));
            BigDecimal htLigne = ligne.getTotalLigneTtc().divide(facteurTva, 2, RoundingMode.HALF_UP);
            totalHt = totalHt.add(htLigne);
        }

        vente.setTotalTtc(totalTtc.setScale(2, RoundingMode.HALF_UP));
        vente.setTotalHt(totalHt.setScale(2, RoundingMode.HALF_UP));
        vente.setTotalTva(vente.getTotalTtc().subtract(vente.getTotalHt()).setScale(2, RoundingMode.HALF_UP));
    }
}
