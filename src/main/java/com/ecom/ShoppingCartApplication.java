package com.ecom;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.ecom.model.UserDtls;
import com.ecom.repository.UserRepository;

@SpringBootApplication
public class ShoppingCartApplication {

	public static void main(String[] args) {
		SpringApplication.run(ShoppingCartApplication.class, args);
	}
	@Bean
	public CommandLineRunner init(UserRepository repo, PasswordEncoder encoder) {
	    return args -> {

	        if (repo.findByEmail("admin@gmail.com") == null) {

	            UserDtls admin = new UserDtls();
	            admin.setName("Admin");
	            admin.setEmail("admin@gmail.com");
	            admin.setMobileNumber("9999999999");
	            admin.setAddress("Admin Address");
	            admin.setCity("Pune");
	            admin.setState("MH");
	            admin.setPincode("411001");

	            admin.setPassword(encoder.encode("admin123"));
	            admin.setRole("ROLE_ADMIN");

	            admin.setIsEnable(true);
	            admin.setAccountNonLocked(true);
	            admin.setFailedAttempt(0);

	            repo.save(admin);
	        }
	    };
	}
}
