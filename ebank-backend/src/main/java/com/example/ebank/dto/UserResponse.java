package com.example.ebank.dto;

import java.util.List;

public record UserResponse(Long id, String username, List<String> roles, Long organizationId) {
}
