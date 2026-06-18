package com.ecommerce.ecommercebackend.dto.user;

import com.ecommerce.ecommercebackend.model.user.Role;
import com.ecommerce.ecommercebackend.model.user.User;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Data;

@Data
public class UserResponse {

  private Long id;
  private String username;
  private String email;
  private Set<String> roles;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public static UserResponse fromEntity(User user) {
    UserResponse response = new UserResponse();
    response.setId(user.getId());
    response.setUsername(user.getUsername());
    response.setEmail(user.getEmail());
    response.setCreatedAt(user.getCreatedAt());
    response.setUpdatedAt(user.getUpdatedAt());

    if (user.getRoles() != null) {
      response.setRoles(user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()));
    }

    return response;
  }
}
