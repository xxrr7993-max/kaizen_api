package org.rod.kaizen_api.models;

import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Entity
@Table(name = "TB_SUBTASK_CHECKINS")
@Data
public class SubtaskCheckinModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID scId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vc_id", nullable = false)
    private VictoryCheckinModel victoryCheckin;

    @Column(nullable = false)
    private int subtaskIndex;

    @Column(nullable = false)
    private boolean completed = false;
}
