package com.TrashCashCampus.DTO;

import lombok.Data;

/**
 * Data Transfer Object for updating existing users
 */
@Data
public class UpdateUserDTO {
    private String email;
    private String password;
    private String displayName;
    private Boolean disabled;
} 