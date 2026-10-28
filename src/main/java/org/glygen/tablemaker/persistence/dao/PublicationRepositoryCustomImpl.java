package org.glygen.tablemaker.persistence.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
	
	private static final Set<String> SORTABLE = Set.of("year", "title", "authors", "pubmedId", "doiId", "journal", "volume");
	
	private Page<Publication> run(String select, String countQuery, Map<String, Object> params,
            String globalFilter, Pageable pageable) {
		StringBuilder where = new StringBuilder();
		if (globalFilter != null && !globalFilter.isBlank()) {
			where.append(" AND (LOWER(p.title) LIKE :filter ESCAPE '\\'"
				 + " OR LOWER(p.authors) LIKE :filter ESCAPE '\\'"
				 + " OR LOWER(p.journal) LIKE :filter ESCAPE '\\'"
				 + " OR CAST(p.pubmedId AS string) LIKE :filter ESCAPE '\\')");
			String escaped = globalFilter.toLowerCase().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
			params.put("filter", "%" + escaped + "%");		
		}
		
		String orderBy = "";
		if (pageable.getSort().isSorted()) {
			for (Sort.Order order : pageable.getSort()) {
				if (SORTABLE.contains(order.getProperty())) {
					orderBy = " ORDER BY p." + order.getProperty() + (order.isAscending() ? " ASC" : " DESC");
				}
			}
		}
		
		String finalQuery = select + where + orderBy;
        String finalCountQuery = countQuery + where.toString();
        
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
	public Page<Publication> listPublications(String datasetIdentifier, String globalFilter, Pageable pageable) {
		String baseQuery = "SELECT distinct p FROM DatasetVersion dv JOIN dv.publications p WHERE dv.dataset.datasetIdentifier = :datasetId AND dv.head = true";
		String countQuery = "SELECT count(distinct p) FROM DatasetVersion dv JOIN dv.publications p WHERE dv.dataset.datasetIdentifier = :datasetId AND dv.head = true";
		
		Map<String, Object> params = new HashMap<>();
		params.put("datasetId", datasetIdentifier);
		return run(baseQuery, countQuery, params, globalFilter, pageable);
	}
	
	@Override
	public Page<Publication> listPublications(Long versionId, String globalFilter, Pageable pageable) {
		String baseQuery = "SELECT distinct p FROM DatasetVersion dv JOIN dv.publications p WHERE dv.versionId = :versionId";
		String countQuery = "SELECT count(distinct p) FROM DatasetVersion dv JOIN dv.publications p WHERE dv.versionId = :versionId";
        Map<String, Object> params = new HashMap<>();
        params.put("versionId", versionId);
        
        return run(baseQuery, countQuery, params, globalFilter, pageable);
        
        
	}

}
