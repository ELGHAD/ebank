package com.example.ebank.services;

import com.example.ebank.dto.AddClientRequest;
import com.example.ebank.dto.AddClientResponse;
import com.example.ebank.entities.AppUser;
import com.example.ebank.entities.Client;
import com.example.ebank.repositories.AppRoleRepository;
import com.example.ebank.repositories.AppUserRepository;
import com.example.ebank.repositories.ClientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ClientServiceImpl implements ClientService {

    private final ClientRepository clientRepo;
    private final AppUserRepository userRepo;
    private final AppRoleRepository roleRepo;
    private final PasswordEncoder passwordEncoder;

    @Override
    public AddClientResponse addClient(AddClientRequest req) {

        if (clientRepo.existsByIdentityRef(req.getIdentityRef())) {
            throw new RuntimeException("Identity reference already exists");
        }

        if (clientRepo.existsByEmail(req.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        String username = req.getEmail().split("@")[0];
        String password = "client123";

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setEmail(req.getEmail());
        user.setPassword(passwordEncoder.encode(password));
        user.setEnabled(true);
        user.getRoles().add(
                roleRepo.findByRoleName("CLIENT").orElseThrow()
        );
        userRepo.save(user);

        Client client = new Client();
        client.setIdentityRef(req.getIdentityRef());
        client.setFirstname(req.getFirstname());
        client.setLastname(req.getLastname());
        client.setBirthDate(req.getBirthDate());
        client.setEmail(req.getEmail());
        client.setAddress(req.getAddress());
        client.setAppUser(user);

        clientRepo.save(client);

        return new AddClientResponse(
                client.getId(),
                client.getIdentityRef(),
                client.getEmail(),
                "Client created successfully with login: " + username
        );
    }
}
