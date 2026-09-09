package com.eventreg.eventservice.specification;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Data
@Schema(description = "A group of filters that must all match")
public class AndFilter implements Filter {
  private final String type = "AND";

  @Schema(description = "Nested filters, all must match")
  private List<Filter> value;
}
