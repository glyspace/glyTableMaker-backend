package org.glygen.tablemaker.persistence.dao;

import java.util.ArrayList;
import java.util.List;

import org.glygen.tablemaker.persistence.glycan.Glycan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

public class GlycanRepositoryCustomImpl implements GlycanRepositoryCustom {


	@PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<Long> findAllIdsBySpecification(Specification<Glycan> spec, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        // Main query
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<Glycan> root = query.from(Glycan.class);
        query.select(root.get("glycanId")).distinct(true);

        if (spec != null) {
            Predicate predicate = spec.toPredicate(root, query, cb);
            if (predicate != null) {
                query.where(predicate);
            }
        }

        // Apply sorting
        if (pageable.getSort().isSorted()) {
            List<Order> orders = new ArrayList<>();
            for (Sort.Order order : pageable.getSort()) {
                Path<Object> path = root.get(order.getProperty());
                orders.add(order.isAscending() ? cb.asc(path) : cb.desc(path));
            }
            query.orderBy(orders);
        }

        TypedQuery<Long> typedQuery = entityManager.createQuery(query);
        typedQuery.setFirstResult((int) pageable.getOffset());
        typedQuery.setMaxResults(pageable.getPageSize());

        List<Long> ids = typedQuery.getResultList();

        // Count query
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Glycan> countRoot = countQuery.from(Glycan.class);
        countQuery.select(cb.countDistinct(countRoot));

        if (spec != null) {
            Predicate predicate = spec.toPredicate(countRoot, countQuery, cb);
            if (predicate != null) {
                countQuery.where(predicate);
            }
        }

        Long total = entityManager.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(ids, pageable, total);
    }
}

