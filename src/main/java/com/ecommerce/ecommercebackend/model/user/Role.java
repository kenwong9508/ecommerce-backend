package com.ecommerce.ecommercebackend.model.user;

import com.ecommerce.ecommercebackend.model.base.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "roles")
@Getter
@Setter
public class Role extends BaseEntity {
  @Column(length = 20, unique = true, nullable = false)
  private String name; // e.g., "USER", "ADMIN"
}
