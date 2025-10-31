package com.siseg.repository;

import com.siseg.model.Role;
import com.siseg.model.enumerations.ERole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByRoleName(ERole roleName);
}


