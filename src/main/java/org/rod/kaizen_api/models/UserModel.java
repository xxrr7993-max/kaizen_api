package org.rod.kaizen_api.models;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.rod.kaizen_api.enums.UserStatus;
import org.rod.kaizen_api.enums.UserType;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "TB_USERS")
@Data
public class UserModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID userId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserType userType = UserType.USER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus userStatus = UserStatus.ACTIVE;

    @Column(nullable = false)
    private int streakCurrent = 0;

    @Column(nullable = false)
    private int streakRecord = 0;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Column(length = 36)
    private String passwordResetToken;

    private LocalDateTime passwordResetTokenExpiry;
}
