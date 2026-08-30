package com.eventreg.specification;

import java.util.List;
import lombok.Data;

@Data
public class AndFilter implements Filter {
  private final String type = "AND";
  private List<Filter> value;
}
