package org.glygen.tablemaker.persistence.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.glygen.tablemaker.persistence.UserEntity;
import org.glygen.tablemaker.persistence.protein.Glycoprotein;
import org.glygen.tablemaker.util.QueryFilterUtil;
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
	
	private static final Set<String> SORTABLE = Set.of("tags", "siteNo", "name", "proteinName", "sequence", "geneSymbol", "sequenceVersion");


	@Override
	public Page<Glycoprotein> searchGlycoproteins(String tagKeyword, String uniprotId, String name, String proteinName,
			String seqVersion, UserEntity user, boolean orFilter, boolean orderByTags, boolean orderBySites,
			Pageable pageable) {
		String baseQuery = "SELECT DISTINCT g FROM Glycoprotein g";
        String countQuery = "SELECT COUNT(DISTINCT g) FROM Glycoprotein g";
        
        Map<String, Object> params = new HashMap<>();
        String groupByClause = "";
        
        if (tagKeyword != null || orderByTags || orderBySites) {
        	if (!orderBySites) {
	        	baseQuery = "SELECT DISTINCT g, t.label FROM Glycoprotein g LEFT JOIN g.tags t";
	        	countQuery = "SELECT COUNT(DISTINCT g) FROM Glycoprotein g LEFT JOIN g.tags t";
        	} else if (tagKeyword == null && !orderByTags){
        		baseQuery = "SELECT DISTINCT g, COUNT(s) FROM Glycoprotein g LEFT JOIN g.sites s";
	        	countQuery = "SELECT COUNT(DISTINCT g) FROM Glycoprotein g LEFT JOIN g.sites s";
	        	groupByClause = " GROUP BY g";
        	} else {
        		baseQuery = "SELECT g, COUNT(s), t.label FROM Glycoprotein g LEFT JOIN g.sites s LEFT JOIN g.tags t";
        		countQuery = "SELECT COUNT(DISTINCT g) FROM Glycoprotein g LEFT JOIN g.sites s LEFT JOIN g.tags t";
        		groupByClause = " GROUP BY g, t.label";
        	}
        }
        
        StringBuilder whereClause = new StringBuilder("");
        String where = "";
        
        if (tagKeyword != null) {
            appendCondition(whereClause, "LOWER(t.label) LIKE LOWER(:tagKeyword) ESCAPE '\\'", orFilter);
            params.put("tagKeyword", QueryFilterUtil.contains(tagKeyword));
        }
        if (uniprotId != null) {
            appendCondition(whereClause, "LOWER(g.uniprotId) like LOWER(:uniprotId) ESCAPE '\\'", orFilter);
            params.put("uniprotId", QueryFilterUtil.contains(uniprotId));
        }
        if (seqVersion != null) {
            appendCondition(whereClause, "LOWER(g.sequenceVersion) like LOWER(:sequenceVersion) ESCAPE '\\'", orFilter);
            params.put("sequenceVersion", QueryFilterUtil.contains(seqVersion));
        }
        if (name != null) {
            appendCondition(whereClause, "LOWER(g.name) like LOWER(:name) ESCAPE '\\'", orFilter);
            params.put("name", QueryFilterUtil.contains(name));
        }
        if (proteinName != null) {
            appendCondition(whereClause, "LOWER(g.proteinName) like LOWER(:proteinName) ESCAPE '\\'", orFilter);
            params.put("proteinName", QueryFilterUtil.contains(proteinName));
        }
        
        if (!whereClause.isEmpty()) {
        	if (whereClause.toString().startsWith(" AND")) {
        		where = " WHERE " + whereClause.substring(whereClause.indexOf(" AND") + 5);
        	} else if (whereClause.toString().startsWith(" OR")) {
        		where = " WHERE (" + whereClause.substring(whereClause.indexOf(" OR") + 4) + ") ";
        	}
        }
        
        if (user != null) {
        	if (where.isEmpty()) {
        		where += " WHERE g.user = :user";
        	} else {
        		where +=" AND g.user = :user";
        	}
        	params.put("user", user);
        }
        
        String orderBy = "";
        // Apply sorting
        if (pageable.getSort().isSorted()) {
            for (Sort.Order order : pageable.getSort()) {
            	if (SORTABLE.contains(order.getProperty())) {
	                if (order.getProperty().contains("tags")) {
	                	orderBy = " ORDER BY t.label " + (order.isAscending() ? "ASC" : "DESC");
	                } else if (order.getProperty().contains("siteNo")) {
	                	orderBy = " ORDER BY COUNT(s) " + (order.isAscending() ? "ASC" : "DESC");
	                } else {
	                	orderBy = " ORDER BY " + order.getProperty() + " " + (order.isAscending() ? "ASC" : "DESC"); 
	                }
            	}
            }
        }

        String finalQuery = baseQuery + where + groupByClause + orderBy;
        String finalCountQuery = countQuery + where;
       
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
	

	private void appendCondition(StringBuilder whereClause, String condition, boolean orFilter) {
		whereClause.append(orFilter ? " OR " : " AND ").append(condition);    
	}
}
