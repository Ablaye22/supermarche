package com.supermarche.service;

import com.supermarche.dao.CaisseDao;
import com.supermarche.dao.SessionCaisseDao;
import com.supermarche.exception.SupermarcheException;
import com.supermarche.model.Caisse;
import com.supermarche.model.SessionCaisse;
import com.supermarche.model.StatutSession;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class CaisseService {

    private final CaisseDao caisseDao = new CaisseDao();
    private final SessionCaisseDao sessionCaisseDao = new SessionCaisseDao();
    private final AuthService authService;
    private final AuditService auditService = new AuditService();

    public CaisseService(AuthService authService) {
        this.authService = authService;
    }

    public List<Caisse> listerCaissesActives() {
        return caisseDao.listerActives();
    }

    public SessionCaisse ouvrirSession(int idCaisse, BigDecimal fondOuverture) {
        authService.exigerPermission("CAISSE_OUVRIR_FERMER");

        if (fondOuverture == null || fondOuverture.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Le fond d'ouverture doit etre positif ou nul.");
        }
        if (caisseDao.possedeSessionOuverte(idCaisse)) {
            throw new SupermarcheException("Cette caisse a deja une session ouverte. Fermez-la avant d'en ouvrir une nouvelle.");
        }

        SessionCaisse session = new SessionCaisse();
        session.setIdCaisse(idCaisse);
        session.setIdUtilisateur(authService.getUtilisateurConnecte().getId());
        session.setFondOuverture(fondOuverture);

        SessionCaisse creee = sessionCaisseDao.ouvrir(session);
        auditService.enregistrer(idUtilisateurConnecte(), "CAISSE_OUVERTE", "sessions_caisse",
                creee.getId(), "Fond d'ouverture : " + fondOuverture);
        return creee;
    }

    /**
     * Ferme une session de caisse. Le fond theorique est calcule a partir
     * du fond d'ouverture + especes encaissees pendant la session ; le
     * fond reel est saisi par le caissier apres comptage physique.
     * L'ecart (positif ou negatif) est trace pour permettre un controle
     * de gestion ulterieur.
     */
    public SessionCaisse fermerSession(long idSession, BigDecimal fondReel) {
        authService.exigerPermission("CAISSE_OUVRIR_FERMER");

        SessionCaisse session = sessionCaisseDao.trouverParId(idSession)
                .orElseThrow(() -> new SupermarcheException("Session de caisse introuvable (id=" + idSession + ")"));

        if (session.getStatut() != StatutSession.OUVERTE) {
            throw new SupermarcheException("Cette session est deja fermee.");
        }

        BigDecimal totalEspeces = sessionCaisseDao.calculerTotalEspecesEncaisse(idSession);
        BigDecimal fondTheorique = session.getFondOuverture().add(totalEspeces);
        BigDecimal ecart = fondReel.subtract(fondTheorique);

        sessionCaisseDao.fermer(idSession, fondTheorique, fondReel, ecart);

        auditService.enregistrer(idUtilisateurConnecte(), "CAISSE_FERMEE", "sessions_caisse", idSession,
                "Theorique : " + fondTheorique + " / Reel : " + fondReel + " / Ecart : " + ecart);

        session.setFondFermetureTheorique(fondTheorique);
        session.setFondFermetureReel(fondReel);
        session.setEcart(ecart);
        session.setStatut(StatutSession.FERMEE);
        return session;
    }

    public Optional<SessionCaisse> trouverSessionOuverte(int idCaisse) {
        return sessionCaisseDao.trouverSessionOuvertePourCaisse(idCaisse);
    }

    private Integer idUtilisateurConnecte() {
        return authService.getUtilisateurConnecte() != null ? authService.getUtilisateurConnecte().getId() : null;
    }
}
