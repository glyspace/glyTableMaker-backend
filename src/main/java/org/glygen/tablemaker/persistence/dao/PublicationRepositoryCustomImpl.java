package org.glygen.tablemaker.persistence.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.glygen.tablemaker.persistence.dataset.Publication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

public class PublicationRepositoryCustomImpl implements PublicationRepositoryCustom {
	
	@PersistenceContext
    private EntityManager entityManager;

	@Override
	public Page<Publication> listPublications(String datasetIdentifier, String globalFilter, Pageable pageable) {
		String baseQuery = "SELECT p FROM DatasetVersion dv JOIN dv.publications p WHERE dv.dataset.datasetIdentifier = :datasetId AND dv.head = true";
		String countQuery = "SELECT count(distinct p) FROM DatasetVersion dv JOIN dv.publications p WHERE dv.dataset.datasetIdentifier = :datasetId AND dv.head = true";
		
		StringBuilder whereClause = new StringBuilder("");
        Map<String, Object> params = new HashMap<>();
        
        params.put("datasetId", datasetIdentifier);
        
        String orderBy = "";
        // Apply sorting
        if (pageable.getSort().isSorted()) {
            for (Sort.Order order : pageable.getSort()) {
                orderBy = " ORDER BY p." + order.getProperty() + " " + (order.isAscending() ? "ASC" : "DESC"); 
            }
        }
        
        if (globalFilter != null) {
        	whereClause.append(" AND (");
            whereClause.append(" LOWER(p.title) LIKE '%" + globalFilter.toLowerCase() + "%' OR ");
            whereClause.append(" LOWER(p.authors) LIKE '%" + globalFilter.toLowerCase() + "%' OR ");
            whereClause.append(" LOWER(p.journal) LIKE '%" + globalFilter.toLowerCase() + "%' OR ");
            whereClause.append(" CAST(p.pubmedId AS string) LIKE '%" + globalFilter.toLowerCase() + "%')");
            
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
		List<Publication> publications = rawResults.stream()
		    .map(row -> (Publication) row[0])
		    .distinct() 
		    .collect(Collectors.toList());

        return new PageImpl<>(publications, pageable, count.getSingleResult());
	}
	
	@Override
	public Page<Publication> listPublications(Long versionId, String globalFilter, Pageable pageable) {
		String baseQuery = "SELECT p FROM DatasetVersion dv JOIN dv.publications p WHERE dv.versionId = :versionId";
		String countQuery = "SELECT count(distinct p) FROM DatasetVersion dv JOIN dv.publications p WHERE dv.versionId = :versionId";
		
		StringBuilder whereClause = new StringBuilder("");
        Map<String, Object> params = new HashMap<>();
        
        params.put("versionId", versionId);
        
        String orderBy = "";
        // Apply sorting
        if (pageable.getSort().isSorted()) {
            for (Sort.Order order : pageable.getSort()) {
                orderBy = " ORDER BY p." + order.getProperty() + " " + (order.isAscending() ? "ASC" : "DESC"); 
            }
        }
        
        if (globalFilter != null) {
        	whereClause.append(" AND (");
            whereClause.append(" LOWER(p.title) LIKE '%" + globalFilter.toLowerCase() + "%' OR ");
            whereClause.append(" LOWER(p.authors) LIKE '%" + globalFilter.toLowerCase() + "%' OR ");
            whereClause.append(" LOWER(p.journal) LIKE '%" + globalFilter.toLowerCase() + "%' OR ");
            whereClause.append(" CAST(p.pubmedId AS string) LIKE '%" + globalFilter.toLowerCase() + "%')");
            
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
		List<Publication> publications = rawResults.stream()
		    .map(row -> (Publication) row[0])
		    .distinct() 
		    .collect(Collectors.toList());

        return new PageImpl<>(publications, pageable, count.getSingleResult());
	}

}
