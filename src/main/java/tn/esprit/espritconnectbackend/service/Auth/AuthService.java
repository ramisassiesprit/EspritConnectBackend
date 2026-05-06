package tn.esprit.espritconnectbackend.service.Auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tn.esprit.espritconnectbackend.dto.AuthenticationRequest;
import tn.esprit.espritconnectbackend.dto.RegisterRequest;
import tn.esprit.espritconnectbackend.dto.ResetPasswordRequest;
import tn.esprit.espritconnectbackend.dto.AuthenticationResponse;

public interface AuthService {
    AuthenticationResponse register(RegisterRequest request);
    AuthenticationResponse login(AuthenticationRequest request, HttpServletRequest servletRequest, HttpServletResponse response);
    AuthenticationResponse refreshToken(HttpServletRequest request, HttpServletResponse response);
    void logout(HttpServletRequest request, HttpServletResponse response);
    void forgotPassword(String email);
    void resetPassword(ResetPasswordRequest request);
}
