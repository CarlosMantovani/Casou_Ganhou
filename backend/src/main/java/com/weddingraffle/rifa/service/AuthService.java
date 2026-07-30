package com.weddingraffle.rifa.service;

import com.weddingraffle.rifa.dto.AuthLoginRequest;
import com.weddingraffle.rifa.dto.AuthLoginResponse;

public interface AuthService {

    AuthLoginResponse login(AuthLoginRequest request);
}
