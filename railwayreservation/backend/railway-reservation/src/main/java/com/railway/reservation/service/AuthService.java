package com.railway.reservation.service;

import com.railway.reservation.model.User;
import com.railway.reservation.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User register(String username, String password, String role) {

        if (userRepository.findByUsername(username).isPresent()) {
            return null;
        }

        User user = new User(username, password, role);

        return userRepository.save(user);
    }

    public User login(String username, String password) {

        return userRepository.findByUsername(username)
                .filter(user -> user.getPassword().equals(password))
                .orElse(null);
    }

    public void createDefaultAdmin() {

        if (userRepository.findByUsername("admin").isEmpty()) {

            User admin = new User(
                    "admin",
                    "admin123",
                    "ADMIN"
            );

            userRepository.save(admin);

            System.out.println("Default admin account created.");
        }
    }
}