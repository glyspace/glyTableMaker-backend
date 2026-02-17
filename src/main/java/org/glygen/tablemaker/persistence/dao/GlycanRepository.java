package org.glygen.tablemaker.persistence.dao;

import java.util.List;

import org.glygen.tablemaker.persistence.UserEntity;
import org.glygen.tablemaker.persistence.glycan.Glycan;
import org.glygen.tablemaker.persistence.glycan.RegistrationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GlycanRepository extends JpaRepository<Glycan, Long>,  JpaSpecificationExecutor<Glycan>, GlycanRepositoryCustom {
    
	public Glycan findByGlycanIdAndUser (Long id, UserEntity user);
	
    public List<Glycan> findByGlytoucanIDIgnoreCaseAndUser(String glytoucanID, UserEntity user);
    public List<Glycan> findByWurcsIgnoreCaseAndUser(String wurcs, UserEntity user);
    public List<Glycan> findByGwsIgnoreCaseAndUser(String gws, UserEntity user);
    public List<Glycan> findByGlycoCTIgnoreCaseAndUser(String glycoCT, UserEntity user);
    public List<Glycan> findByUserAndUploadFiles_Id (UserEntity user, Long uploadId);
    public List<Glycan> findByStatus (RegistrationStatus status);
    
    public Page<Glycan> findAllByUser(UserEntity user, Pageable pageable);
    public Long countByStatus(RegistrationStatus newlyRegistered);
    
	@Query("SELECT DISTINCT g.glytoucanID FROM Glycan g")
    public List<String> findDistinctGlytoucanId();
	
	long countByGlytoucanIDInAndStatus(List<String> glytoucanids, RegistrationStatus status);
	
	public List<Glycan> findByGlytoucanIDIgnoreCase (String glytoucanID);
}
