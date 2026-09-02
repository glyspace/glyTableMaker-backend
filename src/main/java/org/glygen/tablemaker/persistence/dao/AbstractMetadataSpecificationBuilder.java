package org.glygen.tablemaker.persistence.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.glygen.tablemaker.view.Filter;
import org.glygen.tablemaker.view.Sorting;
import org.springframework.data.jpa.domain.Specification;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

public abstract class AbstractMetadataSpecificationBuilder<T> {
    protected abstract Map<String, String> nativeFieldPaths();
    protected abstract Map<String, List<String>> nestedSortPaths();
    
    private static final Set<String> BLOCKED_FIELDS = Set.of("contributor");

    // Top-level {id, name, uri} object — sort should compare on "name", not raw serialized object
    private static final Set<String> SINGLE_OBJECT_FIELDS = Set.of(
        "species", "tissue", "cellline", "mutantSpecies", "speciesOrigin"
    );

    // multiple == true — filter works generically (serialized array text), sort has no sensible ordering
    private static final Set<String> MULTI_VALUE_FIELDS = Set.of(
        "disease", "cellularComponent", "chemical", "drug", "radiation",
        "experimentalTechnique", "technique"
    );
    
    protected List<Order> defaultSortOrders(Root<T> root, CriteriaBuilder cb) {
        return List.of(); // no default unless a subclass overrides
    }

    public Specification<T> build(List<Filter> filters, List<Sorting> sortingList,
                                   Long versionId, String datasetId) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (versionId != null) {
                predicates.add(cb.equal(root.get("dataset").get("versionId"), versionId));
            } else if (datasetId != null) {
                predicates.add(cb.equal(root.get("dataset").get("dataset").get("datasetIdentifier"), datasetId));
                predicates.add(cb.isTrue(root.get("dataset").get("head")));
            }

            if (filters != null) {
                for (Filter f : filters) {
                	predicates.add(buildFilterPredicate(root, cb, f));
                }
            }

            if (sortingList != null && !sortingList.isEmpty()) {
                List<Order> orders = new ArrayList<>();
                for (Sorting s : sortingList) {
                    orders.addAll(resolveSortOrders(root, cb, s));
                }
                orders.add(cb.asc(root.get("id"))); // stable tiebreaker, always last
                query.orderBy(orders);
            } else {
                query.orderBy(defaultSortOrders(root, cb));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
    
	protected List<Order> resolveSortOrders(Root<T> root, CriteriaBuilder cb, Sorting s) {
        if (BLOCKED_FIELDS.contains(s.getId()) || MULTI_VALUE_FIELDS.contains(s.getId())) {
            throw new IllegalArgumentException("Field '" + s.getId() + "' does not support sorting.");
        }
        Expression<String> expr = resolveSortExpression(root, cb, s.getId());
        return List.of(s.getDesc() ? cb.desc(expr) : cb.asc(expr));
    }
    
    protected Expression<String> resolveSortExpression(Root<T> root, CriteriaBuilder cb, String fieldId) {
        String nativePath = nativeFieldPaths().get(fieldId);
        if (nativePath != null) return root.get(nativePath);
        
        Expression<JsonNode> jsonValue = root.get("metadataGroup").get("value");
        Expression<String> expr;

        if (nestedSortPaths().containsKey(fieldId)) {
            List<String> path = nestedSortPaths().get(fieldId);
            List<Expression<?>> args = new ArrayList<>();
            args.add(jsonValue);
            for (String segment : path) args.add(cb.literal(segment));
            expr = cb.function("jsonb_extract_path_text", String.class, args.toArray(new Expression[0]));
        } else if (SINGLE_OBJECT_FIELDS.contains(fieldId)) {
            expr = cb.function("jsonb_extract_path_text", String.class,
                jsonValue, cb.literal(fieldId), cb.literal("name"));
        } else {
            expr = cb.function("jsonb_extract_path_text", String.class, jsonValue, cb.literal(fieldId));
        }
        
        return expr;
    }

    protected Expression<String> resolveExpression(Root<T> root, CriteriaBuilder cb, String fieldId) {
        String nativePath = nativeFieldPaths().get(fieldId);
        if (nativePath != null) {
            return root.get(nativePath);
        }
        return cb.function(
            "jsonb_extract_path_text", String.class,
            root.get("metadataGroup").get("value"),
            cb.literal(fieldId)
        );
    }
    
    protected Predicate buildFilterPredicate(Root<T> root, CriteriaBuilder cb, Filter f) {
        String fieldId = f.getId();

        if (BLOCKED_FIELDS.contains(fieldId)) {
            throw new IllegalArgumentException("Field '" + fieldId + "' is not supported for filtering.");
        }

        String nativePath = nativeFieldPaths().get(fieldId);
        Expression<String> expr = nativePath != null
            ? root.get(nativePath)
            : cb.function("jsonb_extract_path_text", String.class,
                          root.get("metadataGroup").get("value"), cb.literal(fieldId));

        return cb.like(cb.lower(expr), "%" + f.getValue().toLowerCase() + "%");
    }
}
