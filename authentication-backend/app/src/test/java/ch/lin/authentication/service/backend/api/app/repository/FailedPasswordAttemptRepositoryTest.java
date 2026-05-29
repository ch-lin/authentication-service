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
package ch.lin.authentication.service.backend.api.app.repository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import ch.lin.authentication.service.backend.api.domain.model.FailedPasswordAttempt;

@DataJpaTest
class FailedPasswordAttemptRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private FailedPasswordAttemptRepository repository;

    @Test
    void findByUsername_ShouldReturnRecord_WhenExists() {
        // Arrange
        FailedPasswordAttempt attempt = FailedPasswordAttempt.builder()
                .username("test@example.com")
                .attemptCount(3)
                .build();
        entityManager.persist(attempt);
        entityManager.flush();

        // Act
        Optional<FailedPasswordAttempt> found = repository.findByUsername("test@example.com");

        // Assert
        assertThat(found).isPresent();
        assertThat(found.get().getUsername()).isEqualTo("test@example.com");
        assertThat(found.get().getAttemptCount()).isEqualTo(3);
    }

    @Test
    void findByUsername_ShouldReturnEmpty_WhenNotExists() {
        Optional<FailedPasswordAttempt> found = repository.findByUsername("nonexistent@example.com");
        assertThat(found).isEmpty();
    }

    @Test
    void deleteByUsername_ShouldRemoveOnlyTargetUserRecords() {
        // Arrange
        FailedPasswordAttempt targetAttempt = FailedPasswordAttempt.builder()
                .username("target@example.com")
                .build();
        FailedPasswordAttempt otherAttempt = FailedPasswordAttempt.builder()
                .username("other@example.com")
                .build();

        entityManager.persist(targetAttempt);
        entityManager.persist(otherAttempt);
        entityManager.flush();

        // Act
        repository.deleteByUsername("target@example.com");

        // Assert
        assertThat(repository.findAll()).hasSize(1);
        assertThat(repository.findAll().get(0).getUsername()).isEqualTo("other@example.com");
    }
}
