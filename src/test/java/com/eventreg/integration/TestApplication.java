package com.eventreg.integration;

import com.eventreg.PartyApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.FilterType;

/**
 * Test-only Spring Bootstrap that mirrors {@link PartyApplication} but does NOT enable scheduling.
 *
 * <p>Keeping {@link WaitingQueueScheduler} as a plain bean (so tests can invoke it manually) is
 * required for deterministic waitlist integration tests; the production {@code @EnableScheduling}
 * on {@link PartyApplication} would otherwise run the scheduler in the background during tests.
 */
@Configuration
@EnableAutoConfiguration
@EnableAspectJAutoProxy
@ComponentScan(
    basePackages = "com.eventreg",
    excludeFilters =
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = PartyApplication.class))
public class TestApplication {}
