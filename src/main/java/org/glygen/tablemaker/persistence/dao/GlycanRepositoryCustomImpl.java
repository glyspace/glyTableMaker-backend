package org.glygen.tablemaker.persistence.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.glygen.tablemaker.persistence.UserEntity;
import org.glygen.tablemaker.persistence.glycan.Glycan;
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

    @Override
    public Page<Glycan> searchGlycans(String tagKeyword, String glytoucanID, String mass, UserEntity user, boolean orFilter, boolean orderByTags, Pageable pageable) {
        String baseQuery = "SELECT DISTINCT g FROM Glycan g";
        String countQuery = "SELECT COUNT(DISTINCT g) FROM Glycan g";
        
        if (tagKeyword != null || orderByTags) {
        	baseQuery = "SELECT DISTINCT g, t.label FROM Glycan g LEFT JOIN g.tags t";
        	countQuery = "SELECT COUNT(DISTINCT g) FROM Glycan g LEFT JOIN g.tags t";
        }
        
        StringBuilder whereClause = new StringBuilder("");
        Map<String, Object> params = new HashMap<>();
        
        if (tagKeyword != null) {
        	whereClause.append( " WHERE LOWER(t.label) LIKE LOWER(:tagKeyword)");
        	params.put("tagKeyword", "%" + tagKeyword + "%");
        }
        
        if (glytoucanID != null) {
        	if (orFilter) {
        		if (whereClause.isEmpty()) {
        			whereClause.append(" WHERE LOWER(g.glytoucanID) like LOWER(:glytoucanID)");
        		} else {
        			whereClause.append(" OR LOWER(g.glytoucanID) like LOWER(:glytoucanID)");
        		}
        	} else {
        		if (whereClause.isEmpty()) {
        			whereClause.append(" WHERE LOWER(g.glytoucanID) like LOWER(:glytoucanID)");
        		} else {
        			whereClause.append(" AND LOWER(g.glytoucanID) like LOWER(:glytoucanID)");
        		}
        	}
            params.put("glytoucanID", "%" + glytoucanID + "%");
        }
        if (mass != null) {
        	if (orFilter) {
        		if (whereClause.isEmpty()) {
        			whereClause.append(" WHERE CAST(g.mass AS string) like :mass");
        		} else {
        			whereClause.append(" OR CAST(g.mass AS string) like :mass");
        		}
        	} else {
        		if (whereClause.isEmpty()) {
        			whereClause.append(" WHERE CAST(g.mass AS string) like :mass");
        		} else {
        			whereClause.append(" AND CAST(g.mass AS string) like :mass");
        		}
        	}
            params.put("mass", "%" + mass + "%");
        }
        
        if (user != null) {
        	if (whereClause.isEmpty()) {
        		whereClause.append(" WHERE g.user = :user");
        	} else {
        		whereClause.append(" AND g.user = :user");
        	}
        	params.put("user", user);
        }
        
        String orderBy = "";
        // Apply sorting
        if (pageable.getSort().isSorted()) {
            for (Sort.Order order : pageable.getSort()) {
                if (order.getProperty().contains("tags")) {
                	orderBy = " ORDER BY t.label " + (order.isAscending() ? "ASC" : "DESC");
                } else {
                	orderBy = " ORDER BY " + order.getProperty() + " " + (order.isAscending() ? "ASC" : "DESC"); 
                }
            }
        }

        String finalQuery = baseQuery + whereClause.toString() + orderBy;
        String finalCountQuery = countQuery + whereClause.toString();
        

		TypedQuery<Object[]> query = entityManager.createQuery(finalQuery, Object[].class);

        //TypedQuery<Glycan> query = entityManager.createQuery(finalQuery, Glycan.class);
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

