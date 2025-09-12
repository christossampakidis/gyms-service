/*-
 * File: GroupService.java
 * Package: org.acme.services
 * Author: xristossab
 * Created on: Sep 12, 2025-5:55:47 PM
 *
 * Description: [Add your description here]
 */
package org.acme.services;

import java.util.List;

import org.eclipse.microprofile.jwt.JsonWebToken;
import org.keycloak.representations.idm.GroupRepresentation;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
public class GroupService {

    @Inject
    KeycloakAdminService keycloakProvider;

    @Inject
    JsonWebToken jwt;

    public final List<String> groupNames = List.of("Administrators", "Owner", "Sales", "Sales Administrator");

    public void createGroup(String name) {

        GroupRepresentation parentGroup = new GroupRepresentation();
        parentGroup.setName(name);

        keycloakProvider.getClient().realm("gyms-app").groups().add(parentGroup);

        List<GroupRepresentation> allGroups = keycloakProvider.getClient().realm("gyms-app").groups().groups();
        GroupRepresentation createdParent = allGroups.stream()
                .filter(g -> g.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Parent group not found"));

        for (String groupName : groupNames) {
            GroupRepresentation subGroup = new GroupRepresentation();
            subGroup.setName(groupName);
            Response response = keycloakProvider.getClient().realm("gyms-app")
                    .groups().group(createdParent.getId()).subGroup(subGroup);

            // Extract the created subgroup ID from the response
            String createdSubGroupId = extractIdFromLocationHeader(response);

            // Add the user to the newly created subgroup
            keycloakProvider.getClient().realm("gyms-app")
                    .users().get(jwt.getSubject())
                    .joinGroup(createdSubGroupId);

            response.close();

        }

    }

    private String extractIdFromLocationHeader(Response response) {
        String location = response.getHeaderString("Location");
        return location.substring(location.lastIndexOf('/') + 1);
    }

}
