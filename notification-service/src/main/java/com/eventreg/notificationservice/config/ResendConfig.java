package com.eventreg.notificationservice.config;

import com.resend.Resend;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Spring configuration that provisions the Resend email client bean. */
@Configuration
public class ResendConfig {

  @Bean
  public Resend resend(@Value("${resend.api-key}") String apiKey) {
    return new Resend(apiKey);
  }
}
