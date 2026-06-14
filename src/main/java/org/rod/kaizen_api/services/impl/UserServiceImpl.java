package org.rod.kaizen_api.services.impl;

import org.rod.kaizen_api.dtos.UserProfileDto;
import org.rod.kaizen_api.exceptions.ConflictException;
import org.rod.kaizen_api.exceptions.NotFoundException;
import org.rod.kaizen_api.models.UserModel;
import org.rod.kaizen_api.repositories.RefreshTokenRepository;
import org.rod.kaizen_api.repositories.UserRepository;
import org.rod.kaizen_api.repositories.VictoryRepository;
import org.rod.kaizen_api.services.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final VictoryRepository victoryRepository;

    public UserServiceImpl(UserRepository userRepository,
                           RefreshTokenRepository refreshTokenRepository,
                           VictoryRepository victoryRepository) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.victoryRepository = victoryRepository;
    }

    @Override
    public UserProfileDto getProfile(String userId) {
        return toProfileDto(findById(UUID.fromString(userId)));
    }

    @Override
    @Transactional
    public UserProfileDto updateProfile(String userId, String name, String email) {
        UserModel user = findById(UUID.fromString(userId));
        if (name != null && !name.isBlank()) user.setName(name);
        if (email != null && !email.isBlank()) {
            if (!email.equals(user.getEmail()) && userRepository.existsByEmail(email)) {
                throw new ConflictException("Email already in use");
            }
            user.setEmail(email);
        }
        return toProfileDto(userRepository.save(user));
    }

    @Override
    @Transactional
    public void deleteAccount(String userId) {
        UserModel user = findById(UUID.fromString(userId));
        refreshTokenRepository.deleteByUser(user);
        victoryRepository.deleteByUser(user);
        userRepository.delete(user);
    }

    @Override
    public UserModel findById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private UserProfileDto toProfileDto(UserModel user) {
        return new UserProfileDto(
                user.getUserId(), user.getName(), user.getEmail(),
                user.getCreatedAt(), user.getStreakCurrent(), user.getStreakRecord()
        );
    }
}
