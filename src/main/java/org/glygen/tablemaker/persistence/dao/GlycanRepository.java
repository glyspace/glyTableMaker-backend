package org.glygen.tablemaker.persistence.dao;

import java.util.List;

import org.glygen.tablemaker.persistence.UserEntity;
import org.glygen.tablemaker.persistence.glycan.Glycan;
import org.glygen.tablemaker.persistence.glycan.RegistrationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface GlycanRepository extends JpaRepository<Glycan, Long>,  JpaSpecificationExecutor<Glycan>{
    
    public Glycan findByGlytoucanIDIgnoreCaseAndUser(String glytoucanID, UserEntity user);
    public Glycan findByWurcsIgnoreCaseAndUser(String wurcs, UserEntity user);
    public Glycan findByGwsIgnoreCaseAndUser(String gws, UserEntity user);
    public Glycan findByGlycoCTIgnoreCaseAndUser(String glycoCT, UserEntity user);
    public Glycan findByGlycanIdAndUser (Long id, UserEntity user);
    public Page<Glycan> findAllByUser(UserEntity user, Pageable pageable);
    public Page<Glycan> findAll(Specification<Glycan> spec, Pageable pageable);
    public List<Glycan> findByUserAndUploadFiles_Id (UserEntity user, Long uploadId);
	
    public Long countByStatus(RegistrationStatus newlyRegistered);
    
	@Query("SELECT DISTINCT g.glytoucanID FROM Glycan g")
    List<String> findDistinctGlytoucanId();
}
