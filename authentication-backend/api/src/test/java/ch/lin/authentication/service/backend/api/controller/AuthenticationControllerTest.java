/*=============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2025 Che-Hung Lin
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *===========================================================================*/
package ch.lin.authentication.service.backend.api.controller;

import java.security.Principal;
import java.util.Date;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

import ch.lin.authentication.service.backend.api.app.service.AuthorizationService;
import ch.lin.authentication.service.backend.api.app.service.JwtToken;
import ch.lin.authentication.service.backend.api.domain.model.Client;
import ch.lin.authentication.service.backend.api.domain.model.Role;
import ch.lin.authentication.service.backend.api.dto.AuthenticationRequest;
import ch.lin.authentication.service.backend.api.dto.ClientAuthenticationRequest;
import ch.lin.authentication.service.backend.api.dto.ClientRegisterRequest;
import ch.lin.authentication.service.backend.api.dto.RegisterRequest;
import ch.lin.authentication.service.backend.api.dto.UpdatePasswordRequest;

@ExtendWith(MockitoExtension.class)
class AuthenticationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthorizationService authorizationService;

    @InjectMocks
    private AuthenticationController authenticationController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authenticationController).build();
    }

    @Test
    void register_ShouldReturnTokens_WhenValidRequest() throws Exception {
        RegisterRequest request = new RegisterRequest("John", "Doe", "john.doe@example.com", "password123", Role.USER);
        JwtToken jwtToken = new JwtToken("accessToken", "refreshToken", new Date(), 3600L);

        when(authorizationService.register(request.firstname(), request.lastname(), request.email(), request.password(), request.role()))
                .thenReturn(jwtToken);

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("accessToken"))
                .andExpect(jsonPath("$.refresh_token").value("refreshToken"))
                .andExpect(jsonPath("$.token_type").value("Bearer"))
                .andExpect(jsonPath("$.expires_in").value(3600));
    }

    @Test
    void authenticate_ShouldReturnTokens_WhenValidCredentials() throws Exception {
        AuthenticationRequest request = new AuthenticationRequest("john.doe@example.com", "password123");
        JwtToken jwtToken = new JwtToken("accessToken", "refreshToken", new Date(), 3600L);

        when(authorizationService.authenticate(request.email(), request.password()))
                .thenReturn(jwtToken);

        mockMvc.perform(post("/api/v1/auth/authenticate")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("accessToken"))
                .andExpect(jsonPath("$.refresh_token").value("refreshToken"));
    }

    @Test
    void authenticate_ShouldExtractIpFromXForwardedForHeader_WhenHeaderIsPresent() throws Exception {
        AuthenticationRequest request = new AuthenticationRequest("john.doe@example.com", "password123");
        JwtToken jwtToken = new JwtToken("accessToken", "refreshToken", new Date(), 3600L);

        when(authorizationService.authenticate(eq(request.email()), eq(request.password())))
                .thenReturn(jwtToken);

        // Simulate a scenario where the request passes through a Proxy or Load Balancer with a list of original IPs
        mockMvc.perform(post("/api/v1/auth/authenticate")
                .header("X-Forwarded-For", "192.168.1.100, 10.0.0.1")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                .andExpect(status().isOk());
    }

    @Test
    void authenticate_ShouldFallbackToRemoteAddr_WhenXForwardedForHeaderIsEmpty() throws Exception {
        AuthenticationRequest request = new AuthenticationRequest("john.doe@example.com", "password123");
        JwtToken jwtToken = new JwtToken("accessToken", "refreshToken", new Date(), 3600L);

        when(authorizationService.authenticate(eq(request.email()), eq(request.password())))
                .thenReturn(jwtToken);

        mockMvc.perform(post("/api/v1/auth/authenticate")
                .header("X-Forwarded-For", "")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                .andExpect(status().isOk());
    }

    @Test
    void authenticate_ShouldReturnLocked_WhenAccountIsLocked() throws Exception {
        AuthenticationRequest request = new AuthenticationRequest("john.doe@example.com", "password123");

        when(authorizationService.authenticate(eq(request.email()), eq(request.password())))
                .thenThrow(new LockedException("Account is locked"));

        mockMvc.perform(post("/api/v1/auth/authenticate")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.message").value("Account is locked. Please try again in 15 minutes."));
    }

    @Test
    void registerClient_ShouldReturnCredentials_WhenValidRequest() throws Exception {
        ClientRegisterRequest request = new ClientRegisterRequest("downloader-app", Role.SERVICE);
        Client client = Client.builder()
                .clientId("generated-client-id")
                .clientSecret("generated-client-secret")
                .build();

        when(authorizationService.registerClient(request.clientName(), request.role()))
                .thenReturn(client);

        mockMvc.perform(post("/api/v1/auth/client-register")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientId").value("generated-client-id"))
                .andExpect(jsonPath("$.clientSecret").value("generated-client-secret"));
    }

    @Test
    void authenticateClient_ShouldReturnTokens_WhenValidCredentials() throws Exception {
        ClientAuthenticationRequest request = new ClientAuthenticationRequest("generated-client-id", "generated-client-secret");
        JwtToken jwtToken = new JwtToken("accessToken", "refreshToken", new Date(), 3600L);

        when(authorizationService.authenticateClient(request.clientId(), request.clientSecret()))
                .thenReturn(jwtToken);

        mockMvc.perform(post("/api/v1/auth/client-authenticate")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("accessToken"));
    }

    @Test
    void refreshToken_ShouldReturnNewTokens_WhenValidToken() throws Exception {
        String refreshToken = "validRefresh";
        JwtToken jwtToken = new JwtToken("newAccess", "newRefresh", new Date(), 3600L);

        when(authorizationService.refreshUserToken(refreshToken)).thenReturn(jwtToken);

        mockMvc.perform(post("/api/v1/auth/refresh")
                .header("Authorization", "Bearer " + refreshToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("newAccess"));
    }

    @Test
    void refreshToken_ShouldReturnBadRequest_WhenHeaderMissing() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refreshToken_ShouldReturnBadRequest_WhenHeaderInvalidFormat() throws Exception {
        mockMvc.perform(post("/api/v1/auth/refresh")
                .header("Authorization", "Basic invalid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refreshToken_ShouldReturnUnauthorized_WhenTokenInvalid() throws Exception {
        String refreshToken = "invalid";
        when(authorizationService.refreshUserToken(refreshToken)).thenThrow(new JwtException("Invalid token"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                .header("Authorization", "Bearer " + refreshToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshClientToken_ShouldReturnNewTokens_WhenValidToken() throws Exception {
        String refreshToken = "validRefresh";
        JwtToken jwtToken = new JwtToken("newAccess", "newRefresh", new Date(), 3600L);

        when(authorizationService.refreshClientToken(refreshToken)).thenReturn(jwtToken);

        mockMvc.perform(post("/api/v1/auth/client-refresh")
                .header("Authorization", "Bearer " + refreshToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token").value("newAccess"));
    }

    @Test
    void refreshClientToken_ShouldReturnBadRequest_WhenHeaderMissing() throws Exception {
        mockMvc.perform(post("/api/v1/auth/client-refresh"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refreshClientToken_ShouldReturnBadRequest_WhenHeaderInvalidFormat() throws Exception {
        mockMvc.perform(post("/api/v1/auth/client-refresh")
                .header("Authorization", "Basic invalid"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refreshClientToken_ShouldReturnUnauthorized_WhenTokenInvalid() throws Exception {
        String refreshToken = "invalid";
        when(authorizationService.refreshClientToken(refreshToken)).thenThrow(new IllegalArgumentException("Invalid token"));

        mockMvc.perform(post("/api/v1/auth/client-refresh")
                .header("Authorization", "Bearer " + refreshToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cleanup_ShouldReturnOk() throws Exception {
        mockMvc.perform(delete("/api/v1/auth/cleanup"))
                .andExpect(status().isOk())
                .andExpect(content().string("All users and clients have been deleted and sequences have been reset."));

        verify(authorizationService).cleanup();
    }

    @Test
    void updatePassword_ShouldReturnOk_WhenValidRequest() throws Exception {
        UpdatePasswordRequest request = new UpdatePasswordRequest("old", "new");
        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn("test@example.com");

        mockMvc.perform(put("/api/v1/auth/users/password")
                .principal(principal)
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                .andExpect(status().isOk());

        verify(authorizationService).updatePassword("test@example.com", "old", "new");
    }

    @Test
    void updatePassword_ShouldReturnUnauthorized_WhenPrincipalNull() throws Exception {
        UpdatePasswordRequest request = new UpdatePasswordRequest("old", "new");

        mockMvc.perform(put("/api/v1/auth/users/password")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updatePassword_ShouldReturnForbidden_WhenBadCredentials() throws Exception {
        UpdatePasswordRequest request = new UpdatePasswordRequest("old", "new");
        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn("test@example.com");

        doThrow(new BadCredentialsException("Invalid old password"))
                .when(authorizationService).updatePassword("test@example.com", "old", "new");

        mockMvc.perform(put("/api/v1/auth/users/password")
                .principal(principal)
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                .andExpect(status().isForbidden());
    }

    @Test
    void updatePassword_ShouldReturnLocked_WhenAccountIsLocked() throws Exception {
        UpdatePasswordRequest request = new UpdatePasswordRequest("old", "new");
        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn("test@example.com");

        doThrow(new LockedException("Account is locked"))
                .when(authorizationService).updatePassword(eq("test@example.com"), eq("old"), eq("new"));

        mockMvc.perform(put("/api/v1/auth/users/password")
                .principal(principal)
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                .andExpect(status().isLocked())
                .andExpect(jsonPath("$.message").value("Account is locked. Please try again in 15 minutes."));
    }

    @Test
    void updatePassword_ShouldReturnBadRequest_WhenUserNotFound() throws Exception {
        UpdatePasswordRequest request = new UpdatePasswordRequest("old", "new");
        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn("test@example.com");

        doThrow(new IllegalArgumentException("User not found"))
                .when(authorizationService).updatePassword("test@example.com", "old", "new");

        mockMvc.perform(put("/api/v1/auth/users/password")
                .principal(principal)
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .content(Objects.requireNonNull(objectMapper.writeValueAsString(request))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rotateClientSecret_ShouldReturnNewSecret_WhenValid() throws Exception {
        String clientId = "client-123";
        Client client = Client.builder().clientId(clientId).clientSecret("newRawSecret").build();

        when(authorizationService.rotateClientSecret(clientId)).thenReturn(client);

        mockMvc.perform(post("/api/v1/auth/clients/{clientId}/rotate-secret", clientId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clientId").value(clientId))
                .andExpect(jsonPath("$.clientSecret").value("newRawSecret"));
    }

    @Test
    void rotateClientSecret_ShouldReturnBadRequest_WhenClientNotFound() throws Exception {
        String clientId = "invalid";

        when(authorizationService.rotateClientSecret(clientId)).thenThrow(new IllegalArgumentException("Invalid client ID"));

        mockMvc.perform(post("/api/v1/auth/clients/{clientId}/rotate-secret", clientId))
                .andExpect(status().isBadRequest());
    }
}
