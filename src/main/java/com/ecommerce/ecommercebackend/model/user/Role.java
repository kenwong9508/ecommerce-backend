package com.ecommerce.ecommercebackend.model.user;

import com.ecommerce.ecommercebackend.model.base.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "roles")
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class Role extends BaseEntity {
    @Column(length = 20, unique = true, nullable = false)
    private String name; // e.g., "USER", "ADMIN"
}
