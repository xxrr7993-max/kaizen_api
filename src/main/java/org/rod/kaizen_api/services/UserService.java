package org.rod.kaizen_api.services;

import org.rod.kaizen_api.dtos.UserProfileDto;
import org.rod.kaizen_api.models.UserModel;

import java.util.UUID;

public interface UserService {
    UserProfileDto getProfile(String userId);
    UserProfileDto updateProfile(String userId, String name, String email);
    void deleteAccount(String userId);
    UserModel findById(UUID id);
}
