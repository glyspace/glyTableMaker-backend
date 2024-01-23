package org.glygen.tablemaker.persistence.dao;

import org.glygen.tablemaker.persistence.UserEntity;
import org.glygen.tablemaker.persistence.glycan.Glycan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GlycanRepository extends JpaRepository<Glycan, Long> {
    
    public Glycan findByGlytoucanIDIgnoreCase(String glytoucanID);
    public Glycan findByWurcsIgnoreCase(String wurcs);
    public Glycan findByGwsIgnoreCase(String gws);
    public Glycan findByGlycoCTIgnoreCase(String glycoCT);
    public Page<Glycan> findAllByUser(UserEntity user, Pageable pageable);
    

}
