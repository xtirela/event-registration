package com.eventreg.specification;

import java.util.List;
import lombok.Data;

@Data
public class OrFilter implements Filter {
  private final String type = "OR";
  private List<Filter> value;
}
