package org.glygen.tablemaker.persistence.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.glygen.tablemaker.persistence.UserEntity;
import org.glygen.tablemaker.persistence.protein.Glycoprotein;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

public class GlycoproteinRepositoryCustomImpl implements GlycoproteinRepositoryCustom {
	
	@PersistenceContext
    private EntityManager entityManager;

	@Override
	public Page<Glycoprotein> searchGlycoproteins(String tagKeyword, String uniprotId, String name, String proteinName,
			String seqVersion, String siteNo, UserEntity user, boolean orFilter, boolean orderByTags, boolean orderBySites,
			Pageable pageable) {
		String baseQuery = "SELECT DISTINCT g FROM Glycoprotein g";
        String countQuery = "SELECT COUNT(DISTINCT g) FROM Glycoprotein g";
        
        if (tagKeyword != null || orderByTags || siteNo != null || orderBySites) {
        	if (siteNo == null && !orderBySites) {
	        	baseQuery = "SELECT DISTINCT g, t.label FROM Glycoprotein g LEFT JOIN g.tags t";
	        	countQuery = "SELECT COUNT(DISTINCT g) FROM Glycoprotein g LEFT JOIN g.tags t";
        	} else if (tagKeyword == null && !orderByTags){
        		baseQuery = "SELECT DISTINCT g, COUNT(s) FROM Glycoprotein g LEFT JOIN g.sites s";
	        	countQuery = "SELECT COUNT(DISTINCT g) FROM Glycoprotein g LEFT JOIN g.sites s";
        	} else {
        		baseQuery = "SELECT g, COUNT(s), t.label FROM Glycoprotein g LEFT JOIN g.sites s LEFT JOIN g.tags t";
        		countQuery = "SELECT COUNT(DISTINCT g) FROM Glycoprotein g LEFT JOIN g.sites s LEFT JOIN g.tags t";
        	}
        }
        
        StringBuilder whereClause = new StringBuilder("");
        Map<String, Object> params = new HashMap<>();
        
        if (tagKeyword != null) {
        	whereClause.append( " WHERE LOWER(t.label) LIKE LOWER(:tagKeyword)");
        	params.put("tagKeyword", "%" + tagKeyword + "%");
        }
        
        if (uniprotId != null) {
        	if (orFilter) {
        		if (whereClause.isEmpty()) {
        			whereClause.append(" WHERE LOWER(g.uniprotId) like LOWER(:uniprotId)");
        		} else {
        			whereClause.append(" OR LOWER(g.uniprotId) like LOWER(:uniprotId)");
        		}
        	} else {
        		if (whereClause.isEmpty()) {
        			whereClause.append(" WHERE LOWER(g.uniprotId) like LOWER(:uniprotId)");
        		} else {
        			whereClause.append(" AND LOWER(g.uniprotId) like LOWER(:uniprotId)");
        		}
        	}
            params.put("uniprotId", "%" + uniprotId + "%");
        }
        
        if (seqVersion != null) {
        	if (orFilter) {
        		if (whereClause.isEmpty()) {
        			whereClause.append(" WHERE LOWER(g.sequenceVersion) like LOWER(:sequenceVersion)");
        		} else {
        			whereClause.append(" OR LOWER(g.sequenceVersion) like LOWER(:sequenceVersion)");
        		}
        	} else {
        		if (whereClause.isEmpty()) {
        			whereClause.append(" WHERE LOWER(g.sequenceVersion) like LOWER(:sequenceVersion)");
        		} else {
        			whereClause.append(" AND LOWER(g.sequenceVersion) like LOWER(:sequenceVersion)");
        		}
        	}
            params.put("sequenceVersion", "%" + seqVersion + "%");
        }
        
        if (name != null) {
        	if (orFilter) {
        		if (whereClause.isEmpty()) {
        			whereClause.append(" WHERE LOWER(g.name) like LOWER(:name)");
        		} else {
        			whereClause.append(" OR LOWER(g.name) like LOWER(:name)");
        		}
        	} else {
        		if (whereClause.isEmpty()) {
        			whereClause.append(" WHERE LOWER(g.name) like LOWER(:name)");
        		} else {
        			whereClause.append(" AND LOWER(g.name) like LOWER(:name)");
        		}
        	}
            params.put("name", "%" + name + "%");
        }
        
        if (proteinName != null) {
        	if (orFilter) {
        		if (whereClause.isEmpty()) {
        			whereClause.append(" WHERE LOWER(g.proteinName) like LOWER(:proteinName)");
        		} else {
        			whereClause.append(" OR LOWER(g.proteinName) like LOWER(:proteinName)");
        		}
        	} else {
        		if (whereClause.isEmpty()) {
        			whereClause.append(" WHERE LOWER(g.proteinName) like LOWER(:proteinName)");
        		} else {
        			whereClause.append(" AND LOWER(g.proteinName) like LOWER(:proteinName)");
        		}
        	}
            params.put("proteinName", "%" + proteinName + "%");
        }
        
        if (siteNo != null) {
        	if (orFilter) {
        		if (whereClause.isEmpty()) {
        			whereClause.append(" WHERE CAST(COUNT(g.sites) AS string) like :siteNo");
        		} else {
        			whereClause.append(" OR CAST(COUNT(g.sites) AS string) like :siteNo");
        		}
        	} else {
        		if (whereClause.isEmpty()) {
        			whereClause.append(" WHERE CAST(COUNT(g.sites) AS string) like :siteNo");
        		} else {
        			whereClause.append(" AND CAST(COUNT(g.sites) AS string) like :siteNo");
        		}
        	}
            params.put("siteNo", "%" + siteNo + "%");
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
        String groupBy = "";
        // Apply sorting
        if (pageable.getSort().isSorted()) {
            for (Sort.Order order : pageable.getSort()) {
                if (order.getProperty().contains("tags")) {
                	orderBy = " ORDER BY t.label " + (order.isAscending() ? "ASC" : "DESC");
                } else if (order.getProperty().contains("siteNo")) {
                	orderBy = " ORDER BY COUNT(s) " + (order.isAscending() ? "ASC" : "DESC");
                	groupBy = " GROUP BY g";
                } else {
                	orderBy = " ORDER BY " + order.getProperty() + " " + (order.isAscending() ? "ASC" : "DESC"); 
                }
            }
        }

        String finalQuery = baseQuery + whereClause.toString() + groupBy + orderBy;
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
		List<Glycoprotein> glycoproteins = rawResults.stream()
		    .map(row -> (Glycoprotein) row[0])
		    .distinct() 
		    .collect(Collectors.toList());

        return new PageImpl<>(glycoproteins, pageable, count.getSingleResult());
	}

}
