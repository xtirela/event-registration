package com.eventreg.eventservice.controller;

import com.eventreg.eventservice.annotation.swagger.AddressSuggestionsDto;
import com.eventreg.eventservice.annotation.swagger.SimpleErrors;
import com.eventreg.eventservice.service.implementation.AddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Address search", description = "Address suggestions for events")
@RestController
@RequestMapping("/api/address")
@RequiredArgsConstructor
public class AddressController {
  private final AddressService addressService;

  @Operation(description = "Get address suggestions matching the query")
  @AddressSuggestionsDto
  @SimpleErrors
  @PreAuthorize("hasAuthority('EVENT_VIEW')")
  @GetMapping("/suggest")
  public ResponseEntity<List<String>> addressSuggest(
      @Parameter(
              description = "Address query to search for",
              required = true,
              in = ParameterIn.QUERY)
          @RequestParam
          String query) {
    return ResponseEntity.ok(addressService.getSuggestions(query));
  }
}
