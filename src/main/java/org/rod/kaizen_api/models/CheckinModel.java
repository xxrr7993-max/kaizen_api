package org.rod.kaizen_api.models;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "TB_CHECKINS", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "date"})
})
@Data
public class CheckinModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID checkinId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserModel user;

    @Column(nullable = false)
    private LocalDate date;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "checkin", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<VictoryCheckinModel> victoryCheckins = new ArrayList<>();

    @OneToMany(mappedBy = "checkin", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<DiscardableTaskModel> discardableTasks = new ArrayList<>();
}
