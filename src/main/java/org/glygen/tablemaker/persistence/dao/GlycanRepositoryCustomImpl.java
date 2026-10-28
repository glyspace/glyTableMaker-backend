package org.glygen.tablemaker.persistence.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.glygen.tablemaker.persistence.UserEntity;
import org.glygen.tablemaker.persistence.glycan.Glycan;
import org.glygen.tablemaker.util.QueryFilterUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

public class GlycanRepositoryCustomImpl implements GlycanRepositoryCustom {

	@PersistenceContext
    private EntityManager entityManager;
	
	private static final Set<String> SORTABLE = Set.of("tags", "glytoucanID", "wurcs", "glycoCT", "status", "mass", "condensedString", "byonicString");

    @Override
    public Page<Glycan> searchGlycans(String tagKeyword, String glytoucanID, String mass, UserEntity user, boolean orFilter, boolean orderByTags, Pageable pageable) {
        String baseQuery = "SELECT DISTINCT g FROM Glycan g";
        String countQuery = "SELECT COUNT(DISTINCT g) FROM Glycan g";
        
        if (tagKeyword != null || orderByTags) {
        	baseQuery = "SELECT DISTINCT g, t.label FROM Glycan g LEFT JOIN g.tags t";
        	countQuery = "SELECT COUNT(DISTINCT g) FROM Glycan g LEFT JOIN g.tags t";
        } 
        
        Map<String, Object> params = new HashMap<>();
        List<String> conditions = new ArrayList<>();

        if (tagKeyword != null) {
            conditions.add("LOWER(t.label) LIKE LOWER(:tagKeyword) ESCAPE '\\'");
            params.put("tagKeyword", QueryFilterUtil.contains(tagKeyword));
        }
        if (glytoucanID != null) {
            conditions.add("LOWER(g.glytoucanID) LIKE LOWER(:glytoucanID) ESCAPE '\\'");
            params.put("glytoucanID", QueryFilterUtil.contains(glytoucanID));
        }
        if (mass != null) {
            conditions.add("CAST(g.mass AS string) LIKE :mass ESCAPE '\\'");
            params.put("mass", QueryFilterUtil.contains(mass));
        }

        StringBuilder whereClause = new StringBuilder();
        if (!conditions.isEmpty()) {
            whereClause.append(" WHERE (")
                       .append(String.join(orFilter ? " OR " : " AND ", conditions))
                       .append(")");
        }

        if (user != null) {
            whereClause.append(whereClause.isEmpty() ? " WHERE " : " AND ").append("g.user = :user");
            params.put("user", user);
        }
        
        String orderBy = "";
        // Apply sorting
        if (pageable.getSort().isSorted()) {
            for (Sort.Order order : pageable.getSort()) {
            	if (SORTABLE.contains(order.getProperty())) {
	                if (order.getProperty().contains("tags")) {
	                	orderBy = " ORDER BY t.label " + (order.isAscending() ? "ASC" : "DESC");
	                } else {
	                	orderBy = " ORDER BY " + order.getProperty() + " " + (order.isAscending() ? "ASC" : "DESC"); 
	                }
            	}
            }
        }

        String finalQuery = baseQuery + whereClause.toString() + orderBy;
        String finalCountQuery = countQuery + whereClause.toString();
        

		TypedQuery<Object[]> query = entityManager.createQuery(finalQuery, Object[].class);
        TypedQuery<Long> count = entityManager.createQuery(finalCountQuery, Long.class);

        params.forEach((k, v) -> {
            query.setParameter(k, v);
            count.setParameter(k, v);
        });

        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());
        

		List<Object[]> rawResults = query.getResultList();
		List<Glycan> glycans = rawResults.stream()
		    .map(row -> (Glycan) row[0])
		    .distinct() 
		    .collect(Collectors.toList());

        return new PageImpl<>(glycans, pageable, count.getSingleResult());
    }

}

