package com.eventreg.specification;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@Component
public class SpecificationCreator<T>
{
    public Specification<T> create(Filter filter) {
        return null;
    }
}
