/*-
 * File: GroupService.java
 * Package: org.acme.services
 * Author: xristossab
 * Created on: Sep 12, 2025-5:55:47 PM
 *
 * Description: [Add your description here]
 */
package org.acme.services;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        Map<String, List<String>> attributes = new HashMap<>();
        attributes.put("createdBy", List.of(jwt.getSubject()));
        attributes.put("enabled", List.of("true"));
        GroupRepresentation parentGroup = new GroupRepresentation();
        parentGroup.setName(name);
        parentGroup.setAttributes(attributes);

        keycloakProvider.getClient().realm("gyms-app").groups().add(parentGroup);

        List<GroupRepresentation> allGroups = keycloakProvider.getClient().realm("gyms-app").groups().groups();
        GroupRepresentation createdParent = allGroups.stream()
                .filter(g -> g.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Parent group not found"));

        for (String groupName : groupNames) {
            GroupRepresentation subGroup = new GroupRepresentation();
            subGroup.setName(groupName);
            subGroup.setAttributes(attributes);
            Response response = keycloakProvider.getClient().realm("gyms-app")
                    .groups().group(createdParent.getId()).subGroup(subGroup);

            String createdSubGroupId = extractIdFromLocationHeader(response);

            keycloakProvider.getClient().realm("gyms-app")
                    .users().get(jwt.getSubject())
                    .joinGroup(createdSubGroupId);

            response.close();

        }

    }

    public void LeaveGroup(String groupId) {
        keycloakProvider.getClient().realm("gyms-app")
                .users().get(jwt.getSubject())
                .leaveGroup(groupId);
    }

    public void DestroyGroup(String groupId) {
        String currentUserId = jwt.getSubject();

        GroupRepresentation group = keycloakProvider.getClient().realm("gyms-app")
                .groups().group(groupId).toRepresentation();

        if (group.getAttributes() == null ||
                !group.getAttributes().containsKey("createdBy")) {
            throw new SecurityException("Group creator information not found");
        }

        List<String> createdByValues = group.getAttributes().get("createdBy");
        if (createdByValues.isEmpty()) {
            throw new SecurityException("Group creator information not found");
        }

        String groupCreatorId = createdByValues.get(0);

        if (!currentUserId.equals(groupCreatorId)) {
            throw new SecurityException("Only the group creator can destroy this group");
        }

        keycloakProvider.getClient().realm("gyms-app")
                .groups().group(groupId).remove();
    }

    public void groupAttributeUpdate(String groupId, String attributeName, String newValue) {
        updateGroupAttribute(groupId, attributeName, newValue);
    }

    private String extractIdFromLocationHeader(Response response) {
        String location = response.getHeaderString("Location");
        return location.substring(location.lastIndexOf('/') + 1);
    }

    public void updateGroupAttribute(String groupId, String attributeName, String newValue) {
        GroupRepresentation group = keycloakProvider.getClient().realm("gyms-app")
                .groups().group(groupId).toRepresentation();

        Map<String, List<String>> attributes = group.getAttributes();
        if (attributes == null) {
            attributes = new HashMap<>();
        }
        attributes.put(attributeName, Arrays.asList(newValue));

        group.setAttributes(attributes);

        keycloakProvider.getClient().realm("gyms-app")
                .groups().group(groupId).update(group);
    }

}
