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
package ch.lin.authentication.service.backend.api.app.service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;

import ch.lin.authentication.service.backend.api.app.repository.ClientRepository;
import ch.lin.authentication.service.backend.api.app.repository.FailedPasswordAttemptRepository;
import ch.lin.authentication.service.backend.api.app.repository.UserRepository;
import ch.lin.authentication.service.backend.api.domain.model.AuthenticationConfig;
import ch.lin.authentication.service.backend.api.domain.model.Client;
import ch.lin.authentication.service.backend.api.domain.model.FailedPasswordAttempt;
import ch.lin.authentication.service.backend.api.domain.model.Role;
import ch.lin.authentication.service.backend.api.domain.model.User;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceImplTest {

    @Mock
    private JwtEncoder jwtEncoder;
    @Mock
    private JwtDecoder jwtDecoder;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private UserDetailsService userDetailsService;
    @Mock
    private ConfigsService configsService;
    @Mock
    private FailedPasswordAttemptRepository failedPasswordAttemptRepository;

    @InjectMocks
    private AuthorizationServiceImpl authorizationService;

    private AuthenticationConfig authConfig;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        authConfig = AuthenticationConfig.builder()
                .jwtExpiration(3600000L) // 1 hour
                .jwtRefreshExpiration(7200000L) // 2 hours
                .jwtIssuerUri("http://test-issuer")
                .maxFailedAttempts(5)
                .lockoutDurationMinutes(15)
                .build();
    }

    @Test
    void cleanup_ShouldClearRepositories() {
        authorizationService.cleanup();
        verify(userRepository).cleanTable();
        verify(userRepository).resetSequence();
        verify(clientRepository).cleanTable();
        verify(clientRepository).resetSequence();
    }

    @Test
    @SuppressWarnings("null")
    void register_ShouldSaveUserAndReturnToken() {
        // Arrange
        String firstname = "John";
        String lastname = "Doe";
        String email = "john.doe@example.com";
        String password = "password";
        Role role = Role.USER;

        when(passwordEncoder.encode(password)).thenReturn("encodedPassword");
        when(configsService.getResolvedConfig(null)).thenReturn(authConfig);

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            return u.toBuilder().id(1L).build();
        });

        Jwt jwtMock = mock(Jwt.class);
        when(jwtMock.getTokenValue()).thenReturn("mockToken");
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(jwtMock);

        // Act
        JwtToken token = authorizationService.register(firstname, lastname, email, password, role);

        // Assert
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getFirstname()).isEqualTo(firstname);
        assertThat(savedUser.getLastname()).isEqualTo(lastname);
        assertThat(savedUser.getEmail()).isEqualTo(email);
        assertThat(savedUser.getPassword()).isEqualTo("encodedPassword");
        assertThat(savedUser.getRole()).isEqualTo(role);

        // Verify the parameters passed to the JWT encoder for both tokens
        ArgumentCaptor<JwtEncoderParameters> jwtParamsCaptor = ArgumentCaptor.forClass(JwtEncoderParameters.class);
        verify(jwtEncoder, times(2)).encode(jwtParamsCaptor.capture());
        List<JwtEncoderParameters> allJwtParams = jwtParamsCaptor.getAllValues();

        // Assert on Access Token parameters (assuming it's the first call)
        JwtEncoderParameters accessTokenParams = allJwtParams.get(0);
        assertThat(accessTokenParams.getClaims().getSubject()).isEqualTo("1");
        assertThat(accessTokenParams.getClaims().getIssuer().toString()).isEqualTo(authConfig.getJwtIssuerUri());

        // Assert on Refresh Token parameters (assuming it's the second call)
        JwtEncoderParameters refreshTokenParams = allJwtParams.get(1);
        assertThat(refreshTokenParams.getClaims().getExpiresAt()).isAfter(accessTokenParams.getClaims().getExpiresAt());

        assertThat(token.token()).isEqualTo("mockToken");
        assertThat(token.refreshToken()).isEqualTo("mockToken");
    }

    @Test
    @SuppressWarnings("null")
    void registerClient_ShouldSaveClientAndReturnRawSecret() {
        // Arrange
        String clientName = "TestClient";
        Role role = Role.ADMIN;
        when(passwordEncoder.encode(anyString())).thenReturn("encodedSecret");

        // Act
        Client result = authorizationService.registerClient(clientName, role);

        // Assert
        ArgumentCaptor<Client> clientCaptor = ArgumentCaptor.forClass(Client.class);
        verify(clientRepository).save(clientCaptor.capture());
        Client savedClient = clientCaptor.getValue();

        assertThat(savedClient.getClientName()).isEqualTo(clientName);
        assertThat(savedClient.getClientSecret()).isEqualTo("encodedSecret"); // Verify DB gets encoded secret
        assertThat(savedClient.getRole()).isEqualTo(role);

        assertThat(result.getClientName()).isEqualTo(clientName);
        assertThat(result.getClientSecret()).isNotEqualTo("encodedSecret");
        assertThat(result.getRole()).isEqualTo(role);
    }

    @Test
    void authenticate_ShouldAuthenticateAndReturnToken() {
        // Arrange
        String email = "test@example.com";
        String password = "password";

        User authUser = mock(User.class);
        when(authUser.getId()).thenReturn(1L);

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(authUser);
        doReturn(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))).when(auth).getAuthorities();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(configsService.getResolvedConfig(null)).thenReturn(authConfig);

        Jwt jwtMock = mock(Jwt.class);
        when(jwtMock.getTokenValue()).thenReturn("mockToken");
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(jwtMock);

        User user = mock(User.class);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        // Act
        JwtToken token = authorizationService.authenticate(email, password);

        // Assert
        ArgumentCaptor<JwtEncoderParameters> jwtParamsCaptor = ArgumentCaptor.forClass(JwtEncoderParameters.class);
        // Expecting 2 calls (Access Token + Refresh Token)
        verify(jwtEncoder, times(2)).encode(jwtParamsCaptor.capture());
        List<JwtEncoderParameters> allJwtParams = jwtParamsCaptor.getAllValues();

        JwtEncoderParameters accessTokenParams = allJwtParams.get(0);
        assertThat(accessTokenParams.getClaims().getSubject()).isEqualTo("1");
        assertThat(accessTokenParams.getClaims().getIssuer().toString()).isEqualTo(authConfig.getJwtIssuerUri());

        assertThat(token.token()).isEqualTo("mockToken");
        verify(failedPasswordAttemptRepository).deleteByUsername(email);
        verify(user).unlockAccount();
        verify(userRepository).save(Objects.requireNonNull(user));
    }

    @Test
    void authenticate_ShouldThrow_WhenPrincipalNotUser() {
        String email = "test@example.com";
        String password = "password";

        org.springframework.security.core.userdetails.UserDetails genericUserDetails = mock(org.springframework.security.core.userdetails.UserDetails.class);
        
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(genericUserDetails);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);

        assertThatThrownBy(() -> authorizationService.authenticate(email, password))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported principal type");
    }

    @Test
    void authenticate_ShouldThrow_WhenPrincipalIsNull() {
        String email = "test@example.com";
        String password = "password";

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(null);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);

        assertThatThrownBy(() -> authorizationService.authenticate(email, password))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported principal type: null");
    }

    @Test
    void authenticateClient_ShouldReturnToken_WhenCredentialsValid() {
        // Arrange
        String clientId = "clientId";
        String clientSecret = "secret";
        String encodedSecret = "encodedSecret";
        Client client = Client.builder()
                .clientId(clientId)
                .clientSecret(encodedSecret)
                .role(Role.USER)
                .build();

        when(clientRepository.findByClientId(clientId)).thenReturn(Optional.of(client));
        when(passwordEncoder.matches(clientSecret, encodedSecret)).thenReturn(true);
        when(configsService.getResolvedConfig(null)).thenReturn(authConfig);

        Jwt jwtMock = mock(Jwt.class);
        when(jwtMock.getTokenValue()).thenReturn("mockToken");
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(jwtMock);

        // Act
        JwtToken token = authorizationService.authenticateClient(clientId, clientSecret);

        // Assert
        assertThat(token.token()).isEqualTo("mockToken");
    }

    @Test
    void authenticateClient_ShouldThrow_WhenClientNotFound() {
        when(clientRepository.findByClientId("id")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> authorizationService.authenticateClient("id", "secret"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid client ID or secret");
    }

    @Test
    void authenticateClient_ShouldThrow_WhenSecretInvalid() {
        Client client = Client.builder().clientId("id").clientSecret("encoded").build();
        when(clientRepository.findByClientId("id")).thenReturn(Optional.of(client));
        when(passwordEncoder.matches("secret", "encoded")).thenReturn(false);

        assertThatThrownBy(() -> authorizationService.authenticateClient("id", "secret"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid client ID or secret");
    }

    @Test
    void refreshUserToken_ShouldReturnNewToken() {
        // Arrange
        String refreshToken = "validRefresh";
        String userIdStr = "1";
        Jwt decodedJwt = mock(Jwt.class);
        when(decodedJwt.getSubject()).thenReturn(userIdStr);
        when(jwtDecoder.decode(refreshToken)).thenReturn(decodedJwt);

        User user = User.builder().id(1L).email("test@example.com").role(Role.USER).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        when(configsService.getResolvedConfig(null)).thenReturn(authConfig);
        Jwt jwtMock = mock(Jwt.class);
        when(jwtMock.getTokenValue()).thenReturn("newToken");
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(jwtMock);

        // Act
        JwtToken token = authorizationService.refreshUserToken(refreshToken);

        // Assert
        assertThat(token.token()).isEqualTo("newToken");
    }

    @Test
    void refreshUserToken_ShouldReturnNewToken_WithEmailFallback() {
        // Arrange
        String refreshToken = "validRefresh";
        String username = "user@example.com";
        Jwt decodedJwt = mock(Jwt.class);
        when(decodedJwt.getSubject()).thenReturn(username);
        when(jwtDecoder.decode(refreshToken)).thenReturn(decodedJwt);

        User user = User.builder().id(1L).email(username).role(Role.USER).build();
        when(userDetailsService.loadUserByUsername(username)).thenReturn(user);

        when(configsService.getResolvedConfig(null)).thenReturn(authConfig);
        Jwt jwtMock = mock(Jwt.class);
        when(jwtMock.getTokenValue()).thenReturn("newToken");
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(jwtMock);

        // Act
        JwtToken token = authorizationService.refreshUserToken(refreshToken);

        // Assert
        assertThat(token.token()).isEqualTo("newToken");
    }

    @Test
    void refreshUserToken_ShouldThrow_WhenTokenInvalid() {
        String refreshToken = "invalid";
        when(jwtDecoder.decode(refreshToken)).thenThrow(new JwtException("Invalid token"));

        assertThatThrownBy(() -> authorizationService.refreshUserToken(refreshToken))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid refresh token");
    }

    @Test
    void refreshUserToken_ShouldThrow_WhenUserDetailsNotUser() {
        String refreshToken = "validRefresh";
        String username = "user@example.com";
        Jwt decodedJwt = mock(Jwt.class);
        when(decodedJwt.getSubject()).thenReturn(username);
        when(jwtDecoder.decode(refreshToken)).thenReturn(decodedJwt);

        org.springframework.security.core.userdetails.UserDetails genericUserDetails = mock(org.springframework.security.core.userdetails.UserDetails.class);
        when(userDetailsService.loadUserByUsername(username)).thenReturn(genericUserDetails);

        assertThatThrownBy(() -> authorizationService.refreshUserToken(refreshToken))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported UserDetails type");
    }

    @Test
    void refreshUserToken_ShouldThrow_WhenUserDetailsIsNull() {
        String refreshToken = "validRefresh";
        String username = "user@example.com";
        Jwt decodedJwt = mock(Jwt.class);
        when(decodedJwt.getSubject()).thenReturn(username);
        when(jwtDecoder.decode(refreshToken)).thenReturn(decodedJwt);

        when(userDetailsService.loadUserByUsername(username)).thenReturn(null);

        assertThatThrownBy(() -> authorizationService.refreshUserToken(refreshToken))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported UserDetails type: null");
    }

    @Test
    void refreshUserToken_ShouldThrow_WhenUserNotFoundById() {
        String refreshToken = "validRefresh";
        String userIdStr = "999";
        Jwt decodedJwt = mock(Jwt.class);
        when(decodedJwt.getSubject()).thenReturn(userIdStr);
        when(jwtDecoder.decode(refreshToken)).thenReturn(decodedJwt);

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authorizationService.refreshUserToken(refreshToken))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User not found");
    }

    @Test
    void refreshClientToken_ShouldReturnNewToken() {
        // Arrange
        String refreshToken = "validRefresh";
        String clientId = "client";
        Jwt decodedJwt = mock(Jwt.class);
        when(decodedJwt.getSubject()).thenReturn(clientId);
        when(jwtDecoder.decode(refreshToken)).thenReturn(decodedJwt);

        Client client = Client.builder().clientId(clientId).role(Role.USER).build();
        when(clientRepository.findByClientId(clientId)).thenReturn(Optional.of(client));

        when(configsService.getResolvedConfig(null)).thenReturn(authConfig);
        Jwt jwtMock = mock(Jwt.class);
        when(jwtMock.getTokenValue()).thenReturn("newToken");
        when(jwtEncoder.encode(any(JwtEncoderParameters.class))).thenReturn(jwtMock);

        // Act
        JwtToken token = authorizationService.refreshClientToken(refreshToken);

        // Assert
        assertThat(token.token()).isEqualTo("newToken");
    }

    @Test
    void refreshClientToken_ShouldThrow_WhenClientNotFound() {
        String refreshToken = "validRefresh";
        String clientId = "client";
        Jwt decodedJwt = mock(Jwt.class);
        when(decodedJwt.getSubject()).thenReturn(clientId);
        when(jwtDecoder.decode(refreshToken)).thenReturn(decodedJwt);

        when(clientRepository.findByClientId(clientId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authorizationService.refreshClientToken(refreshToken))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid client ID in refresh token");
    }

    @Test
    void refreshClientToken_ShouldThrow_WhenTokenInvalid() {
        String refreshToken = "invalid";
        when(jwtDecoder.decode(refreshToken)).thenThrow(new JwtException("Invalid token"));

        assertThatThrownBy(() -> authorizationService.refreshClientToken(refreshToken))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid refresh token");
    }

    @Test
    void updatePassword_ShouldUpdateAndSave_WhenValid() {
        String email = "test@example.com";
        User user = User.builder()
                .email(email)
                .password("encodedOldPassword")
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old", "encodedOldPassword")).thenReturn(true);
        when(passwordEncoder.encode("new")).thenReturn("encodedNewPassword");

        authorizationService.updatePassword(email, "old", "new");

        assertThat(user.getPassword()).isEqualTo("encodedNewPassword");
        verify(userRepository).save(user);
        verify(failedPasswordAttemptRepository).deleteByUsername(email);
    }

    @Test
    void updatePassword_ShouldThrow_WhenUserNotFound() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authorizationService.updatePassword("test@example.com", "old", "new"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User not found");
    }

    @Test
    void updatePassword_ShouldThrow_WhenOldPasswordInvalid() {
        String email = "test@example.com";
        User user = User.builder().email(email).password("encodedOld").build();
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "encodedOld")).thenReturn(false);

        FailedPasswordAttempt attempt = FailedPasswordAttempt.builder()
                .username(email)
                .attemptCount(1)
                .build();
        when(failedPasswordAttemptRepository.findByUsername(email)).thenReturn(Optional.of(attempt));
        when(configsService.getResolvedConfig(null)).thenReturn(authConfig);

        assertThatThrownBy(() -> authorizationService.updatePassword(email, "wrong", "new"))
                .isInstanceOf(org.springframework.security.authentication.BadCredentialsException.class)
                .hasMessage("Invalid old password");

        verify(failedPasswordAttemptRepository).save(Objects.requireNonNull(attempt));
        assertThat(attempt.getAttemptCount()).isEqualTo(2);
    }

    @Test
    void updatePassword_ShouldThrowLockedException_WhenAccountIsLocked() {
        String email = "test@example.com";
        User user = mock(User.class);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));
        when(user.isAccountNonLocked()).thenReturn(false);

        assertThatThrownBy(() -> authorizationService.updatePassword(email, "old", "new"))
                .isInstanceOf(org.springframework.security.authentication.LockedException.class)
                .hasMessage("Account is locked");
    }

    @Test
    void authenticate_ShouldHandleFailedAttemptAndLockAccount_WhenThresholdReached() {
        String email = "test@example.com";

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new org.springframework.security.authentication.BadCredentialsException("Bad credentials"));

        FailedPasswordAttempt attempt = FailedPasswordAttempt.builder()
                .username(email)
                .attemptCount(4)
                .build();
        when(failedPasswordAttemptRepository.findByUsername(email)).thenReturn(Optional.of(attempt));
        when(configsService.getResolvedConfig(null)).thenReturn(authConfig); // max is 5

        User user = mock(User.class);
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authorizationService.authenticate(email, "wrongPassword"))
                .isInstanceOf(org.springframework.security.authentication.BadCredentialsException.class);

        verify(failedPasswordAttemptRepository).save(Objects.requireNonNull(attempt));
        assertThat(attempt.getAttemptCount()).isEqualTo(5);
        verify(user).lockAccount(any());
        verify(userRepository).save(Objects.requireNonNull(user));
    }

    @Test
    void rotateClientSecret_ShouldUpdateAndSave_WhenValid() {
        String clientId = "client-123";
        Client client = Client.builder()
                .clientId(clientId)
                .clientSecret("oldSecret")
                .role(Role.SERVICE)
                .build();

        when(clientRepository.findByClientId(clientId)).thenReturn(Optional.of(client));
        when(passwordEncoder.encode(anyString())).thenReturn("encodedNewSecret");

        Client result = authorizationService.rotateClientSecret(clientId);

        assertThat(client.getClientSecret()).isEqualTo("encodedNewSecret");
        verify(clientRepository).save(client);

        assertThat(result.getClientId()).isEqualTo(clientId);
        assertThat(result.getClientSecret()).isNotEqualTo("encodedNewSecret"); // Ensure returned secret is plain text
        assertThat(result.getClientSecret()).isNotBlank();
    }

    @Test
    void rotateClientSecret_ShouldThrow_WhenClientNotFound() {
        when(clientRepository.findByClientId("invalid")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authorizationService.rotateClientSecret("invalid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid client ID");
    }
}
