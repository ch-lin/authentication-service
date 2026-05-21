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

import static ch.lin.authentication.service.backend.api.domain.model.Client.TABLE_NAME;
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
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * Represents a machine client for service-to-service authentication. This class
 * is a JPA entity that maps to the "client" table in the database.
 */
@Entity
@Table(name = TABLE_NAME, indexes = {
    @Index(name = Client.ID_INDEX, columnList = BaseEntity.ID_COLUMN),
    @Index(name = Client.CLIENT_ID_INDEX, columnList = Client.CLIENT_ID_COLUMN)}, uniqueConstraints = {
    @UniqueConstraint(columnNames = Client.CLIENT_ID_COLUMN)})
@Getter
@ToString
@EqualsAndHashCode(of = {"clientId"}, callSuper = false)
@SuperBuilder(toBuilder = true)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Client extends AuditableEntity {

    /**
     * The name of the database table for clients.
     */
    public static final String TABLE_NAME = "client";

    /**
     * The name of the index for the ID column.
     */
    public static final String ID_INDEX = "client_id_index";

    /**
     * The name of the client name column in the database.
     */
    public static final String CLIENT_NAME_COLUMN = "client_name";

    /**
     * The name of the index for the client ID column.
     */
    public static final String CLIENT_ID_INDEX = "client_clientid_index";

    /**
     * The name of the client ID column in the database.
     */
    public static final String CLIENT_ID_COLUMN = "client_id";

    /**
     * The name of the client secret column in the database.
     */
    public static final String CLIENT_SECRET_COLUMN = "client_secret";

    /**
     * The name of the role column in the database.
     */
    public static final String ROLE_COLUMN = "role";

    /**
     * A human-readable name for the client application.
     */
    @NotNull
    @Column(name = Client.CLIENT_NAME_COLUMN, nullable = false)
    @Setter
    private String clientName;

    /**
     * The public, unique identifier for the client, used for authentication.
     */
    @NotNull
    @Column(name = Client.CLIENT_ID_COLUMN, nullable = false, unique = true)
    private String clientId;

    /**
     * The confidential secret for the client, used for authentication. This
     * should be stored in a hashed format in the database.
     */
    @NotNull
    @Column(name = Client.CLIENT_SECRET_COLUMN, nullable = false)
    private String clientSecret;

    /**
     * The role assigned to the client, which determines its permissions.
     */
    @NotNull
    @Enumerated(EnumType.STRING)
    @ColumnDefault("'SERVICE'")
    @Column(name = Client.ROLE_COLUMN, nullable = false)
    @lombok.Builder.Default
    private Role role = Role.SERVICE;

    /**
     * Updates the role of this client.
     *
     * @param newRole The new role to assign. Cannot be null.
     * @throws IllegalArgumentException if newRole is null.
     */
    public void updateRole(Role newRole) {
        if (newRole == null) {
            throw new IllegalArgumentException("Client role cannot be null.");
        }
        this.role = newRole;
    }
}
