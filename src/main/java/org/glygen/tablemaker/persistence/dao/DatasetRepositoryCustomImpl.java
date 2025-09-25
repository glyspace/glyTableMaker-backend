package org.glygen.tablemaker.persistence.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.glygen.tablemaker.persistence.protein.GlycoproteinColumns;
import org.glygen.tablemaker.persistence.table.GlycanColumns;
import org.glygen.tablemaker.view.Filter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

public class DatasetRepositoryCustomImpl implements DatasetRepositoryCustom {

	@PersistenceContext
    private EntityManager entityManager;

	@Override
	public Page<String> getData(String datasetId, String globalFilter, List<Filter> filterList, Pageable pageable) {
		String baseQuery = "SELECT distinct d.rowId FROM DatasetVersion dv JOIN dv.data d "
				+ "WHERE dv.dataset.datasetIdentifier = :datasetId AND dv.head = true";
		String countQuery = "SELECT count(distinct d.rowId) FROM DatasetVersion dv JOIN dv.data d "
				+ "WHERE dv.dataset.datasetIdentifier = :datasetId AND dv.head = true";
		
        Map<String, Object> params = new HashMap<>();
        params.put("datasetId", datasetId);
        
        return query (baseQuery, countQuery, globalFilter, filterList, pageable, params, false);
	}
	
	@Override
	public Page<String> getGlycoproteinData(String datasetId, String globalFilter, List<Filter> filterList, Pageable pageable) {
		String baseQuery = "SELECT distinct d.rowId FROM DatasetVersion dv JOIN dv.glycoproteinData d "
				+ "WHERE dv.dataset.datasetIdentifier = :datasetId AND dv.head = true";
		String countQuery = "SELECT count(distinct d.rowId) FROM DatasetVersion dv JOIN dv.glycoproteinData d "
				+ "WHERE dv.dataset.datasetIdentifier = :datasetId AND dv.head = true";
		
        Map<String, Object> params = new HashMap<>();
        
        params.put("datasetId", datasetId);
        
        return query (baseQuery, countQuery, globalFilter, filterList, pageable, params, true);
	}

	@Override
	public Page<String> getDataByVersion(Long versionId, String globalFilter, List<Filter> filterList,
			Pageable pageable) {
		String baseQuery = "SELECT distinct d.rowId FROM DatasetVersion dv JOIN dv.data d "
				+ "WHERE dv.versionId = :versionId";
		String countQuery = "SELECT count(distinct d.rowId) FROM DatasetVersion dv JOIN dv.data d "
				+ "WHERE dv.versionId = :datasetId";
	
        Map<String, Object> params = new HashMap<>();
        
        params.put("versionId", versionId);
               
        return query (baseQuery, countQuery, globalFilter, filterList, pageable, params, false);
	}
	
	@Override
	public Page<String> getGlycoproteinDataByVersion(Long versionId, String globalFilter, List<Filter> filterList,
			Pageable pageable) {
		String baseQuery = "SELECT distinct d.rowId FROM DatasetVersion dv JOIN dv.glycoproteinData d "
				+ "WHERE dv.versionId = :versionId";
		String countQuery = "SELECT count(distinct d.rowId) FROM DatasetVersion dv JOIN dv.glycoproteinData d "
				+ "WHERE dv.versionId = :datasetId";
	
        Map<String, Object> params = new HashMap<>();
        
        params.put("versionId", versionId);
               
        return query (baseQuery, countQuery, globalFilter, filterList, pageable, params, true);
	}
	
	private Page<String> query (String baseQuery, String countQuery, String globalFilter, List<Filter> filterList, 
			Pageable pageable, Map<String, Object> params, Boolean glycoprotein) {
		StringBuilder whereClause = new StringBuilder("");
        if (filterList != null && !filterList.isEmpty()) {
        	int i=0;
        	whereClause.append(" AND (");
        	for (Filter filter: filterList) {
        		if (filter.getId().equalsIgnoreCase(GlycanColumns.GLYTOUCANID.name())) {
        			if (glycoprotein) {
        				whereClause.append(" (d.glycoproteinColumn = 'GLYTOUCANID' AND LOWER(d.value) LIKE '%" + filter.getValue() + "%')");
        			} else {
        				whereClause.append(" (d.glycanColumn = 'GLYTOUCANID' AND LOWER(d.value) LIKE '%" + filter.getValue() + "%')");
        			}
        		} else if (filter.getId().equalsIgnoreCase(GlycoproteinColumns.UNIPROTID.name())) { 
        			whereClause.append(" (d.glycoproteinColumn = 'UNIPROTID' AND LOWER(d.value) LIKE '%" + filter.getValue() + "%')");
        		} else if (filter.getId().equalsIgnoreCase(GlycoproteinColumns.AMINOACID.name())) { 
        			whereClause.append(" (d.glycoproteinColumn = 'AMINOACID' AND LOWER(d.value) LIKE '%" + filter.getValue() + "%')");
        		} else if (filter.getId().equalsIgnoreCase(GlycoproteinColumns.SITE.name())) { 
        			whereClause.append(" (d.glycoproteinColumn = 'SITE' AND LOWER(d.value) LIKE '%" + filter.getValue() + "%')");
        		} else if (filter.getId().equalsIgnoreCase(GlycoproteinColumns.GLYCOSYLATIONTYPE.name())) { 
        			whereClause.append(" (d.glycoproteinColumn = 'GLYCOSYLATIONTYPE' AND LOWER(d.value) LIKE '%" + filter.getValue() + "%')");
        		} else if (filter.getId().equalsIgnoreCase(GlycoproteinColumns.GLYCOSYLATIONSUBTYPE.name())) { 
        			whereClause.append(" (d.glycoproteinColumn = 'GLYCOSYLATIONSUBTYPE' AND LOWER(d.value) LIKE '%" + filter.getValue() + "%')");
        		} else if (filter.getId().contains("-ID")) {
        			String datatype = filter.getId().substring(0, filter.getId().indexOf("-"));
        			whereClause.append(" (d.datatype.datatypeId = " + datatype + " AND LOWER(d.valueId) LIKE '%" + filter.getValue() + "%')");
        		} else {
        			whereClause.append(" (d.datatype.datatypeId = " + filter.getId() + " AND LOWER(d.value) LIKE '%" + filter.getValue() + "%')");
        		}
        		
        		i++;
        		if (i < filterList.size()){
        			whereClause.append(" OR ");
        		}
        	}
        	whereClause.append(")");
        }
        
        if (globalFilter != null && !globalFilter.isEmpty()) {
        	whereClause.append(" AND (");
            whereClause.append(" LOWER(d.value) LIKE '%" + globalFilter.toLowerCase() + "%' OR ");
            whereClause.append(" LOWER(d.valueUri) LIKE '%" + globalFilter.toLowerCase() + "%' OR ");
            whereClause.append(" LOWER(d.valueId) LIKE '%" + globalFilter.toLowerCase() + "%')");
        }
        
        String finalQuery = baseQuery + whereClause.toString();
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
		List<String> rows = rawResults.stream()
		    .map(row -> (String) row[0])
		    .distinct() 
		    .collect(Collectors.toList());

        return new PageImpl<>(rows, pageable, count.getSingleResult());
	}
}
