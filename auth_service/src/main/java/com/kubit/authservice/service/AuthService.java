package com.kubit.authservice.service;

import com.kubit.authservice.domain.entity.RegisterRequest;

public interface AuthService {
    void register(RegisterRequest request);
}
