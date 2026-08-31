package com.supermarche.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Genere un ticket de caisse au format "reçu" (80mm, police monospace)
 * plutot qu'un document A4 : hauteur de page calculee dynamiquement selon
 * le nombre de lignes du panier, pas de pagination (un ticket = une page).
 */
public final class TicketPdfExporter {

    /** Une ligne du panier a imprimer sur le ticket. */
    public record LigneTicket(String designation, BigDecimal quantite, BigDecimal prixUnitaire, BigDecimal total) {
    }

    private static final DateTimeFormatter FORMAT_DATE_HEURE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final float LARGEUR_PAGE = 227f;   // ~80mm
    private static final float MARGE = 12f;
    private static final float LARGEUR_UTILE = LARGEUR_PAGE - 2 * MARGE;

    private static final float H_TITRE = 16f;
    private static final float H_SOUS_TITRE = 13f;
    private static final float H_NORMALE = 11f;
    private static final float H_TOTAL = 15f;
    private static final float H_SEPARATEUR = 9f;
    private static final float H_BLANC = 6f;

    private static final PDFont POLICE_TITRE = new PDType1Font(Standard14Fonts.FontName.COURIER_BOLD);
    private static final PDFont POLICE_NORMALE = new PDType1Font(Standard14Fonts.FontName.COURIER);
    private static final PDFont POLICE_TOTAL = new PDType1Font(Standard14Fonts.FontName.COURIER_BOLD);

    private TicketPdfExporter() {
    }

    public static void exporter(
            File fichier,
            String nomCaisse,
            String nomCaissier,
            String numeroTicket,
            LocalDateTime dateVente,
            List<LigneTicket> lignes,
            BigDecimal totalTtc,
            String modePaiement,
            BigDecimal montantPaye,
            BigDecimal monnaieRendue
    ) throws IOException {

        boolean afficherMonnaie = monnaieRendue != null && monnaieRendue.compareTo(BigDecimal.ZERO) > 0;

        float hauteurPage = MARGE * 2
                + H_TITRE + H_SOUS_TITRE + H_SEPARATEUR
                + H_NORMALE * 3 + H_SEPARATEUR
                + (H_NORMALE * 2) * lignes.size()
                + H_SEPARATEUR
                + H_TOTAL
                + H_NORMALE * 2
                + (afficherMonnaie ? H_NORMALE : 0)
                + H_SEPARATEUR + H_BLANC + H_SOUS_TITRE;

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(new PDRectangle(LARGEUR_PAGE, hauteurPage));
            document.addPage(page);

            try (PDPageContentStream flux = new PDPageContentStream(document, page)) {
                float y = hauteurPage - MARGE;

                y = ecrireCentre(flux, "SUPERMARCHE", y, POLICE_TITRE, 12f, H_TITRE);
                y = ecrireCentre(flux, "Ticket de caisse", y, POLICE_NORMALE, 9f, H_SOUS_TITRE);
                y = separateur(flux, y);

                y = ecrireGauche(flux, "Caisse : " + nomCaisse, y, H_NORMALE);
                y = ecrireGauche(flux, "Date : " + FORMAT_DATE_HEURE.format(dateVente), y, H_NORMALE);
                y = ecrireGauche(flux, "Caissier : " + nomCaissier, y, H_NORMALE);
                y = ecrireGauche(flux, "Ticket n. " + numeroTicket, y, H_NORMALE);
                y = separateur(flux, y);

                for (LigneTicket ligne : lignes) {
                    String designation = tronquer(ligne.designation(), POLICE_NORMALE, 8f, LARGEUR_UTILE);
                    y = ecrireGauche(flux, designation, y, H_NORMALE);

                    String qteEtPrix = ligne.quantite().stripTrailingZeros().toPlainString()
                            + " x " + formatMontant(ligne.prixUnitaire());
                    String total = formatMontant(ligne.total());
                    y = ecrireGaucheDroite(flux, qteEtPrix, total, y, H_NORMALE);
                }
                y = separateur(flux, y);

                y = ecrireGaucheDroite(flux, "TOTAL TTC", formatMontant(totalTtc) + " EUR", y, H_TOTAL, POLICE_TOTAL, 10f);
                y = ecrireGauche(flux, "Paiement : " + modePaiement, y, H_NORMALE);
                y = ecrireGauche(flux, "Recu : " + formatMontant(montantPaye) + " EUR", y, H_NORMALE);
                if (afficherMonnaie) {
                    y = ecrireGauche(flux, "Rendu : " + formatMontant(monnaieRendue) + " EUR", y, H_NORMALE);
                }
                y = separateur(flux, y);
                y -= H_BLANC;
                ecrireCentre(flux, "Merci de votre visite !", y, POLICE_NORMALE, 9f, H_SOUS_TITRE);
            }

            document.save(fichier);
        }
    }

    private static String formatMontant(BigDecimal montant) {
        return montant.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }

    private static float ecrireCentre(PDPageContentStream flux, String texte, float y, PDFont police, float taille, float hauteurLigne) throws IOException {
        float largeurTexte = police.getStringWidth(texte) / 1000f * taille;
        float x = MARGE + (LARGEUR_UTILE - largeurTexte) / 2f;
        flux.beginText();
        flux.setFont(police, taille);
        flux.newLineAtOffset(x, y);
        flux.showText(texte);
        flux.endText();
        return y - hauteurLigne;
    }

    private static float ecrireGauche(PDPageContentStream flux, String texte, float y, float hauteurLigne) throws IOException {
        flux.beginText();
        flux.setFont(POLICE_NORMALE, 8f);
        flux.newLineAtOffset(MARGE, y);
        flux.showText(texte);
        flux.endText();
        return y - hauteurLigne;
    }

    private static float ecrireGaucheDroite(PDPageContentStream flux, String gauche, String droite, float y, float hauteurLigne) throws IOException {
        return ecrireGaucheDroite(flux, gauche, droite, y, hauteurLigne, POLICE_NORMALE, 8f);
    }

    private static float ecrireGaucheDroite(PDPageContentStream flux, String gauche, String droite, float y, float hauteurLigne, PDFont police, float taille) throws IOException {
        flux.beginText();
        flux.setFont(police, taille);
        flux.newLineAtOffset(MARGE, y);
        flux.showText(gauche);
        flux.endText();

        float largeurDroite = police.getStringWidth(droite) / 1000f * taille;
        flux.beginText();
        flux.setFont(police, taille);
        flux.newLineAtOffset(MARGE + LARGEUR_UTILE - largeurDroite, y);
        flux.showText(droite);
        flux.endText();

        return y - hauteurLigne;
    }

    private static float separateur(PDPageContentStream flux, float y) throws IOException {
        float yLigne = y - 3f;
        flux.setLineWidth(0.7f);
        flux.moveTo(MARGE, yLigne);
        flux.lineTo(MARGE + LARGEUR_UTILE, yLigne);
        flux.stroke();
        return y - H_SEPARATEUR;
    }

    private static String tronquer(String texte, PDFont police, float taille, float largeurMax) throws IOException {
        if (texte == null) {
            return "";
        }
        String resultat = texte;
        while (police.getStringWidth(resultat) / 1000f * taille > largeurMax && resultat.length() > 1) {
            resultat = resultat.substring(0, resultat.length() - 1);
        }
        if (!resultat.equals(texte) && resultat.length() > 1) {
            resultat = resultat.substring(0, resultat.length() - 1) + ".";
        }
        return resultat;
    }
}                                                                                                                                                                                                                                                                           