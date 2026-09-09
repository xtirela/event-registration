package com.eventreg.participantservice.aop;

import com.eventreg.participantservice.model.IdempotencyRecord;
import com.eventreg.participantservice.repository.IdempotencyRepository;
import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

/** Aspect that enforces idempotent request processing via claim-based locking. */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class IdempotencyAspect {

  private static final long CLAIM_POLL_INTERVAL_MS = 100;
  private static final long CLAIM_POLL_TIMEOUT_MS = 3_000;

  private final IdempotencyRepository idempotencyRepository;
  private final ObjectMapper objectMapper;

  /** Intercepts methods annotated with {@code @Idempotent} and deduplicates concurrent calls. */
  @Around("@annotation(com.eventreg.participantservice.annotation.Idempotent)")
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

    // Атомарный claim: победитель выполняет операцию, проигравший дожидается её результата.
    // ponytail: если победитель умрёт в процессе, claim без response_body зависнет и ретраи
    // получат 409; апгрейд — TTL-джоба, вычищающая такие строки.
    if (idempotencyRepository.claim(key, method, path) == 0) {
      return waitForResult(key, method, path, joinPoint);
    }

    try {
      Object result = joinPoint.proceed();

      IdempotencyRecord record =
          idempotencyRepository
              .findById(key)
              .orElseThrow(() -> new IllegalStateException("Idempotency claim lost: " + key));
      record.setHttpStatus(extractStatus(result));
      record.setResponseBody(objectMapper.writeValueAsString(result));
      idempotencyRepository.save(record);

      return result;
    } catch (Throwable t) {
      try {
        idempotencyRepository.deleteById(key);
      } catch (RuntimeException e) {
        log.warn("Failed to release idempotency claim {}", key, e);
      }
      throw t;
    }
  }

  private Object waitForResult(
      String key, String method, String path, ProceedingJoinPoint joinPoint) throws Throwable {
    long deadline = System.currentTimeMillis() + CLAIM_POLL_TIMEOUT_MS;
    while (true) {
      IdempotencyRecord existing =
          idempotencyRepository
              .findById(key)
              .orElseThrow(
                  () -> new ResponseStatusException(HttpStatus.CONFLICT, "Idempotency-Key lost"));

      if (!method.equals(existing.getRequestMethod()) || !path.equals(existing.getRequestPath())) {
        throw new ResponseStatusException(
            HttpStatus.CONFLICT, "Idempotency-Key reused for a different request");
      }

      if (existing.getResponseBody() != null) {
        return objectMapper.readValue(existing.getResponseBody(), getReturnType(joinPoint));
      }

      if (System.currentTimeMillis() >= deadline) {
        // ponytail: busy-wait на время обработки победителя; перейти на DB WAITFOR/outbox,
        // если этот путь станет узким местом.
        throw new ResponseStatusException(
            HttpStatus.CONFLICT, "Idempotent request still in progress, retry later");
      }

      Thread.sleep(CLAIM_POLL_INTERVAL_MS);
    }
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
