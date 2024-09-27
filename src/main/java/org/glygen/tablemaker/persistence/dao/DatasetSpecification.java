package org.glygen.tablemaker.persistence.dao;

import org.glygen.tablemaker.persistence.UserEntity;
import org.glygen.tablemaker.persistence.dataset.Dataset;
import org.glygen.tablemaker.view.Filter;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@SuppressWarnings("serial")
public class DatasetSpecification implements Specification<Dataset> {
	
	Filter filter;
	
	public DatasetSpecification(final Filter filter) {
		super();
		this.filter = filter;
	}

	@Override
	public Predicate toPredicate(Root<Dataset> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
		return criteriaBuilder.like(
                root.<String>get(filter.getId()).as(String.class), "%" + filter.getValue() + "%");
	}
	
	public static Specification<Dataset> hasUserWithId(Long userid) {
	    return (root, query, criteriaBuilder) -> {
	        Join<UserEntity, Dataset> datasetUser = root.join("user");
	        return criteriaBuilder.equal(datasetUser.get("userId"), userid);
	    };
	}
	
	public static Specification<Dataset> hasUserWithUsername(String username) {
	    return (root, query, criteriaBuilder) -> {
	        Join<UserEntity, Dataset> datasetUser = root.join("user");
	        return criteriaBuilder.or(
	        		criteriaBuilder.like(criteriaBuilder.lower(datasetUser.get("username")), "%" + username.toLowerCase() + "%"),
	        		criteriaBuilder.like(criteriaBuilder.lower(datasetUser.get("firstName")), "%" + username.toLowerCase() + "%"),
	        		criteriaBuilder.like(criteriaBuilder.lower(datasetUser.get("lastName")), "%" + username.toLowerCase() + "%"));
	    };
	}

}
