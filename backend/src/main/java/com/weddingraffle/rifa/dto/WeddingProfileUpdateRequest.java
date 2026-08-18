package com.weddingraffle.rifa.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record WeddingProfileUpdateRequest(
        @NotBlank @Size(max = 120) String groomName,
        @NotBlank @Size(max = 120) String brideName,
        @Valid @NotNull WeddingPaletteUpdateRequest palette) {}
