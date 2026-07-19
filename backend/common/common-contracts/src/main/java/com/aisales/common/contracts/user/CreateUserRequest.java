package com.aisales.common.contracts.user;

import com.aisales.common.core.validation.PasswordPolicy;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateUserRequest {

    @NotBlank
    @Email
    private String email;

    @PasswordPolicy
    private String password;

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    private Set<String> roles;
}
