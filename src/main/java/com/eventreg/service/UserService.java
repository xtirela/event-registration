package com.eventreg.service;

import com.eventreg.dto.request.update.UserUpdateRequest;
import com.eventreg.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

public interface UserService {

  User updateUser(Long userId, UserUpdateRequest userUpdateRequest);

  void deleteUser(Long userId);

  User findById(Long userId);

  Page<User> findAll(Specification<User> spec, Pageable page);

  Page<User> findAll(Pageable page);
}
