package com.example.ebank.repositories;

import com.example.ebank.entities.Client;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository extends JpaRepository<Client, Long> {

    boolean existsByIdentityRef(String identityRef);

    boolean existsByEmail(String email);
}
