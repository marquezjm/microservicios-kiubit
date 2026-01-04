package com.kubit.authservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kubit.authservice.domain.entity.AuthUser;
import com.kubit.authservice.domain.entity.RegisterRequest;
import com.kubit.authservice.domain.entity.LoginRequest;
import com.kubit.authservice.domain.entity.UserLoginResponse;
import com.kubit.authservice.service.AuthService;
import com.kubit.authservice.util.JwtUtil;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(AuthController.class)
class AuthControllerTest {

        @Autowired
        private MockMvc mockMvc;
        /*
         * @Autowired
         * private ObjectMapper objectMapper;
         */
        private ObjectMapper objectMapper = new ObjectMapper();

        @MockitoBean
        private AuthService authService;

        @MockitoBean
        private JwtUtil jwtUtil;

        private RegisterRequest registerRequest;
        private LoginRequest loginRequest;
        private AuthUser authUser;
        private UserLoginResponse userLoginResponse;

        @BeforeEach
        void setup() {
                registerRequest = RegisterRequest.builder()
                                .email("test@kiubit.mx")
                                .password("1234")
                                .name("Test User")
                                .age(20)
                                .build();

                authUser = AuthUser.builder()
                                .id(1L)
                                .email(registerRequest.getEmail())
                                .build();

                loginRequest = LoginRequest.builder()
                                .email("test@kiubit.mx")
                                .password("1234")
                                .build();

                userLoginResponse = new UserLoginResponse(authUser, "accessToken", "refreshToken");
        }

        @Test
        void register_ReturnsCreatedUserWithoutPassword() throws Exception {
                given(authService.register(any(RegisterRequest.class))).willReturn(authUser);

                MvcResult result = mockMvc.perform(post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(registerRequest)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                                .andExpect(jsonPath("$.email").value("test@kiubit.mx"))
                                .andReturn();
        }

        @Test
        void login_ReturnsSetCookieAndUser() throws Exception {
                given(authService.login(any(LoginRequest.class))).willReturn(userLoginResponse);

                MvcResult result = mockMvc.perform(post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginRequest)))
                                .andExpect(status().isOk())
                                .andExpect(cookie().exists("accessToken"))
                                .andExpect(cookie().exists("refreshToken"))
                                .andExpect(cookie().exists("deviceId"))
                                .andExpect(jsonPath("$.user.email").value("test@kiubit.mx"))
                                .andReturn();
        }

        @Test
        void refresh_ReturnsSetCookieAndUser() throws Exception {
                given(authService.refreshToken(anyString(), anyString(), anyString()))
                                .willReturn(userLoginResponse);
                mockMvc.perform(post("/auth/refresh")
                                .cookie(new Cookie("refreshToken", "refreshToken"))
                                .cookie(new Cookie("deviceId", "device123"))
                                .header("X-Forwarded-For", "1.2.3.4"))
                                .andExpect(status().isOk())
                                .andExpect(cookie().exists("accessToken"))
                                .andExpect(cookie().exists("refreshToken"))
                                .andExpect(jsonPath("$.user.email").value("test@kiubit.mx"));
        }

        @Test
        void logout_ClearsCookiesAndReturnsOk() throws Exception {
                mockMvc.perform(post("/auth/logout")
                                .cookie(new Cookie("refreshToken", "refreshToken"))
                                .cookie(new Cookie("deviceId", "device123"))
                                .header("X-Forwarded-For", "127.0.0.1"))
                                .andExpect(status().isOk())
                                .andExpect(cookie().maxAge("accessToken", 0))
                                .andExpect(cookie().maxAge("refreshToken", 0));
        }

        @Test
        void refresh_WithInvalidToken_ReturnsError() throws Exception {
                given(authService.refreshToken(anyString(), anyString(), anyString()))
                                .willThrow(new IllegalArgumentException("Invalid or expired refresh token"));
                mockMvc.perform(post("/auth/refresh")
                                .cookie(new Cookie("refreshToken", "badtoken"))
                                .cookie(new Cookie("deviceId", "device123")))
                                .andExpect(status().is4xxClientError());
        }

        @Test
        void login_FailsIfDeviceIdMissing() throws Exception {
                // Simula lógica donde controller obliga a deviceId
                // Aquí esperarías un BAD_REQUEST si así lo defines (o simplemente revisa
                // outcome)
                given(authService.login(any(LoginRequest.class)))
                                .willThrow(new IllegalArgumentException("deviceId required"));
                LoginRequest req = loginRequest;
                // No se coloca cookie deviceId
                mockMvc.perform(post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(req)))
                                .andExpect(status().is4xxClientError());
        }
}
