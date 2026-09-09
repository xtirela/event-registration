package com.eventreg.apigateway.controller;

import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Fallback responses for downstream services routed through the gateway. */
@RestController
@RequestMapping("/fallback")
public class FallbackController {

  /**
   * Returns a service-unavailable problem detail for the given downstream service.
   *
   * @param service the name of the unavailable service
   * @return a 503 problem detail
   */
  @RequestMapping("/{service}")
  public ProblemDetail fallback(@PathVariable String service) {
    ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, "Service is down");
    problemDetail.setType(
        URI.create("https://api.eventreg.com/errors/" + service + "-unavailable"));
    problemDetail.setTitle("service-down");
    problemDetail.setProperty("service", service);
    return problemDetail;
  }
}
