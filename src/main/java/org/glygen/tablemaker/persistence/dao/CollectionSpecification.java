package org.glygen.tablemaker.persistence.dao;

import org.glygen.tablemaker.persistence.UserEntity;
import org.glygen.tablemaker.persistence.glycan.Collection;
import org.glygen.tablemaker.view.Filter;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@SuppressWarnings("serial")
public class CollectionSpecification implements Specification<Collection> {
	Filter filter;
	
	public CollectionSpecification(final Filter filter) {
		super();
		this.filter = filter;
	}
  
	@Override
	public Predicate toPredicate(Root<Collection> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
		return criteriaBuilder.like(
                root.<String>get(filter.getId()).as(String.class), "%" + filter.getValue() + "%");
	}
	
	public static Specification<Collection> hasUserWithId(Long userid) {
	    return (root, query, criteriaBuilder) -> {
	        Join<UserEntity, Collection> collectionUser = root.join("user");
	        return criteriaBuilder.equal(collectionUser.get("userId"), userid);
	    };
	}
}
