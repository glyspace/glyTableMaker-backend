package org.glygen.tablemaker.persistence.dao;

import org.glygen.tablemaker.persistence.UserEntity;
import org.glygen.tablemaker.persistence.glycan.Glycan;
import org.glygen.tablemaker.persistence.glycan.GlycanTag;
import org.glygen.tablemaker.view.Filter;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@SuppressWarnings("serial")
public class GlycanSpecifications implements Specification<Glycan> {
	
	Filter filter;
	
	public GlycanSpecifications(final Filter filter) {
		super();
		this.filter = filter;
	}
  
	@Override
	public Predicate toPredicate(Root<Glycan> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
		return criteriaBuilder.like(
                root.<String>get(filter.getId()).as(String.class), "%" + filter.getValue() + "%");
	}
	
	public static Specification<Glycan> hasUserWithId(Long userid) {
	    return (root, query, criteriaBuilder) -> {
	    	query.distinct(true);
	        Join<UserEntity, Glycan> glycanUser = root.join("user");
	        return criteriaBuilder.equal(glycanUser.get("userId"), userid);
	    };
	}
	
	public static Specification<Glycan> hasGlycanTag(String label) {
	    return (root, query, criteriaBuilder) -> {
	    	query.distinct(true);
	        Join<GlycanTag, Glycan> glycanTags = root.join("tags");
	        return criteriaBuilder.like(glycanTags.get("label"), "%" + label + "%");
	    };
	}
	
	public static Specification<Glycan> orderByTags(boolean asc) {
	    return (root, query, cb) -> {
	        query.distinct(true); // Prevent duplicates
	        Join<Glycan, GlycanTag> tagsJoin = root.join("tags", JoinType.LEFT);

	        if (!Long.class.equals(query.getResultType())) {
	            if (asc)
	                query.orderBy(cb.asc(tagsJoin.get("label")));
	            else
	                query.orderBy(cb.desc(tagsJoin.get("label")));
	        }

	        return cb.conjunction();
	    };
	}

	
	/*public static Specification<Glycan> orderByTags(boolean asc) {
        return new Specification<Glycan>() {
            @Override
            public Predicate toPredicate(Root<Glycan> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
                // Join Glycan with GlycanTag
                Join<Glycan, GlycanTag> tagsJoin = root.join("tags", JoinType.LEFT);
                
                // Order by tag label
                if (asc)
                	query.orderBy(criteriaBuilder.asc(tagsJoin.get("label")));
                else 
                	query.orderBy(criteriaBuilder.desc(tagsJoin.get("label")));
                
                return criteriaBuilder.conjunction();
            }
        };
    }*/
}
