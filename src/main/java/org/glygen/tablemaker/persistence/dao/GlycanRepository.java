package org.glygen.tablemaker.persistence.dao;

import org.glygen.tablemaker.persistence.UserEntity;
import org.glygen.tablemaker.persistence.glycan.Glycan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface GlycanRepository extends JpaRepository<Glycan, Long>,  JpaSpecificationExecutor<Glycan>{
    
    public Glycan findByGlytoucanIDIgnoreCaseAndUser(String glytoucanID, UserEntity user);
    public Glycan findByWurcsIgnoreCaseAndUser(String wurcs, UserEntity user);
    public Glycan findByGwsIgnoreCaseAndUser(String gws, UserEntity user);
    public Glycan findByGlycoCTIgnoreCaseAndUser(String glycoCT, UserEntity user);
    public Glycan findByGlycanIdAndUser (Long id, UserEntity user);
    public Page<Glycan> findAllByUser(UserEntity user, Pageable pageable);
    public Page<Glycan> findAllByUser(UserEntity user, Specification<Glycan> spec, Pageable pageable);
}
