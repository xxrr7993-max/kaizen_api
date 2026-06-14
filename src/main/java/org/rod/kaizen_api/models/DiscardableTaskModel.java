package org.rod.kaizen_api.models;

import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Entity
@Table(name = "TB_DISCARDABLE_TASKS")
@Data
public class DiscardableTaskModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID taskId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "checkin_id", nullable = false)
    private CheckinModel checkin;

    @Column(nullable = false, length = 200)
    private String goal;

    @Column(nullable = false)
    private boolean completed = false;
}
