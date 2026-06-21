package org.rod.kaizen_api.repositories;

import org.rod.kaizen_api.models.CheckinModel;
import org.rod.kaizen_api.models.UserModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CheckinRepository extends JpaRepository<CheckinModel, UUID> {
    Optional<CheckinModel> findByUserAndDate(UserModel user, LocalDate date);
    List<CheckinModel> findByUserAndDateBetweenOrderByDateAsc(UserModel user, LocalDate start, LocalDate end);
    List<CheckinModel> findByUserOrderByDateDesc(UserModel user);

    @Query("SELECT COUNT(DISTINCT c) FROM CheckinModel c JOIN c.victoryCheckins vc WHERE c.user = :user AND vc.completed = true")
    long countActiveDays(UserModel user);

}
