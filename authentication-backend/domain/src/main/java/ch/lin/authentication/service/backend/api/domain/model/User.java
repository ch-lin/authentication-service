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

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import org.hibernate.annotations.ColumnDefault;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import static ch.lin.authentication.service.backend.api.domain.model.User.TABLE_NAME;
import ch.lin.platform.domain.model.AuditableEntity;
import ch.lin.platform.domain.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * Represents a user in the system.
 * <p>
 * This class is a JPA entity that maps to the "user" table and also implements
 * Spring Security's {@link UserDetails} interface to integrate with the
 * authentication framework.
 */
@Entity
@Table(name = TABLE_NAME, indexes = {
    @Index(name = User.ID_INDEX, columnList = BaseEntity.ID_COLUMN),
    @Index(name = User.EMAIL_INDEX, columnList = User.EMAIL_COLUMN)}, uniqueConstraints = {
    @UniqueConstraint(columnNames = User.EMAIL_COLUMN)})
@Getter
@EqualsAndHashCode(of = {"email"}, callSuper = false)
@SuperBuilder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class User extends AuditableEntity implements UserDetails {

    /**
     * The serialization version UID for this class.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The name of the database table for users.
     */
    public static final String TABLE_NAME = "user";

    /**
     * The name of the index for the ID column.
     */
    public static final String ID_INDEX = "user_id_index";

    /**
     * The name of the index for the email column.
     */
    public static final String EMAIL_INDEX = "user_email_index";

    /**
     * The name of the first name column in the database.
     */
    public static final String FIRSTNAME_COLUMN = "firstname";

    /**
     * The name of the last name column in the database.
     */
    public static final String LASTNAME_COLUMN = "lastname";

    /**
     * The name of the email column in the database.
     */
    public static final String EMAIL_COLUMN = "email";

    /**
     * The name of the password column in the database.
     */
    public static final String PASSWORD_COLUMN = "password";

    /**
     * The name of the role column in the database.
     */
    public static final String ROLE_COLUMN = "role";

    /**
     * The name of the locked until column in the database.
     */
    public static final String LOCKED_UNTIL_COLUMN = "locked_until";

    /**
     * The user's first name.
     */
    @NotNull
    @Column(name = User.FIRSTNAME_COLUMN, nullable = false)
    @Setter
    private String firstname;

    /**
     * The user's last name.
     */
    @NotNull
    @Column(name = User.LASTNAME_COLUMN, nullable = false)
    @Setter
    private String lastname;

    /**
     * The user's email address, which also serves as their username for
     * authentication.
     */
    @NotNull
    @Column(name = User.EMAIL_COLUMN, nullable = false, unique = true)
    private String email;

    /**
     * The user's hashed password.
     */
    @NotNull
    @Column(name = User.PASSWORD_COLUMN, nullable = false)
    private String password;

    /**
     * The role assigned to the user, determining their permissions.
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'USER'")
    @Column(name = User.ROLE_COLUMN, nullable = false)
    @lombok.Builder.Default
    private Role role = Role.USER;

    /**
     * The timestamp until which the user account is locked. Null if not locked.
     */
    @Column(name = User.LOCKED_UNTIL_COLUMN)
    @Setter
    private Instant lockedUntil;

    /**
     * Returns the authorities granted to the user. In this implementation, it's
     * a single role.
     *
     * @return A collection of granted authorities.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.name()));
    }

    /**
     * Returns the username used to authenticate the user. In this case, it's
     * the email address.
     *
     * @return The user's email address.
     */
    @Override
    public String getUsername() {
        return email;
    }

    /**
     * Indicates whether the user's account has expired. An expired account
     * cannot be authenticated.
     *
     * @return {@code true} because this implementation does not support account
     * expiration.
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Indicates whether the user is locked or unlocked. A locked user cannot be
     * authenticated.
     * <p>
     * Returns true if lockedUntil is null or if the current time is past the
     * lockedUntil timestamp.
     *
     * @return {@code true} if the account is not locked, {@code false}
     * otherwise.
     */
    @Override
    public boolean isAccountNonLocked() {
        return lockedUntil == null || Instant.now().isAfter(lockedUntil);
    }

    /**
     * Indicates whether the user's credentials (password) has expired. Expired
     * credentials prevent authentication.
     *
     * @return {@code true} because this implementation does not support
     * credential expiration.
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Indicates whether the user is enabled or disabled. A disabled user cannot
     * be authenticated.
     *
     * @return {@code true} because this implementation does not support
     * disabling accounts.
     */
    @Override
    public boolean isEnabled() {
        return true;
    }

    /**
     * Updates the role of this user.
     *
     * @param newRole The new role to assign. Cannot be null.
     * @throws IllegalArgumentException if newRole is null.
     */
    public void updateRole(Role newRole) {
        if (newRole == null) {
            throw new IllegalArgumentException("User role cannot be null.");
        }
        this.role = newRole;
    }

    /**
     * Updates the user's password.
     *
     * @param newHashedPassword The new hashed password.
     */
    public void updatePassword(String newHashedPassword) {
        this.password = newHashedPassword;
    }

    /**
     * Locks the user account until the specified time.
     *
     * @param unlockTime The time when the account should be unlocked.
     */
    public void lockAccount(Instant unlockTime) {
        this.lockedUntil = unlockTime;
    }

    /**
     * Unlocks the user account.
     */
    public void unlockAccount() {
        this.lockedUntil = null;
    }
}
