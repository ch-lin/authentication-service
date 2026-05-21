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
package ch.lin.authentication.service.backend.api.domain.model;

import java.time.OffsetDateTime;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class UserTest {

    @Test
    @DisplayName("Should return correct authorities based on role")
    void getAuthorities() {
        // Given
        // We assume Role is an enum in the same package and has at least one value.
        if (Role.values().length > 0) {
            Role testRole = Role.values()[0];
            User user = User.builder()
                    .firstname("John")
                    .lastname("Doe")
                    .email("test@example.com")
                    .password("password123")
                    .role(testRole)
                    .build();

            // When
            Collection<? extends GrantedAuthority> authorities = user.getAuthorities();

            // Then
            assertNotNull(authorities);
            assertEquals(1, authorities.size());
            assertTrue(authorities.contains(new SimpleGrantedAuthority(testRole.name())));
        }
    }

    @Test
    @DisplayName("Should return email as username")
    void getUsername() {
        // Given
        String email = "test@example.com";
        User user = User.builder()
                .firstname("John")
                .lastname("Doe")
                .password("password123")
                .email(email)
                .build();

        // When
        String username = user.getUsername();

        // Then
        assertEquals(email, username);
    }

    @Test
    @DisplayName("Should return true for all boolean status flags")
    void accountStatusFlags() {
        User user = User.builder()
                .firstname("John")
                .lastname("Doe")
                .email("test@example.com")
                .password("password123")
                .build();

        assertTrue(user.isAccountNonExpired());
        assertTrue(user.isAccountNonLocked());
        assertTrue(user.isCredentialsNonExpired());
        assertTrue(user.isEnabled());
    }

    @Test
    @DisplayName("Should verify Lombok generated methods (Constructors, Getters, Equals, HashCode)")
    void lombokMethods() {
        // Given
        Long id = 1L;
        String firstname = "John";
        String lastname = "Doe";
        String email = "john.doe@example.com";
        String password = "password123";
        Role role = (Role.values().length > 0) ? Role.values()[0] : null;
        OffsetDateTime now = OffsetDateTime.now();

        // When
        User fullUser = User.builder()
                .id(id)
                .firstname(firstname)
                .lastname(lastname)
                .email(email)
                .password(password)
                .role(role)
                .createdAt(now)
                .updatedAt(now)
                .build();

        // Then
        assertEquals(id, fullUser.getId());
        assertEquals(firstname, fullUser.getFirstname());
        assertEquals(lastname, fullUser.getLastname());
        assertEquals(email, fullUser.getEmail());
        assertEquals(password, fullUser.getPassword());
        assertEquals(role, fullUser.getRole());
        assertEquals(now, fullUser.getCreatedAt());
        assertEquals(now, fullUser.getUpdatedAt());

        // Test Equals and HashCode (Should only compare by email)
        User anotherUser = User.builder()
                .id(2L) // Different ID
                .firstname("Different") // Different name
                .lastname("User")
                .email(email)
                .password("different_password")
                .role(role)
                .createdAt(now)
                .updatedAt(now)
                .build();
        assertEquals(fullUser, anotherUser);
        assertEquals(fullUser.hashCode(), anotherUser.hashCode());
    }

    @Test
    @DisplayName("Should successfully update role")
    void updateRole_Success() {
        // Given
        User user = User.builder().role(Role.USER).build();

        // When
        user.updateRole(Role.ADMIN);

        // Then
        assertEquals(Role.ADMIN, user.getRole());
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when updating role to null")
    void updateRole_NullThrowsException() {
        // Given
        User user = User.builder().role(Role.USER).build();

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> user.updateRole(null));
        assertEquals("User role cannot be null.", exception.getMessage());
    }
}
