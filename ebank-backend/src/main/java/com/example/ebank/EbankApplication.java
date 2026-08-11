package com.example.ebank;

import com.example.ebank.entities.AppRole;
import com.example.ebank.entities.AppUser;
import com.example.ebank.repositories.AppRoleRepository;
import com.example.ebank.repositories.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
public class EbankApplication {

	public static void main(String[] args) {
		SpringApplication.run(EbankApplication.class, args);
	}

	@Bean
	CommandLineRunner startData(AppUserRepository userRepo,
								AppRoleRepository roleRepo,
								PasswordEncoder passwordEncoder) {
		return args -> {
			// Create roles
			if (!roleRepo.existsByRoleName("AGENT_GUICHET")) {
				roleRepo.save(new AppRole(null, "AGENT_GUICHET"));
			}
			if (!roleRepo.existsByRoleName("CLIENT")) {
				roleRepo.save(new AppRole(null, "CLIENT"));
			}

			// Create admin user if not exists
			if (!userRepo.existsByUsername("admin")) {
				AppUser user = new AppUser();
				user.setUsername("admin");
				user.setPassword(passwordEncoder.encode("admin123"));
				user.setEmail("admin@ebank.com");
				user.setEnabled(true);
				user.getRoles().add(roleRepo.findByRoleName("AGENT_GUICHET").orElseThrow());
				userRepo.save(user);
			}
		};
	}
}
