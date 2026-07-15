package com.application.Servease.auth.service;


import com.application.Servease.auth.dto.Loginrequest;
import com.application.Servease.auth.dto.RegisterRequest;

import com.application.Servease.entity.User;
import com.application.Servease.entity.enums.UserRole;
import com.application.Servease.entity.enums.UserStatus;
import com.application.Servease.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;


@Service
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository,
                       BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public String register(RegisterRequest request){
        if(request.getEmail()==null || request.getPassword()==null || request.getPassword().length()<6 || request.getPhone()==null) {
            return "Fill All Fields Correctly!";
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            return "Email already registered";
        }

        if (userRepository.existsByPhone(request.getPhone())) {
            return "Phone already registered";
        }

        User user = new User();

        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setPasswordHash(
                passwordEncoder.encode(request.getPassword())
        );
        user.setRole(request.getRole());

        if (request.getRole() == UserRole.CUSTOMER) {
            user.setStatus(UserStatus.ACTIVE);
        } else if (request.getRole() == UserRole.PROVIDER) {
            user.setStatus(UserStatus.PENDING);
        } else {
            user.setStatus(UserStatus.ACTIVE);
        }

        userRepository.save(user);

        return "User registered successfully";
    }


//    public String Login (Loginrequest loginrequest){
//
//
//        if(loginrequest.getEmail()==null || loginrequest.getPassword()==null || loginrequest.getPassword().length()<6) {
//            return "Fill All Fields Correctly!";
//        }
//        if(userRepository.findByEmail(loginrequest.getEmail())!=null) {
//
//        }
//
//    }

    }



