package org.glygen.tablemaker.persistence.dao;

import org.glygen.tablemaker.persistence.glycan.Glycan;
import org.springframework.data.jpa.domain.Specification;

public class GlycanSpecifications {
    
    /*public static Specification<Glycan> glycanHasGlytoucanId (String glytoucan) {
        return (root, query, builder) -> {
            return builder.like(root.get(Glycan_.glytoucanID), glytoucan);
        };
    }*/
}
