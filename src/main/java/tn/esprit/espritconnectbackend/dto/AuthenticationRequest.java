package tn.esprit.espritconnectbackend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AuthenticationRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String password;
}
