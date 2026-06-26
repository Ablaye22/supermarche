package com.supermarche.service;

import com.supermarche.model.LigneVente;
import com.supermarche.model.Vente;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests de la logique de calcul des totaux de vente. Aucune dependance
 * externe : ce sont des fonctions pures, donc des tests rapides et
 * deterministes qui couvrent les cas d'arrondi les plus sensibles
 * (taux de TVA multiples, remises, quantites au poids).
 */
class CalculateurVenteTest {

    @Test
    void calculTotauxAvecUneSeuleLigneTauxNormal() {
        Vente vente = new Vente();
        vente.ajouterLigne(new LigneVente(1, "Produit A", BigDecimal.valueOf(2),
                new BigDecimal("10.00"), new BigDecimal("20.00")));

        CalculateurVente.calculerTotaux(vente);

        assertEquals(new BigDecimal("20.00"), vente.getTotalTtc());
        // HT = 20 / 1.20 = 16.67 (arrondi au centime)
        assertEquals(new BigDecimal("16.67"), vente.getTotalHt());
        assertEquals(new BigDecimal("3.33"), vente.getTotalTva());
    }

    @Test
    void calculTotauxAvecPlusieursTauxDeTvaDifferents() {
        Vente vente = new Vente();
        // Produit alimentaire a taux reduit (5.5%)
        vente.ajouterLigne(new LigneVente(1, "Pain", BigDecimal.valueOf(1),
                new BigDecimal("2.00"), new BigDecimal("5.50")));
        // Produit a taux normal (20%)
        vente.ajouterLigne(new LigneVente(2, "Gadget", BigDecimal.valueOf(1),
                new BigDecimal("12.00"), new BigDecimal("20.00")));

        CalculateurVente.calculerTotaux(vente);

        assertEquals(new BigDecimal("14.00"), vente.getTotalTtc());
        // Chaque ligne est convertie en HT separement puis sommee, ce qui
        // evite les ecarts d'arrondi qu'un taux moyen global produirait.
        BigDecimal htAttendu = new BigDecimal("2.00").divide(new BigDecimal("1.055"), 2, java.math.RoundingMode.HALF_UP)
                .add(new BigDecimal("12.00").divide(new BigDecimal("1.20"), 2, java.math.RoundingMode.HALF_UP));
        assertEquals(htAttendu.setScale(2, java.math.RoundingMode.HALF_UP), vente.getTotalHt());
    }

    @Test
    void calculTotauxAvecRemiseSurUneLigne() {
        Vente vente = new Vente();
        LigneVente ligne = new LigneVente(1, "Produit", BigDecimal.valueOf(1),
                new BigDecimal("100.00"), new BigDecimal("20.00"));
        ligne.setRemisePourcentage(new BigDecimal("10"));
        vente.ajouterLigne(ligne);

        CalculateurVente.calculerTotaux(vente);

        // 100 avec 10% de remise = 90
        assertEquals(new BigDecimal("90.00"), vente.getTotalTtc());
    }

    @Test
    void calculTotauxAvecQuantiteAuPoidsDecimale() {
        Vente vente = new Vente();
        // 0.750 kg de fromage a 20€/kg
        vente.ajouterLigne(new LigneVente(1, "Fromage", new BigDecimal("0.750"),
                new BigDecimal("20.00"), new BigDecimal("5.50")));

        CalculateurVente.calculerTotaux(vente);

        assertEquals(new BigDecimal("15.00"), vente.getTotalTtc());
    }

    @Test
    void calculTotauxAvecPanierVideDonneZero() {
        Vente vente = new Vente();

        CalculateurVente.calculerTotaux(vente);

        assertEquals(BigDecimal.ZERO.setScale(2), vente.getTotalTtc());
        assertEquals(BigDecimal.ZERO.setScale(2), vente.getTotalHt());
        assertEquals(BigDecimal.ZERO.setScale(2), vente.getTotalTva());
    }

    @Test
    void totalTvaEstToujoursLaDifferenceExacteEntreTtcEtHt() {
        // Invariant comptable : TTC - HT doit toujours egaler exactement la TVA affichee,
        // sans quoi un controle fiscal reverait un ecart de centimes.
        Vente vente = new Vente();
        vente.ajouterLigne(new LigneVente(1, "A", BigDecimal.valueOf(3),
                new BigDecimal("7.33"), new BigDecimal("20.00")));
        vente.ajouterLigne(new LigneVente(2, "B", BigDecimal.valueOf(2),
                new BigDecimal("4.99"), new BigDecimal("5.50")));

        CalculateurVente.calculerTotaux(vente);

        assertEquals(vente.getTotalTtc().subtract(vente.getTotalHt()), vente.getTotalTva());
    }
}
