package com.aisales.tenant.application.service;

import com.aisales.common.exception.exception.NotFoundException;
import com.aisales.tenant.api.request.UserRequest;
import com.aisales.tenant.api.response.UserResponse;
import com.aisales.tenant.application.mapper.UserMapper;
import com.aisales.tenant.domain.entity.User;
import com.aisales.tenant.infrastructure.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional
    public UserResponse create(UserRequest request) {
        User user = userMapper.toEntity(request);
        return userMapper.toResponse(userRepository.save(user));
    }

    public UserResponse getById(UUID id) {
        return userRepository.findById(id)
                .map(userMapper::toResponse)
                .orElseThrow(() -> new NotFoundException("User not found: " + id));
    }

    public List<UserResponse> listByTenant(String tenantId) {
        return userRepository.findByTenantId(tenantId).stream().map(userMapper::toResponse).toList();
    }
}
