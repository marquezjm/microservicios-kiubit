package com.kubit.authservice.domain.entity;

import static org.junit.jupiter.api.Assertions.*;

import com.kubit.authservice.domain.repository.RoleRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
class RoleIntegrationTest {

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void shouldPersistRole() {
        Role role = Role.builder().name("ADMIN").build();
        Role saved = roleRepository.save(role);

        assertNotNull(saved.getId());
        assertEquals("ADMIN", saved.getName());
    }

    @Test
    void shouldNotAllowDuplicateRoleNames() {
        roleRepository.save(Role.builder().name("USER").build());
        Role duplicate = Role.builder().name("USER").build();

        assertThrows(Exception.class, () -> roleRepository.saveAndFlush(duplicate));
    }
}
