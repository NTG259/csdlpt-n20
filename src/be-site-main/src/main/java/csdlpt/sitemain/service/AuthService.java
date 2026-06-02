package csdlpt.sitemain.service;

import csdlpt.sitemain.dto.request.LoginRequest;
import csdlpt.sitemain.dto.request.RegisterRequest;
import csdlpt.sitemain.dto.response.AuthResponse;
import csdlpt.sitemain.dto.response.CheckAvailabilityResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    CheckAvailabilityResponse isEmailAvailable(String email);

    CheckAvailabilityResponse isPhoneAvailable(String phone);
}
