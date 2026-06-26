package com.supermarche.dao;

import com.supermarche.config.DatabaseConfig;
import com.supermarche.exception.AccesDonneesException;
import com.supermarche.model.CodeRole;
import com.supermarche.model.Role;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class RoleDao {

    public Optional<Role> trouverParId(int id) {
        String sql = "SELECT * FROM roles WHERE id_role = ?";
        try (Connection cnx = DatabaseConfig.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Role role = mapper(rs);
                    role.setPermissions(chargerPermissions(cnx, id));
                    return Optional.of(role);
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new AccesDonneesException("Erreur lors de la recherche du role id=" + id, e);
        }
    }

    public Optional<Role> trouverParCode(CodeRole code) {
        String sql = "SELECT * FROM roles WHERE code = ?";
        try (Connection cnx = DatabaseConfig.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, code.name());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Role role = mapper(rs);
                    role.setPermissions(chargerPermissions(cnx, role.getId()));
                    return Optional.of(role);
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new AccesDonneesException("Erreur lors de la recherche du role " + code, e);
        }
    }

    public List<Role> listerTous() {
        String sql = "SELECT * FROM roles ORDER BY libelle";
        List<Role> roles = new ArrayList<>();
        try (Connection cnx = DatabaseConfig.getConnection();
             PreparedStatement ps = cnx.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Role role = mapper(rs);
                role.setPermissions(chargerPermissions(cnx, role.getId()));
                roles.add(role);
            }
            return roles;
        } catch (SQLException e) {
            throw new AccesDonneesException("Erreur lors de la liste des roles", e);
        }
    }

    private Set<String> chargerPermissions(Connection cnx, int idRole) throws SQLException {
        String sql = """
                SELECT p.code FROM permissions p
                JOIN role_permissions rp ON rp.id_permission = p.id_permission
                WHERE rp.id_role = ?
                """;
        Set<String> permissions = new HashSet<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, idRole);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    permissions.add(rs.getString("code"));
                }
            }
        }
        return permissions;
    }

    private Role mapper(ResultSet rs) throws SQLException {
        Role role = new Role();
        role.setId(rs.getInt("id_role"));
        role.setCode(CodeRole.valueOf(rs.getString("code")));
        role.setLibelle(rs.getString("libelle"));
        role.setDescription(rs.getString("description"));
        return role;
    }
}
