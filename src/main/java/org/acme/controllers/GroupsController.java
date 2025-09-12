/*-
 * File: GymsController.java
 * Package: org.acme.controllers
 * Author: Christos Sampakidis
 * Created on: 10/12/2025 at 17:26:17 PM
 *
 * Description: Gyms Controller class.
 */
package org.acme.controllers;

import org.acme.dto.requests.GroupCreationRequest;
import org.acme.services.GroupService;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.jboss.resteasy.reactive.RestResponse;

import io.quarkus.security.Authenticated;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
@Path("/groups")
public class GroupsController {

    @Inject
    GroupService groupService;

    @POST
    @Path("/create")
    @Consumes("application/json")
    @Operation(summary = "Create a new group", description = "Creates a group with the provided details")
    @Authenticated
    public RestResponse<Void> createGroup(GroupCreationRequest request) {
        try {
            groupService.createGroup(request.getName());
        } catch (Exception e) {
            return RestResponse.status(Response.Status.INTERNAL_SERVER_ERROR);
        }
        return RestResponse.status(Response.Status.CREATED);
    }

    @DELETE
    @Path("/destroy/{groupId}")
    @Operation(summary = "Destroy a group", description = "Destroys a group with the provided groupId")
    @Authenticated
    public RestResponse<Void> destroyGroup(@PathParam("groupId") String groupId) {
        try {
            groupService.DestroyGroup(groupId);
        } catch (Exception e) {
            return RestResponse.status(Response.Status.INTERNAL_SERVER_ERROR);
        }
        return RestResponse.status(Response.Status.NO_CONTENT);
    }

    @PATCH
    @Path("/update-attribute/{groupId}/{attributeName}/{newValue}")
    @Operation(summary = "Update group attribute", description = "Updates a specific attribute of a group")
    @Authenticated
    public RestResponse<Void> updateGroupAttribute(@PathParam("groupId") String groupId,
            @PathParam("attributeName") String attributeName, @PathParam("newValue") String newValue) {
        try {
            groupService.groupAttributeUpdate(groupId, attributeName, newValue);
        } catch (Exception e) {
            return RestResponse.status(Response.Status.INTERNAL_SERVER_ERROR);
        }
        return RestResponse.status(Response.Status.NO_CONTENT);
    }

}
