package com.supermarche.util;

import com.supermarche.service.RapportService;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Export PDF du rapport de ventes (synthese, ventilation par jour,
 * meilleures ventes) genere depuis l'ecran Rapports.
 *
 * Utilise les polices standard PDFBox (Helvetica), suffisantes pour un
 * document interne simple -- pas d'embarquement de police necessaire.
 * PDFBox 3.x n'expose plus PDType1Font.HELVETICA comme champ statique :
 * on construit la police via Standard14Fonts.FontName.
 */
public final class RapportPdfExporter {

    private static final float MARGE = 50f;
    private static final float LARGEUR_PAGE = PDRectangle.A4.getWidth();
    private static final float HAUTEUR_PAGE = PDRectangle.A4.getHeight();
    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final PDFont HELVETICA = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDFont HELVETICA_BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    private RapportPdfExporter() {
    }

    public static void exporter(File fichier,
                                 LocalDate debut,
                                 LocalDate fin,
                                 RapportService.SyntheseVentes synthese,
                                 List<RapportService.VentilationParJour> parJour,
                                 List<RapportService.ProduitVendu> topProduits) throws IOException {

        try (PDDocument document = new PDDocument()) {
            Curseur curseur = new Curseur(document);

            curseur.titre("Rapport de ventes");
            curseur.texte("Periode du " + debut.format(FORMAT_DATE) + " au " + fin.format(FORMAT_DATE),
                    HELVETICA, 11);
            curseur.sautLigne(14);

            curseur.sousTitre("Synthese");
            curseur.texte("Chiffre d'affaires TTC : " + FormatUtil.montant(synthese.chiffreAffairesTtc()), HELVETICA, 11);
            curseur.texte("Chiffre d'affaires HT  : " + FormatUtil.montant(synthese.chiffreAffairesHt()), HELVETICA, 11);
            curseur.texte("TVA collectee          : " + FormatUtil.montant(synthese.totalTva()), HELVETICA, 11);
            curseur.texte("Nombre de ventes        : " + synthese.nombreVentes(), HELVETICA, 11);
            curseur.sautLigne(16);

            curseur.sousTitre("Ventes par jour");
            curseur.enteteTableau(new String[]{"Jour", "Total TTC", "Nb ventes"}, new float[]{150, 200, 120});
            for (RapportService.VentilationParJour ligne : parJour) {
                curseur.ligneTableau(new String[]{
                        ligne.jour().format(FORMAT_DATE),
                        FormatUtil.montant(ligne.totalTtc()),
                        String.valueOf(ligne.nombreVentes())
                }, new float[]{150, 200, 120});
            }
            curseur.sautLigne(16);

            curseur.sousTitre("Meilleures ventes");
            curseur.enteteTableau(new String[]{"Produit", "Quantite", "CA TTC"}, new float[]{260, 100, 110});
            for (RapportService.ProduitVendu ligne : topProduits) {
                curseur.ligneTableau(new String[]{
                        ligne.designation(),
                        ligne.quantiteTotale().toPlainString(),
                        FormatUtil.montant(ligne.chiffreAffairesTtc())
                }, new float[]{260, 100, 110});
            }

            curseur.fermer();
            document.save(fichier);
        }
    }

    /**
     * Petit curseur d'ecriture qui gere la position verticale et cree
     * automatiquement une nouvelle page quand on approche du bas de page.
     */
    private static final class Curseur {
        private final PDDocument document;
        private PDPageContentStream flux;
        private float y;

        Curseur(PDDocument document) throws IOException {
            this.document = document;
            nouvellePage();
        }

        private void nouvellePage() throws IOException {
            if (flux != null) {
                flux.close();
            }
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            flux = new PDPageContentStream(document, page);
            y = HAUTEUR_PAGE - MARGE;
        }

        private void assurerEspace(float hauteurNecessaire) throws IOException {
            if (y - hauteurNecessaire < MARGE) {
                nouvellePage();
            }
        }

        void titre(String texte) throws IOException {
            assurerEspace(26);
            ecrire(texte, HELVETICA_BOLD, 18);
            y -= 26;
        }

        void sousTitre(String texte) throws IOException {
            assurerEspace(20);
            ecrire(texte, HELVETICA_BOLD, 13);
            y -= 20;
        }

        void texte(String texte, PDFont police, float taille) throws IOException {
            assurerEspace(16);
            ecrire(texte, police, taille);
            y -= 16;
        }

        void sautLigne(float hauteur) throws IOException {
            assurerEspace(hauteur);
            y -= hauteur;
        }

        void enteteTableau(String[] colonnes, float[] largeurs) throws IOException {
            assurerEspace(18);
            float x = MARGE;
            for (int i = 0; i < colonnes.length; i++) {
                flux.beginText();
                flux.setFont(HELVETICA_BOLD, 10);
                flux.newLineAtOffset(x, y);
                flux.showText(colonnes[i]);
                flux.endText();
                x += largeurs[i];
            }
            y -= 6;
            flux.setLineWidth(0.5f);
            flux.moveTo(MARGE, y);
            flux.lineTo(LARGEUR_PAGE - MARGE, y);
            flux.stroke();
            y -= 12;
        }

        void ligneTableau(String[] valeurs, float[] largeurs) throws IOException {
            assurerEspace(14);
            float x = MARGE;
            for (int i = 0; i < valeurs.length; i++) {
                flux.beginText();
                flux.setFont(HELVETICA, 10);
                flux.newLineAtOffset(x, y);
                flux.showText(valeurs[i] == null ? "" : valeurs[i]);
                flux.endText();
                x += largeurs[i];
            }
            y -= 14;
        }

        private void ecrire(String texte, PDFont police, float taille) throws IOException {
            flux.beginText();
            flux.setFont(police, taille);
            flux.newLineAtOffset(MARGE, y);
            flux.showText(texte);
            flux.endText();
        }

        void fermer() throws IOException {
            flux.close();
        }
    }
}