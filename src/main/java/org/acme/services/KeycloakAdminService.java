/*-
 * File: KeycloakAdminService.java
 * Package: org.acme.services
 * Author: xristossab
 * Created on: Sep 12, 2025-5:54:21 PM
 *
 * Description: [Add your description here]
 */
package org.acme.services;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;

import jakarta.inject.Singleton;

@Singleton
public class KeycloakAdminService {

    private final Keycloak keycloak;

    public KeycloakAdminService() {
        this.keycloak = KeycloakBuilder.builder()
                .serverUrl("http://localhost:8080")
                .realm("master")
                .grantType(OAuth2Constants.PASSWORD)
                .clientId("admin-cli")
                .username("admin")
                .password("admin")
                .build();
    }

    public Keycloak getClient() {
        return keycloak;
    }
}
