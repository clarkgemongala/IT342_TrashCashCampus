package com.TrashCashCampus.DTO;

import lombok.Data;

/**
 * Data Transfer Object for creating new users
 */
@Data
public class CreateUserDTO {
    private String email;
    private String password;
    private String displayName;
    private Boolean disabled = false;
} 