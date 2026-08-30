package com.eventreg.aop;

import com.eventreg.model.IdempotencyRecord;
import com.eventreg.repository.IdempotencyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class IdempotencyAspect {

  private final IdempotencyRepository idempotencyRepository;
  private final ObjectMapper objectMapper;

  @Around("@annotation(com.eventreg.annotation.Idempotent)")
  public Object checkIdempotency(ProceedingJoinPoint joinPoint) throws Throwable {
    log.info("Aspect called for method: {}", joinPoint.getSignature().getName());

    HttpServletRequest request = getCurrentRequest();

    String key = request.getHeader("Idempotency-Key");
    if (key == null) {
      // Нет ключа — просто выполняем
      return joinPoint.proceed();
    }

    String method = request.getMethod();
    String path = request.getRequestURI();

    Optional<IdempotencyRecord> existing = idempotencyRepository.findById(key);
    if (existing.isPresent()
        && existing.get().getRequestMethod().equals(method)
        && existing.get().getRequestPath().equals(path)) {

      return objectMapper.readValue(existing.get().getResponseBody(), getReturnType(joinPoint));
    }

    Object result = joinPoint.proceed();

    IdempotencyRecord record =
        IdempotencyRecord.builder()
            .key(key)
            .requestMethod(method)
            .requestPath(path)
            .httpStatus(extractStatus(result)) // или другой
            .responseBody(objectMapper.writeValueAsString(result))
            .createdAt(OffsetDateTime.now())
            .build();

    idempotencyRepository.save(record);

    return result;
  }

  private HttpStatus extractStatus(Object result) {
    if (result instanceof ResponseEntity<?> responseEntity) {
      return (HttpStatus) responseEntity.getStatusCode();
    }
    return HttpStatus.OK;
  }

  private Class<?> getReturnType(ProceedingJoinPoint joinPoint) {
    MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
    return methodSignature.getReturnType();
  }

  private HttpServletRequest getCurrentRequest() {
    ServletRequestAttributes attrs =
        (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    if (attrs == null) {
      throw new IllegalStateException("No current HTTP request");
    }
    return attrs.getRequest();
  }
}
