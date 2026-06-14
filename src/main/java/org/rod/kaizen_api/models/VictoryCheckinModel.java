package org.rod.kaizen_api.models;

import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "TB_VICTORY_CHECKINS")
@Data
public class VictoryCheckinModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID vcId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checkin_id", nullable = false)
    private CheckinModel checkin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "victory_id", nullable = false)
    private VictoryModel victory;

    @Column(nullable = false)
    private boolean completed = false;

    @OneToMany(mappedBy = "victoryCheckin", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<SubtaskCheckinModel> subtaskCheckins = new ArrayList<>();
}
