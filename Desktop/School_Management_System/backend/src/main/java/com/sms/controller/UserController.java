package com.sms.controller;

import com.sms.dto.response.ApiResponse;
import com.sms.entity.User;
import com.sms.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.sms.security.CustomUserDetails;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<User>> getProfile(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(userDetails.getUser()));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<User>> updateProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody User user) {
        User existing = userDetails.getUser();
        existing.setFirstName(user.getFirstName());
        existing.setLastName(user.getLastName());
        existing.setPhone(user.getPhone());
        existing.setProfilePhoto(user.getProfilePhoto());
        return ResponseEntity.ok(ApiResponse.success("Profile updated", userRepository.save(existing)));
    }

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

}
