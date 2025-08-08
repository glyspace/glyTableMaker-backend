package org.glygen.tablemaker.persistence.dao;

import java.util.List;

import org.glygen.tablemaker.persistence.UserEntity;
import org.glygen.tablemaker.persistence.protein.Glycoprotein;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface GlycoproteinRepository extends JpaRepository<Glycoprotein, Long>, JpaSpecificationExecutor<Glycoprotein>, GlycoproteinRepositoryCustom {
	
	public Page<Glycoprotein> findAllByUser(UserEntity user, Pageable pageable);
    public List<Glycoprotein> findAllByUniprotIdAndUser (String uniprotId, UserEntity user);
    public List<Glycoprotein> findAllByNameAndUser (String name, UserEntity user);
    public Glycoprotein findByIdAndUser (Long id, UserEntity user);
    
    @Query("SELECT DISTINCT g.uniprotId FROM Glycoprotein g")
    List<String> findDistinctUniprotId();

}
