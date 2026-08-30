package com.eventreg.service.implementation;

import com.kuliginstepan.dadata.client.DadataClient;
import com.kuliginstepan.dadata.client.domain.address.AddressRequestBuilder;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AddressService {
  private final DadataClient dadataClient; // <-- Внедряем готовый бин

  public List<String> getSuggestions(String query) {
    return dadataClient
        .suggestAddress(AddressRequestBuilder.create(query).build())
        .map(suggestion -> suggestion.getValue()) // Достаем подсказки
        .collectList()
        .block(); // Блокируем, чтобы получить список (или работай с Flux)
  }
}
