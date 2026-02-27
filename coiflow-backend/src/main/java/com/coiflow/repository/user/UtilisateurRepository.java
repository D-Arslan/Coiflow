package com.coiflow.repository.user;

import com.coiflow.model.user.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, String> {

    Optional<Utilisateur> findByEmail(String email);

    boolean existsByEmail(String email);

    List<Utilisateur> findBySalon_IdAndActive(String salonId, boolean active);

    List<Utilisateur> findBySalon_Id(String salonId);

    @Query("SELECT u FROM Utilisateur u WHERE u.salon.id = :salonId AND TYPE(u) = :type AND u.active = :active")
    List<Utilisateur> findBySalonIdAndTypeAndActive(
            @Param("salonId") String salonId,
            @Param("type") Class<? extends Utilisateur> type,
            @Param("active") boolean active);

    @Query("SELECT u FROM Utilisateur u WHERE u.id = :id AND TYPE(u) = :type")
    Optional<Utilisateur> findByIdAndType(
            @Param("id") String id,
            @Param("type") Class<? extends Utilisateur> type);
}
