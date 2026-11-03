package org.glygen.tablemaker.persistence.dao;

import java.util.Collection;
import java.util.List;

import org.glygen.tablemaker.persistence.UserEntity;
import org.glygen.tablemaker.persistence.glycan.GlycanTag;
import org.glygen.tablemaker.view.TagView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GlycanTagRepository extends JpaRepository<GlycanTag, Long> {
	
	@Query("""
	    SELECT new org.glygen.tablemaker.view.TagView(
	        t.tagId, t.label,
	        (SELECT COUNT(g) FROM Glycan g JOIN g.tags gt WHERE gt.tagId = t.tagId),
	        (SELECT COUNT(p) FROM Glycoprotein p JOIN p.tags pt WHERE pt.tagId = t.tagId))
	    FROM GlycanTag t
	    WHERE t.user = :user
	    ORDER BY t.label
	    """)
	List<TagView> findTagViews(@Param("user") UserEntity user);
	GlycanTag findByTagIdAndUser(Long tagId, UserEntity user);
	GlycanTag findByUserAndLabelIgnoreCase(UserEntity user, String label);
	Collection<GlycanTag> findAllByUser (UserEntity user);
}
