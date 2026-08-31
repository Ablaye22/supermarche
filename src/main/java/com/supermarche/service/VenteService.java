package com.supermarche.service;

import com.supermarche.config.DatabaseConfig;
import com.supermarche.dao.ClientDao;
import com.supermarche.dao.SessionCaisseDao;
import com.supermarche.dao.StockDao;
import com.supermarche.dao.VenteDao;
import com.supermarche.exception.AccesDonneesException;
import com.supermarche.exception.SupermarcheException;
import com.supermarche.model.LigneVente;
import com.supermarche.model.Paiement;
import com.supermarche.model.SessionCaisse;
import com.supermarche.model.StatutSession;
import com.supermarche.model.StatutVente;
import com.supermarche.model.Vente;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Orchestre l'enregistrement complet d'une vente : creation du ticket,
 * decrementation du stock, enregistrement des paiements, et points de
 * fidelite, le tout dans une UNIQUE transaction base de donnees.
 *
 * Pourquoi une seule transaction : si on encaissait la vente puis qu'on
 * decrementait le stock dans deux operations separees, une panne entre
 * les deux laisserait soit une vente sans sortie de stock (incoherence
 * comptable), soit un stock decrement sans vente correspondante. Avec
 * une transaction unique, soit tout reussit, soit rien n'est applique.
 */
public class VenteService {

    private final VenteDao venteDao;
    private final StockDao stockDao;
    private final ClientDao clientDao;
    private final SessionCaisseDao sessionCaisseDao;
    private final AuthService authService;
    private final AuditService auditService;

    /** Compteur en memoire pour generer un numero de ticket lisible en plus de l'id auto-incremente. */
    private static final AtomicInteger compteurTicketsEnSession = new AtomicInteger(0);
    private static final DateTimeFormatter FORMAT_HORODATAGE_TICKET = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public VenteService(AuthService authService) {
        this(authService, new VenteDao(), new StockDao(), new ClientDao(), new SessionCaisseDao(), new AuditService());
    }

    /** Constructeur utilise par les tests pour injecter des DAO simules. */
    public VenteService(AuthService authService, VenteDao venteDao, StockDao stockDao, ClientDao clientDao,
                         SessionCaisseDao sessionCaisseDao, AuditService auditService) {
        this.authService = authService;
        this.venteDao = venteDao;
        this.stockDao = stockDao;
        this.clientDao = clientDao;
        this.sessionCaisseDao = sessionCaisseDao;
        this.auditService = auditService;
    }

    /**
     * Enregistre une vente complete. La liste de lignes doit deja contenir
     * le produit, la quantite et le prix au moment de l'achat (calcules en
     * amont par le controleur de caisse / panier).
     *
     * @param idDepot depot dont le stock doit etre decrement (celui rattache a la caisse)
     */
    public Vente enregistrerVente(long idSession, int idDepot, Integer idClient,
                                   List<LigneVente> lignes, List<Paiement> paiements) {
        authService.exigerPermission("VENTE_CREER");

        if (lignes == null || lignes.isEmpty()) {
            throw new IllegalArgumentException("Une vente doit contenir au moins une ligne.");
        }

        Vente vente = new Vente();
        vente.setNumeroTicket(genererNumeroTicket());
        vente.setIdSession(idSession);
        vente.setIdClient(idClient);
        vente.setLignes(lignes);
        vente.setStatut(StatutVente.VALIDEE);
        CalculateurVente.calculerTotaux(vente);

        BigDecimal totalPaye = paiements.stream().map(Paiement::getMontant).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalPaye.compareTo(vente.getTotalTtc()) < 0) {
            throw new SupermarcheException(
                    "Le montant paye (" + totalPaye + ") est inferieur au total de la vente (" + vente.getTotalTtc() + ").");
        }

        Integer idUtilisateur = idUtilisateurConnecte();

        try (Connection cnx = DatabaseConfig.getConnection()) {
            cnx.setAutoCommit(false);
            try {
                venteDao.creerVenteEtLignes(cnx, vente);

                for (LigneVente ligne : lignes) {
                    // Les quantites au poids (decimales) ne s'appliquent pas a la
                    // decrementation entiere du stock pour les unites comptables ; on
                    // arrondit au superieur pour ne jamais sous-decompter le stock.
                    int quantiteEntiere = ligne.getQuantite().setScale(0, RoundingMode.CEILING).intValue();
                    stockDao.decrementerPourVente(cnx, ligne.getIdProduit(), idDepot, quantiteEntiere,
                            idUtilisateur, vente.getNumeroTicket());
                }

                venteDao.enregistrerPaiements(cnx, vente.getId(), paiements);
                vente.setPaiements(paiements);

                if (idClient != null) {
                    int points = vente.getTotalTtc().setScale(0, RoundingMode.FLOOR).intValue();
                    clientDao.ajouterPointsFidelite(cnx, idClient, points);
                }

                cnx.commit();
            } catch (Exception e) {
                cnx.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new AccesDonneesException("Erreur lors de l'enregistrement de la vente", e);
        }

        auditService.enregistrer(idUtilisateur, "VENTE_CREEE", "ventes", vente.getId(),
                "Ticket " + vente.getNumeroTicket() + " - Total : " + vente.getTotalTtc());

        return vente;
    }

    /**
     * Annule une vente validee : remet le stock et marque la vente comme
     * annulee. Ne supprime jamais la ligne en base (tracabilite comptable
     * et fiscale obligatoire : un ticket annule doit rester visible).
     */
    public void annulerVente(long idVente) {
        authService.exigerPermission("VENTE_ANNULER");

        Integer idUtilisateur = idUtilisateurConnecte();

        try (Connection cnx = DatabaseConfig.getConnection()) {
            cnx.setAutoCommit(false);
            try {
                Optional<Vente> ventOpt = venteDao.trouverParId(cnx, idVente);
                if (ventOpt.isEmpty()) {
                    throw new SupermarcheException("Vente introuvable (id=" + idVente + ")");
                }
                Vente vente = ventOpt.get();
                if (vente.getStatut() != StatutVente.VALIDEE) {
                    throw new SupermarcheException("Seule une vente validee peut etre annulee (statut actuel : "
                            + vente.getStatut() + ")");
                }

                SessionCaisse session = sessionCaisseDao.trouverParId(vente.getIdSession())
                        .orElseThrow(() -> new SupermarcheException("Session de caisse introuvable pour cette vente."));

                for (LigneVente ligne : vente.getLignes()) {
                    int quantiteEntiere = ligne.getQuantite().setScale(0, RoundingMode.CEILING).intValue();
                    // Le depot n'est pas stocke sur la vente elle-meme : on le retrouve via la caisse de la session.
                    stockDao.reintegrerApresAnnulation(cnx, ligne.getIdProduit(),
                            retrouverIdDepotPourCaisse(session.getIdCaisse()),
                            quantiteEntiere, idUtilisateur, vente.getNumeroTicket());
                }

                venteDao.mettreAJourStatut(cnx, idVente, StatutVente.ANNULEE);
                cnx.commit();
            } catch (Exception e) {
                cnx.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new AccesDonneesException("Erreur lors de l'annulation de la vente", e);
        }

        auditService.enregistrer(idUtilisateur, "VENTE_ANNULEE", "ventes", idVente, null);
    }

    public Optional<Vente> trouverParId(long id) {
        return venteDao.trouverParId(id);
    }

    public List<Vente> listerParSession(long idSession) {
        return venteDao.listerParSession(idSession);
    }

    private String genererNumeroTicket() {
        String horodatage = LocalDateTime.now().format(FORMAT_HORODATAGE_TICKET);
        int compteur = compteurTicketsEnSession.incrementAndGet();
        return "T-" + horodatage + "-" + String.format("%04d", compteur % 10000);
    }

    private int retrouverIdDepotPourCaisse(int idCaisse) {
        return new com.supermarche.dao.CaisseDao().trouverParId(idCaisse)
                .map(c -> c.getIdDepot())
                .orElseThrow(() -> new SupermarcheException("Caisse introuvable (id=" + idCaisse + ")"));
    }

    private Integer idUtilisateurConnecte() {
        return authService.getUtilisateurConnecte() != null ? authService.getUtilisateurConnecte().getId() : null;
    }
}
