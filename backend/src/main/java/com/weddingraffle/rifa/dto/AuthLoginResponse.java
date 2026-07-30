package com.weddingraffle.rifa.dto;

public record AuthLoginResponse(String tokenType, String accessToken, long expiresIn) {}
