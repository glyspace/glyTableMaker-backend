package org.glygen.tablemaker.persistence.dao;

import java.util.List;

import org.glygen.tablemaker.persistence.UserEntity;
import org.glygen.tablemaker.persistence.protein.Glycoprotein;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface GlycoproteinRepository extends JpaRepository<Glycoprotein, Long>, JpaSpecificationExecutor<Glycoprotein> {
	
	public Page<Glycoprotein> findAllByUser(UserEntity user, Pageable pageable);
    public Page<Glycoprotein> findAll(Specification<Glycoprotein> spec, Pageable pageable);
    
    public List<Glycoprotein> findAllByUniprotIdAndUser (String uniprotId, UserEntity user);

}
