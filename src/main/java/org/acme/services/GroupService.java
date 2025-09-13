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
import org.keycloak.admin.client.resource.GroupResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
public class GroupService {

    @Inject
    KeycloakAdminService keycloakProvider;

    @Inject
    JsonWebToken jwt;

    private static final Map<String, List<String>> GROUP_ROLE_MAPPING = Map.of(
            "Administrators", List.of("create-group"),
            "Owner", List.of("create-group"),
            "Sales", List.of("create-group"),
            "Sales Administrator", List.of("create-group"),
            "Trainer", List.of("create-group"),
            "Member", List.of("create-group"));

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

        for (String groupName : GROUP_ROLE_MAPPING.keySet()) {
            GroupRepresentation subGroup = new GroupRepresentation();
            subGroup.setName(groupName);
            subGroup.setAttributes(attributes);
            try (Response response = keycloakProvider.getClient().realm("gyms-app")
                    .groups().group(createdParent.getId()).subGroup(subGroup)) {
                String createdSubGroupId = extractIdFromLocationHeader(response);

                List<String> rolesForGroup = GROUP_ROLE_MAPPING.getOrDefault(groupName, List.of());
                if (!rolesForGroup.isEmpty()) {
                    assignRolesToGroup(createdSubGroupId, rolesForGroup);
                }

                keycloakProvider.getClient().realm("gyms-app")
                        .users().get(jwt.getSubject())
                        .joinGroup(createdSubGroupId);
            }

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

    private void assignRolesToGroup(String groupId, List<String> roleNames) {
        RealmResource realm = keycloakProvider.getClient().realm("gyms-app");
        GroupResource groupResource = realm.groups().group(groupId);

        List<RoleRepresentation> roles = roleNames.stream()
                .map(roleName -> realm.roles().get(roleName).toRepresentation())
                .toList();

        groupResource.roles().realmLevel().add(roles);
    }

}
