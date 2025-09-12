/*-
 * File: GroupCreationRequest.java
 * Package: org.acme.dto.requests
 * Author: xristossab
 * Created on: Sep 12, 2025-5:38:33 PM
 *
 * Description: [Add your description here]
 */
package org.acme.dto.requests;

public class GroupCreationRequest {
    private String name;

    public GroupCreationRequest() {
    }

    public GroupCreationRequest(String nameValue) {
        this.name = nameValue;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
