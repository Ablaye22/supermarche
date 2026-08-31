package com.supermarche.service;

import com.supermarche.dao.CommandeFournisseurDao;
import com.supermarche.dao.FournisseurDao;
import com.supermarche.dao.StockDao;
import com.supermarche.exception.SupermarcheException;
import com.supermarche.model.CommandeFournisseur;
import com.supermarche.model.Fournisseur;
import com.supermarche.model.LigneCommandeFournisseur;
import com.supermarche.model.StatutCommande;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class FournisseurService {

    private final FournisseurDao fournisseurDao = new FournisseurDao();
    private final CommandeFournisseurDao commandeDao = new CommandeFournisseurDao();
    private final StockDao stockDao = new StockDao();
    private final AuthService authService;
    private final AuditService auditService = new AuditService();

    public FournisseurService(AuthService authService) {
        this.authService = authService;
    }

    public Fournisseur creerFournisseur(Fournisseur f) {
        authService.exigerPermission("FOURNISSEUR_GERER");
        validerFournisseur(f);
        Fournisseur cree = fournisseurDao.creer(f);
        auditService.enregistrer(idUtilisateurConnecte(), "FOURNISSEUR_CREE", "fournisseurs", cree.getId().longValue(), null);
        return cree;
    }

    public void modifierFournisseur(Fournisseur f) {
        authService.exigerPermission("FOURNISSEUR_GERER");
        validerFournisseur(f);
        fournisseurDao.modifier(f);
        auditService.enregistrer(idUtilisateurConnecte(), "FOURNISSEUR_MODIFIE", "fournisseurs", f.getId().longValue(), null);
    }

    public List<Fournisseur> listerTous() {
        return fournisseurDao.listerTous(false);
    }

    public Optional<Fournisseur> trouverParId(int id) {
        return fournisseurDao.trouverParId(id);
    }

    /** Cree une commande fournisseur en BROUILLON, a passer ensuite a ENVOYEE via changerStatut. */
    public CommandeFournisseur creerCommande(int idFournisseur, LocalDate dateReceptionPrevue,
                                              List<LigneCommandeFournisseur> lignes) {
        authService.exigerPermission("FOURNISSEUR_GERER");
        if (lignes == null || lignes.isEmpty()) {
            throw new IllegalArgumentException("Une commande doit contenir au moins une ligne.");
        }

        CommandeFournisseur commande = new CommandeFournisseur();
        commande.setIdFournisseur(idFournisseur);
        commande.setDateCommande(LocalDate.now());
        commande.setDateReceptionPrevue(dateReceptionPrevue);
        commande.setStatut(StatutCommande.BROUILLON);
        commande.setIdUtilisateur(idUtilisateurConnecte());
        lignes.forEach(commande::ajouterLigne);

        CommandeFournisseur creee = commandeDao.creerAvecLignes(commande);
        auditService.enregistrer(idUtilisateurConnecte(), "COMMANDE_FOURNISSEUR_CREEE",
                "commandes_fournisseur", creee.getId(), null);
        return creee;
    }

    public void changerStatutCommande(long idCommande, StatutCommande nouveauStatut) {
        authService.exigerPermission("FOURNISSEUR_GERER");
        commandeDao.mettreAJourStatut(idCommande, nouveauStatut);
        auditService.enregistrer(idUtilisateurConnecte(), "COMMANDE_FOURNISSEUR_STATUT_CHANGE",
                "commandes_fournisseur", idCommande, nouveauStatut.name());
    }

    /**
     * Enregistre la reception (totale ou partielle) d'une commande
     * fournisseur. Pour chaque ligne, la quantite_recue indiquee dans
     * `quantitesRecues` (cle = id de la ligne) est ajoutee au stock et
     * persistee ; toute ligne absente de la map est consideree non recue
     * lors de cet appel (utile pour une reception en plusieurs livraisons).
     * Le statut de la commande devient RECUE si toutes les lignes sont
     * desormais completes, sinon RECUE_PARTIELLE.
     */
    public void receptionnerCommande(long idCommande, int idDepot, java.util.Map<Long, Integer> quantitesRecues) {
        authService.exigerPermission("STOCK_GERER");

        CommandeFournisseur commande = commandeDao.trouverParId(idCommande)
                .orElseThrow(() -> new SupermarcheException("Commande introuvable (id=" + idCommande + ")"));

        try (java.sql.Connection cnx = com.supermarche.config.DatabaseConfig.getConnection()) {
            cnx.setAutoCommit(false);
            try {
                boolean toutesCompletes = true;
                for (LigneCommandeFournisseur ligne : commande.getLignes()) {
                    Integer quantiteRecueCetteFois = quantitesRecues.get(ligne.getId());
                    if (quantiteRecueCetteFois != null && quantiteRecueCetteFois > 0) {
                        int nouvelleQuantiteRecue = Math.min(
                                ligne.getQuantiteRecue() + quantiteRecueCetteFois, ligne.getQuantiteCommandee());
                        commandeDao.mettreAJourQuantiteRecue(cnx, ligne.getId(), nouvelleQuantiteRecue);
                        stockDao.enregistrerEntree(cnx, ligne.getIdProduit(), idDepot, quantiteRecueCetteFois,
                                idUtilisateurConnecte(), "Commande #" + idCommande);
                        ligne.setQuantiteRecue(nouvelleQuantiteRecue);
                    }
                    if (!ligne.estEntierementRecue()) {
                        toutesCompletes = false;
                    }
                }

                StatutCommande nouveauStatut = toutesCompletes ? StatutCommande.RECUE : StatutCommande.RECUE_PARTIELLE;
                commandeDao.mettreAJourStatut(cnx, idCommande, nouveauStatut);
                cnx.commit();

                auditService.enregistrer(idUtilisateurConnecte(), "COMMANDE_FOURNISSEUR_RECEPTIONNEE",
                        "commandes_fournisseur", idCommande, nouveauStatut.name());
            } catch (Exception e) {
                cnx.rollback();
                throw e;
            }
        } catch (java.sql.SQLException e) {
            throw new com.supermarche.exception.AccesDonneesException("Erreur lors de la reception de la commande", e);
        }
    }

    public List<CommandeFournisseur> listerCommandesParStatut(StatutCommande statut) {
        return commandeDao.listerParStatut(statut);
    }

    private void validerFournisseur(Fournisseur f) {
        if (f.getRaisonSociale() == null || f.getRaisonSociale().isBlank()) {
            throw new IllegalArgumentException("La raison sociale du fournisseur est obligatoire.");
        }
    }

    private Integer idUtilisateurConnecte() {
        return authService.getUtilisateurConnecte() != null ? authService.getUtilisateurConnecte().getId() : null;
    }
}
