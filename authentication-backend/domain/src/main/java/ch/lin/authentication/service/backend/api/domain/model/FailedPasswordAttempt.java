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
package ch.lin.authentication.service.backend.api.domain.model;

import java.time.Instant;

import org.hibernate.annotations.ColumnDefault;

import ch.lin.platform.domain.model.AuditableEntity;
import ch.lin.platform.domain.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Represents a record of a failed password verification attempt.
 * <p>
 * This entity is used to track and limit brute-force attacks by keeping a count
 * of consecutive failed attempts from a specific IP address for a given user.
 */
@Entity
@Table(name = FailedPasswordAttempt.TABLE_NAME, indexes = {
    @Index(name = FailedPasswordAttempt.ID_INDEX, columnList = BaseEntity.ID_COLUMN),
    @Index(name = FailedPasswordAttempt.USERNAME_INDEX, columnList = FailedPasswordAttempt.USERNAME_COLUMN)
})
@Getter
@Setter
@EqualsAndHashCode(of = {"username"}, callSuper = false)
@SuperBuilder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class FailedPasswordAttempt extends AuditableEntity {

    /**
     * The name of the database table for failed password attempts.
     */
    public static final String TABLE_NAME = "failed_password_attempt";

    /**
     * The name of the index for the ID column.
     */
    public static final String ID_INDEX = "failed_pwd_attempt_id_idx";

    /**
     * The column name for the username.
     */
    public static final String USERNAME_COLUMN = "username";

    /**
     * The name of the index for the username column.
     */
    public static final String USERNAME_INDEX = "failed_pwd_attempt_username_idx";

    /**
     * The column name for the attempt count.
     */
    public static final String ATTEMPT_COUNT_COLUMN = "attempt_count";

    /**
     * The column name for the last attempt time.
     */
    public static final String LAST_ATTEMPT_TIME_COLUMN = "last_attempt_time";

    /**
     * The username (or email) of the account that failed verification.
     */
    @NotNull
    @Column(name = FailedPasswordAttempt.USERNAME_COLUMN, nullable = false)
    private String username;

    /**
     * The number of consecutive failed attempts for this username and IP
     * combination.
     */
    @NotNull
    @ColumnDefault("1")
    @Column(name = FailedPasswordAttempt.ATTEMPT_COUNT_COLUMN, nullable = false)
    @lombok.Builder.Default
    private Integer attemptCount = 1;

    /**
     * The exact time of the most recent failed attempt.
     */
    @NotNull
    @Column(name = FailedPasswordAttempt.LAST_ATTEMPT_TIME_COLUMN, nullable = false)
    @lombok.Builder.Default
    private Instant lastAttemptTime = Instant.now();
}
