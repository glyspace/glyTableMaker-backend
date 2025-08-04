package org.glygen.tablemaker.persistence.dao;


import org.glygen.tablemaker.persistence.glycan.Glycan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

public interface GlycanRepositoryCustom {
    Page<Long> findAllIdsBySpecification(Specification<Glycan> spec, Pageable pageable);
}

