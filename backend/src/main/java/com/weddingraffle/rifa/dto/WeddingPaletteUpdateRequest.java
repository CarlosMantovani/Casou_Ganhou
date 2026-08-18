package com.weddingraffle.rifa.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record WeddingPaletteUpdateRequest(
        @NotBlank @Pattern(regexp = "^#[0-9A-Fa-f]{6}$") String ivory,
        @NotBlank @Pattern(regexp = "^#[0-9A-Fa-f]{6}$") String ivoryDeep,
        @NotBlank @Pattern(regexp = "^#[0-9A-Fa-f]{6}$") String ink,
        @NotBlank @Pattern(regexp = "^#[0-9A-Fa-f]{6}$") String inkSoft,
        @NotBlank @Pattern(regexp = "^#[0-9A-Fa-f]{6}$") String green,
        @NotBlank @Pattern(regexp = "^#[0-9A-Fa-f]{6}$") String greenDeep,
        @NotBlank @Pattern(regexp = "^#[0-9A-Fa-f]{6}$") String wine,
        @NotBlank @Pattern(regexp = "^#[0-9A-Fa-f]{6}$") String gold,
        @NotBlank @Pattern(regexp = "^#[0-9A-Fa-f]{6}$") String goldSoft,
        @NotBlank @Pattern(regexp = "^#[0-9A-Fa-f]{6}$") String line) {}
