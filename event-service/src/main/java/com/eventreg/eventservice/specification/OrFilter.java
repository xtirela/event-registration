package com.eventreg.eventservice.specification;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;

@Data
@Schema(description = "A group of filters where any one must match")
public class OrFilter implements Filter {
  private final String type = "OR";

  @Schema(description = "Nested filters, any one must match")
  private List<Filter> value;
}
