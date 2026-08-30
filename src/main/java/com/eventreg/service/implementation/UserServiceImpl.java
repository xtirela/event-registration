package com.eventreg.service.implementation;

import com.eventreg.annotation.Idempotent;
import com.eventreg.dto.request.update.UserUpdateRequest;
import com.eventreg.exception.UserNotFoundException;
import com.eventreg.mapper.UserMapper;
import com.eventreg.model.User;
import com.eventreg.repository.UserRepository;
import com.eventreg.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@AllArgsConstructor
@Service
public class UserServiceImpl implements UserService {

  private final UserRepository userRepository;
  private final UserMapper userMapper;

  @Override
  @Idempotent
  @Transactional
  public User updateUser(Long userId, UserUpdateRequest userUpdateRequest) {
    User user = findById(userId);
    userMapper.userUpdateRequestToUser(userUpdateRequest, user);
    return userRepository.save(user);
  }

  @Idempotent
  @Override
  @Transactional
  public void deleteUser(Long userId) {
    User user = findById(userId);
    userRepository.delete(user);
  }

  @Override
  @Transactional(readOnly = true)
  public User findById(Long userId) {
    return userRepository
        .findById(userId)
        .orElseThrow(() -> new UserNotFoundException(userId, "findById"));
  }

  @Override
  @Transactional(readOnly = true)
  public Page<User> findAll(Specification<User> spec, Pageable page) {
    return userRepository.findAll(spec, page);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<User> findAll(Pageable page) {
    return userRepository.findAll(page);
  }
}
