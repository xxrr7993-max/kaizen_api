package org.rod.kaizen_api.services;

import org.rod.kaizen_api.dtos.auth.*;

public interface AuthService {
    AuthResponseDto register(RegisterRecordDto dto);
    AuthResponseDto login(LoginRecordDto dto);
    AuthResponseDto refresh(RefreshRecordDto dto);
    void forgotPassword(ForgotPasswordRecordDto dto);
    void resetPassword(ResetPasswordRecordDto dto);
    void changePassword(ChangePasswordRecordDto dto, String userId);
}
