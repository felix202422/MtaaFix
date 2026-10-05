package com.sms.service;

import com.sms.dto.request.ChangePasswordRequest;
import com.sms.dto.request.ForgotPasswordRequest;
import com.sms.dto.request.LoginRequest;
import com.sms.dto.request.ResetPasswordRequest;
import com.sms.dto.response.LoginResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {
    LoginResponse login(LoginRequest request);
    void logout(String token);
    void changePassword(Long userId, ChangePasswordRequest request);
    void forgotPassword(ForgotPasswordRequest request);
    void resetPassword(ResetPasswordRequest request);
    LoginResponse refreshToken(String refreshToken);
}
