package org.glygen.tablemaker.persistence.dao;

import org.glygen.tablemaker.persistence.UserEntity;
import org.glygen.tablemaker.persistence.protein.Glycoprotein;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface GlycoproteinRepositoryCustom {

	Page<Glycoprotein> searchGlycoproteins(String tagKeyword, String uniprotId, String name, String proteinName,
			String seqVersion, String siteNo, UserEntity user, boolean orFilter, boolean orderByTags,
			boolean orderBySites, Pageable pageable);

}
