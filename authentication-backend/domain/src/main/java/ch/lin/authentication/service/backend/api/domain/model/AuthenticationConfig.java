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

import org.hibernate.annotations.ColumnDefault;

import static ch.lin.authentication.service.backend.api.domain.model.AuthenticationConfig.TABLE_NAME;
import ch.lin.platform.domain.model.AuditableEntity;
import ch.lin.platform.domain.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Represents a configuration profile for the authentication service.
 * <p>
 * This entity is persisted in the database and holds settings related to JWT
 * (JSON Web Token) generation and lifecycle. Multiple configurations can exist,
 * but typically only one is active at a time.
 */
@Entity
@Table(name = TABLE_NAME, indexes = {
    @Index(name = AuthenticationConfig.ID_INDEX, columnList = BaseEntity.ID_COLUMN),
    @Index(name = AuthenticationConfig.NAME_INDEX, columnList = AuthenticationConfig.NAME_COLUMN)}, uniqueConstraints = {
    @UniqueConstraint(columnNames = AuthenticationConfig.NAME_COLUMN)})
@Getter
@EqualsAndHashCode(of = {"name"}, callSuper = false)
@SuperBuilder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AuthenticationConfig extends AuditableEntity {

    /**
     * The name of the configuration table in the database.
     */
    public static final String TABLE_NAME = "authentication_config";

    /**
     * The name of the index for the ID column.
     */
    public static final String ID_INDEX = "authentication_config_id_index";

    /**
     * The column name for the configuration's unique name.
     */
    public static final String NAME_COLUMN = "name";

    /**
     * The name of the index for the name column.
     */
    public static final String NAME_INDEX = "auth_config_name_index";

    /**
     * The column name for the enabled status.
     */
    public static final String ENABLED_COLUMN = "enabled";

    /**
     * The column name for the JWT expiration time.
     */
    public static final String JWT_EXPIRATION_COLUMN = "jwt_expiration";

    /**
     * The column name for the JWT refresh token expiration time.
     */
    public static final String JWT_REFRESH_EXPIRATION_COLUMN = "jwt_refresh_expiration";

    /**
     * The column name for the JWT issuer URI.
     */
    public static final String JWT_ISSUER_URI_COLUMN = "jwt_issuer_uri";

    /**
     * The unique name of the configuration profile, e.g., "default".
     */
    @NotNull
    @Column(name = AuthenticationConfig.NAME_COLUMN, nullable = false)
    private String name;

    /**
     * Indicates whether this configuration is the currently active one.
     */
    @NotNull
    @ColumnDefault("false")
    @Column(name = AuthenticationConfig.ENABLED_COLUMN, nullable = false)
    @Setter
    @lombok.Builder.Default
    private Boolean enabled = false;

    /**
     * The expiration time for the JWT access token in milliseconds.
     */
    @NotNull
    @Column(name = AuthenticationConfig.JWT_EXPIRATION_COLUMN, nullable = false)
    @Setter
    private Long jwtExpiration;

    /**
     * The expiration time for the JWT refresh token in milliseconds.
     */
    @NotNull
    @Column(name = AuthenticationConfig.JWT_REFRESH_EXPIRATION_COLUMN, nullable = false)
    @Setter
    private Long jwtRefreshExpiration;

    /**
     * The issuer URI to be included in the 'iss' claim of the JWT.
     */
    @NotNull
    @Column(name = AuthenticationConfig.JWT_ISSUER_URI_COLUMN, nullable = false)
    @Setter
    private String jwtIssuerUri;

    /**
     * Creates a new AuthenticationConfig with the specified name.
     *
     * @param name The unique name for this configuration.
     */
    public AuthenticationConfig(String name) {
        this();
        this.name = name;
    }
}
