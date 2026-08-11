package com.apprh.backend.auth.api;

import com.apprh.backend.users.api.UserResponse;

public record AuthResponse(String accessToken, String refreshToken, UserResponse user) {
}
