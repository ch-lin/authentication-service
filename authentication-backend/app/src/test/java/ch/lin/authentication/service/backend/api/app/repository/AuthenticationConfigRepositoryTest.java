/*
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
 */
package ch.lin.authentication.service.backend.api.app.repository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import ch.lin.authentication.service.backend.api.domain.model.AuthenticationConfig;

@DataJpaTest
class AuthenticationConfigRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AuthenticationConfigRepository repository;

    @Test
    void findAllByEnabledTrue_ShouldReturnOnlyEnabledConfigs() {
        AuthenticationConfig config1 = AuthenticationConfig.builder()
                .name("config1")
                .enabled(true)
                .jwtExpiration(3600L)
                .jwtRefreshExpiration(7200L)
                .jwtIssuerUri("http://test")
                .maxFailedAttempts(5)
                .lockoutDurationMinutes(15)
                .build();
        entityManager.persist(config1);

        AuthenticationConfig config2 = AuthenticationConfig.builder()
                .name("config2")
                .enabled(false)
                .jwtExpiration(3600L)
                .jwtRefreshExpiration(7200L)
                .jwtIssuerUri("http://test")
                .maxFailedAttempts(5)
                .lockoutDurationMinutes(15)
                .build();
        entityManager.persist(config2);
        entityManager.flush();

        List<AuthenticationConfig> enabledConfigs = repository.findAllByEnabledTrue();

        assertThat(enabledConfigs).hasSize(1);
        assertThat(enabledConfigs.get(0).getName()).isEqualTo("config1");
    }

    @Test
    void findFirstByEnabledTrue_ShouldReturnEnabledConfig() {
        AuthenticationConfig config = AuthenticationConfig.builder()
                .name("config1")
                .enabled(true)
                .jwtExpiration(3600L)
                .jwtRefreshExpiration(7200L)
                .jwtIssuerUri("http://test")
                .maxFailedAttempts(5)
                .lockoutDurationMinutes(15)
                .build();
        entityManager.persist(config);
        entityManager.flush();

        Optional<AuthenticationConfig> found = repository.findFirstByEnabledTrue();

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("config1");
    }

    @Test
    void findByName_ShouldReturnConfig_WhenExists() {
        AuthenticationConfig config = AuthenticationConfig.builder()
                .name("test-config")
                .enabled(true)
                .jwtExpiration(3600L)
                .jwtRefreshExpiration(7200L)
                .jwtIssuerUri("http://test")
                .maxFailedAttempts(5)
                .lockoutDurationMinutes(15)
                .build();
        entityManager.persist(config);
        entityManager.flush();

        Optional<AuthenticationConfig> found = repository.findByName("test-config");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("test-config");
    }

    @Test
    void cleanTable_ShouldRemoveAllConfigs() {
        AuthenticationConfig config = AuthenticationConfig.builder()
                .name("cleanup-config")
                .enabled(true)
                .jwtExpiration(3600L)
                .jwtRefreshExpiration(7200L)
                .jwtIssuerUri("http://test")
                .maxFailedAttempts(5)
                .lockoutDurationMinutes(15)
                .build();
        entityManager.persist(config);
        entityManager.flush();

        repository.cleanTable();

        assertThat(repository.count()).isZero();
    }
}
