package com.supermarche.util;

import com.supermarche.model.Employe;
import com.supermarche.model.Planning;
import com.supermarche.model.Utilisateur;
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
import java.util.Map;

/**
 * Genere un PDF de la grille hebdomadaire du planning : une ligne par
 * employe, une colonne par jour (Lundi a Dimanche). Chaque case affiche
 * "Libre" ou l'intervalle de travail (ex: 15:30 - 21:30).
 */
public final class PlanningPdfExporter {

    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMAT_JOUR = DateTimeFormatter.ofPattern("dd/MM");
    private static final String[] NOMS_JOURS = {
            "Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche"
    };

    private static final float MARGE = 30f;
    private static final float HAUTEUR_TITRE = 40f;
    private static final float HAUTEUR_ENTETE = 26f;
    private static final float HAUTEUR_LIGNE = 22f;
    private static final float LARGEUR_COL_EMPLOYE = 140f;

    private static final PDFont POLICE_TITRE = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final PDFont POLICE_ENTETE = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
    private static final PDFont POLICE_TEXTE = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

    private PlanningPdfExporter() {
    }

    /**
     * @param fichier                     fichier PDF de destination
     * @param lundi                       lundi de la semaine a exporter
     * @param employes                    employes a afficher (une ligne chacun)
     * @param planningParEmployeEtDate    plannings indexes par id Employe puis par date
     */
    public static void exporter(
            File fichier,
            LocalDate lundi,
            List<Utilisateur> employes,
            Map<Integer, Map<LocalDate, Planning>> planningParEmployeEtDate
    ) throws IOException {

        LocalDate dimanche = lundi.plusDays(6);
        String titre = "Planning du " + FORMAT_DATE.format(lundi) + " au " + FORMAT_DATE.format(dimanche);

        try (PDDocument document = new PDDocument()) {

            PDRectangle taillePage = new PDRectangle(PDRectangle.A4.getHeight(), PDRectangle.A4.getWidth());
            float largeurUtile = taillePage.getWidth() - 2 * MARGE;
            float largeurColJour = (largeurUtile - LARGEUR_COL_EMPLOYE) / 7f;

            float[] largeursColonnes = new float[8];
            largeursColonnes[0] = LARGEUR_COL_EMPLOYE;
            for (int i = 1; i < 8; i++) {
                largeursColonnes[i] = largeurColJour;
            }

            String[] entetesColonnes = new String[8];
            entetesColonnes[0] = "Employe";
            for (int i = 0; i < 7; i++) {
                entetesColonnes[i + 1] = NOMS_JOURS[i] + " " + FORMAT_JOUR.format(lundi.plusDays(i));
            }

            PDPage page = new PDPage(taillePage);
            document.addPage(page);
            PDPageContentStream flux = new PDPageContentStream(document, page);

            float y = taillePage.getHeight() - MARGE;
            y = ecrireTitre(flux, titre, taillePage, y);
            y = dessinerEntete(flux, MARGE, y, largeursColonnes, entetesColonnes);

            float limiteBasse = MARGE + HAUTEUR_LIGNE;

            for (Utilisateur utilisateur : employes) {

                if (y < limiteBasse) {
                    flux.close();
                    page = new PDPage(taillePage);
                    document.addPage(page);
                    flux = new PDPageContentStream(document, page);
                    y = taillePage.getHeight() - MARGE;
                    y = dessinerEntete(flux, MARGE, y, largeursColonnes, entetesColonnes);
                }

                Employe employe = utilisateur.getEmploye();
                String nomComplet = (employe == null) ? "" : employe.getPrenom() + " " + employe.getNom();

                Map<LocalDate, Planning> parDate = (employe == null || employe.getId() == null)
                        ? null
                        : planningParEmployeEtDate.get(employe.getId());

                String[] valeurs = new String[8];
                valeurs[0] = nomComplet;
                for (int i = 0; i < 7; i++) {
                    LocalDate jour = lundi.plusDays(i);
                    Planning p = (parDate == null) ? null : parDate.get(jour);
                    valeurs[i + 1] = (p == null)
                            ? "Libre"
                            : p.getHeureDebut() + " - " + p.getHeureFin();
                }

                dessinerLigne(flux, MARGE, y, largeursColonnes, valeurs);
                y -= HAUTEUR_LIGNE;
            }

            flux.close();
            document.save(fichier);
        }
    }

    private static float ecrireTitre(PDPageContentStream flux, String titre, PDRectangle page, float y) throws IOException {
        float taillePolice = 16f;
        float largeurTexte = POLICE_TITRE.getStringWidth(titre) / 1000f * taillePolice;
        float x = (page.getWidth() - largeurTexte) / 2f;

        flux.beginText();
        flux.setFont(POLICE_TITRE, taillePolice);
        flux.newLineAtOffset(x, y);
        flux.showText(titre);
        flux.endText();

        return y - HAUTEUR_TITRE;
    }

    private static float dessinerEntete(PDPageContentStream flux, float x, float y, float[] largeurs, String[] entetes) throws IOException {
        dessinerCadre(flux, x, y, largeurs, HAUTEUR_ENTETE);
        float cx = x;
        for (int i = 0; i < entetes.length; i++) {
            ecrireCentre(flux, entetes[i], cx, y - HAUTEUR_ENTETE / 2f - 3.5f, largeurs[i], POLICE_ENTETE, 8.5f);
            cx += largeurs[i];
        }
        return y - HAUTEUR_ENTETE;
    }

    private static void dessinerLigne(PDPageContentStream flux, float x, float y, float[] largeurs, String[] valeurs) throws IOException {
        dessinerCadre(flux, x, y, largeurs, HAUTEUR_LIGNE);
        float cx = x;
        for (int i = 0; i < valeurs.length; i++) {
            if (i == 0) {
                ecrireGauche(flux, valeurs[i], cx + 4, y - HAUTEUR_LIGNE / 2f - 3f, largeurs[i] - 8, POLICE_TEXTE, 8.5f);
            } else {
                ecrireCentre(flux, valeurs[i], cx, y - HAUTEUR_LIGNE / 2f - 3f, largeurs[i], POLICE_TEXTE, 8.5f);
            }
            cx += largeurs[i];
        }
    }

    private static void dessinerCadre(PDPageContentStream flux, float x, float y, float[] largeurs, float hauteur) throws IOException {
        float largeurTotale = 0;
        for (float l : largeurs) {
            largeurTotale += l;
        }

        flux.setLineWidth(0.6f);
        flux.moveTo(x, y);
        flux.lineTo(x + largeurTotale, y);
        flux.stroke();

        flux.moveTo(x, y - hauteur);
        flux.lineTo(x + largeurTotale, y - hauteur);
        flux.stroke();

        float cx = x;
        for (float l : largeurs) {
            flux.moveTo(cx, y);
            flux.lineTo(cx, y - hauteur);
            flux.stroke();
            cx += l;
        }
        flux.moveTo(cx, y);
        flux.lineTo(cx, y - hauteur);
        flux.stroke();
    }

    private static void ecrireCentre(PDPageContentStream flux, String texte, float x, float y, float largeurColonne, PDFont police, float taille) throws IOException {
        String tronque = tronquer(texte, police, taille, largeurColonne - 6);
        float largeurTexte = police.getStringWidth(tronque) / 1000f * taille;
        float cx = x + (largeurColonne - largeurTexte) / 2f;
        flux.beginText();
        flux.setFont(police, taille);
        flux.newLineAtOffset(cx, y);
        flux.showText(tronque);
        flux.endText();
    }

    private static void ecrireGauche(PDPageContentStream flux, String texte, float x, float y, float largeurMax, PDFont police, float taille) throws IOException {
        String tronque = tronquer(texte, police, taille, largeurMax);
        flux.beginText();
        flux.setFont(police, taille);
        flux.newLineAtOffset(x, y);
        flux.showText(tronque);
        flux.endText();
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