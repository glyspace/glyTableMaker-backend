package org.glygen.tablemaker.persistence.dao;


import org.glygen.tablemaker.persistence.UserEntity;
import org.glygen.tablemaker.persistence.glycan.Glycan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface GlycanRepositoryCustom {
	Page<Glycan> searchGlycans(String tagKeyword, String glytoucanID, String mass, UserEntity user, boolean orFilter,
			boolean orderByTags, Pageable pageable);
}

