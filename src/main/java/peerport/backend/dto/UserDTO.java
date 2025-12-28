package peerport.backend.dto;

import peerport.backend.model.RoleModel.Role;

public class UserDTO {
    public String userId;
    public String name;
    public String email;
    public String profilePictureUrl;
    public String idNumber;
    public Enum<Role> role;
}
