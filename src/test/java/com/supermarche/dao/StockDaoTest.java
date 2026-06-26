package com.supermarche.dao;

import com.supermarche.exception.StockInsuffisantException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests de la logique critique de gestion du stock : verrouillage avant
 * lecture (FOR UPDATE), rejet d'une vente quand le stock est insuffisant,
 * et tracabilite du mouvement enregistre. Connection/PreparedStatement/
 * ResultSet sont mockes pour isoler cette logique de toute vraie base
 * MariaDB tout en verifiant precisement le SQL execute.
 */
class StockDaoTest {

    private StockDao stockDao;
    private Connection cnxMock;
    private PreparedStatement psLectureMock;
    private PreparedStatement psAutreMock;
    private ResultSet rsMock;

    @BeforeEach
    void setUp() throws SQLException {
        stockDao = new StockDao();
        cnxMock = mock(Connection.class);
        psLectureMock = mock(PreparedStatement.class);
        psAutreMock = mock(PreparedStatement.class);
        rsMock = mock(ResultSet.class);
    }

    @Test
    void decrementerPourVenteEchoueSiStockInsuffisant() throws SQLException {
        // Le stock disponible (5) est inferieur a la quantite demandee (10).
        simulerLectureStock(5);

        StockInsuffisantException exception = assertThrows(StockInsuffisantException.class,
                () -> stockDao.decrementerPourVente(cnxMock, 1, 1, 10, 99, "T-TEST-0001"));

        assertTrue(exception.getMessage().contains("disponible: 5"));
        assertTrue(exception.getMessage().contains("demande: 10"));

        // Aucune mise a jour de quantite ne doit avoir lieu si le stock est insuffisant :
        // c'est l'invariant le plus important a verifier ici (pas de vente "a credit" sur le stock).
        verify(cnxMock, never()).prepareStatement(contains("UPDATE stocks"));
    }

    @Test
    void decrementerPourVenteReussitSiStockSuffisant() throws SQLException {
        simulerLectureStock(20);
        PreparedStatement psUpdate = mock(PreparedStatement.class);
        PreparedStatement psInsertMouvement = mock(PreparedStatement.class);
        when(cnxMock.prepareStatement(contains("UPDATE stocks"))).thenReturn(psUpdate);
        when(cnxMock.prepareStatement(contains("INSERT INTO mouvements_stock"))).thenReturn(psInsertMouvement);

        stockDao.decrementerPourVente(cnxMock, 1, 1, 7, 99, "T-TEST-0002");

        // Verifie que la nouvelle quantite (20 - 7 = 13) est bien celle ecrite en base.
        verify(psUpdate).setInt(1, 13);
        verify(psUpdate).executeUpdate();
        verify(psInsertMouvement).executeUpdate();
    }

    @Test
    void decrementerPourVenteUtiliseSelectForUpdatePourEviterLesVentesConcurrentesIncoherentes() throws SQLException {
        // Verification explicite du verrouillage pessimiste : sans "FOR UPDATE",
        // deux caisses pourraient lire la meme quantite avant que l'une des deux
        // n'ait ecrit sa decrementation, et vendre deux fois le meme dernier article.
        simulerLectureStock(10);
        when(cnxMock.prepareStatement(contains("UPDATE stocks"))).thenReturn(mock(PreparedStatement.class));
        when(cnxMock.prepareStatement(contains("INSERT INTO mouvements_stock"))).thenReturn(mock(PreparedStatement.class));

        stockDao.decrementerPourVente(cnxMock, 1, 1, 1, 99, "T-TEST-0003");

        verify(cnxMock).prepareStatement(argThat(sql -> sql.contains("FOR UPDATE")));
    }

    @Test
    void decrementerPourVenteAvecStockExactementSuffisantNeLeveAucuneException() throws SQLException {
        // Cas limite : exactement la quantite demandee est disponible (0 restant apres la vente).
        simulerLectureStock(3);
        when(cnxMock.prepareStatement(contains("UPDATE stocks"))).thenReturn(mock(PreparedStatement.class));
        when(cnxMock.prepareStatement(contains("INSERT INTO mouvements_stock"))).thenReturn(mock(PreparedStatement.class));

        // Ne doit pas lever StockInsuffisantException quand stock == quantite demandee.
        assertDoesNotThrow(() -> stockDao.decrementerPourVente(cnxMock, 1, 1, 3, 99, "T-TEST-0004"));
    }

    @Test
    void reintegrerApresAnnulationAugmenteLeStock() throws SQLException {
        simulerLectureStock(2);
        PreparedStatement psUpdate = mock(PreparedStatement.class);
        when(cnxMock.prepareStatement(contains("UPDATE stocks"))).thenReturn(psUpdate);
        when(cnxMock.prepareStatement(contains("INSERT INTO mouvements_stock"))).thenReturn(mock(PreparedStatement.class));

        stockDao.reintegrerApresAnnulation(cnxMock, 1, 1, 5, 99, "T-TEST-0005");

        // 2 (stock actuel) + 5 (quantite reintegree) = 7
        verify(psUpdate).setInt(1, 7);
    }

    @Test
    void decrementerPourVenteEchoueProprementSiAucuneLigneDeStockExisteEncorePourCeProduit() throws SQLException {
        // Aucune ligne de stock pour ce produit/depot : lireEtVerrouiller doit
        // la creer a 0 avant de poursuivre, ce qui doit ensuite declencher
        // StockInsuffisantException pour toute quantite demandee > 0.
        when(cnxMock.prepareStatement(contains("FOR UPDATE"))).thenReturn(psLectureMock);
        when(psLectureMock.executeQuery()).thenReturn(rsMock);
        when(rsMock.next()).thenReturn(false);
        PreparedStatement psInsertStock = mock(PreparedStatement.class);
        when(cnxMock.prepareStatement(contains("INSERT INTO stocks"))).thenReturn(psInsertStock);

        assertThrows(StockInsuffisantException.class,
                () -> stockDao.decrementerPourVente(cnxMock, 1, 1, 1, 99, "T-TEST-0006"));

        verify(psInsertStock).executeUpdate();
    }

    /** Simule le SELECT ... FOR UPDATE renvoyant une ligne existante avec la quantite donnee. */
    private void simulerLectureStock(int quantiteActuelle) throws SQLException {
        when(cnxMock.prepareStatement(contains("FOR UPDATE"))).thenReturn(psLectureMock);
        when(psLectureMock.executeQuery()).thenReturn(rsMock);
        when(rsMock.next()).thenReturn(true);
        when(rsMock.getInt("quantite")).thenReturn(quantiteActuelle);
    }
}
