package com.mockarena.identity.domain;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface IdentityUserRepository extends JpaRepository<IdentityUser,UUID>{ Optional<IdentityUser> findByEmail(String email); }
