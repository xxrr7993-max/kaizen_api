package org.rod.kaizen_api.models;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.UpdateTimestamp;
import org.rod.kaizen_api.enums.VictoryCategory;
import org.rod.kaizen_api.util.StringListConverter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "TB_VICTORIES")
@Data
@DynamicUpdate
public class VictoryModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID victoryId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserModel user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VictoryCategory category;

    @Column(nullable = false, length = 200)
    private String goal;

    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "text")
    private List<String> subtasks = new ArrayList<>();

    @Column(name = "sort_order", nullable = false)
    private int order = 0;

    @Column(nullable = false)
    private Boolean isLenient = true;

    private String dayOfWeek;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
