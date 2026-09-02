package org.glygen.tablemaker.persistence.dao;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.glygen.tablemaker.persistence.dataset.DatasetGlycoproteinMetadataRecord;
import org.glygen.tablemaker.view.Filter;
import org.glygen.tablemaker.view.Sorting;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Component;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@Component
public class GlycoproteinMetadataSpecificationBuilder extends AbstractMetadataSpecificationBuilder<DatasetGlycoproteinMetadataRecord>{

    // Fields that live directly on the entity (or a simple dotted path to a related entity)
    private static final Map<String, String> NATIVE_FIELD_PATHS = Map.of(
        "uniProtId", "uniProtId",
        "glytoucanId", "glytoucanId",
        "aminoAcid", "aminoAcid",
        "site", "site",
        "glycosylationType", "glycosylationType",
        "glycosylationSubType", "glycosylationSubType",
        "sampleType", "metadataGroup.sampleType"
    );
    
    private static final Map<String, List<String>> NESTED_SORT_PATHS = Map.of(
            "expressionSystem", List.of("expressionSystem", "speciesExpression", "name"),
            "analyzedProteinMutation", List.of("analyzedProteinMutation", "molecularPhenotype"),
            "geneticBackgroundAlteration", List.of("geneticBackgroundAlteration", "gene")
    );
    
    @Override
    protected Map<String, String> nativeFieldPaths() { return NATIVE_FIELD_PATHS; }
    
    @Override
    protected Map<String, List<String>> nestedSortPaths() { return NESTED_SORT_PATHS; }

    @Override
    protected List<Order> resolveSortOrders(Root<DatasetGlycoproteinMetadataRecord> root, CriteriaBuilder cb, Sorting s) {
        if (s.getId().equalsIgnoreCase("residue")) {
            Direction dir = s.getDesc() ? Direction.DESC : Direction.ASC;
            return List.of(
                dir == Direction.DESC ? cb.desc(root.get("site")) : cb.asc(root.get("site")),
                dir == Direction.DESC ? cb.desc(root.get("aminoAcid")) : cb.asc(root.get("aminoAcid"))
            );
        }
        if (s.getId().equalsIgnoreCase("glycosylationType")) {
        	Direction dir = s.getDesc() ? Direction.DESC : Direction.ASC;
            return List.of(
                dir == Direction.DESC ? cb.desc(root.get("glycosylationType")) : cb.asc(root.get("glycosylationType")),
                dir == Direction.DESC ? cb.desc(root.get("glycosylationSubType")) : cb.asc(root.get("glycosylationSubType"))
            );
        }
        return super.resolveSortOrders(root, cb, s);
    }
    
    @Override
    protected Predicate buildFilterPredicate(Root<DatasetGlycoproteinMetadataRecord> root, CriteriaBuilder cb,
    		Filter f) {
    	String fieldId = f.getId();
        String term = "%" + f.getValue().toLowerCase() + "%";
        
    	if (fieldId.equalsIgnoreCase("residue")) {
            Expression<String> aminoAcid = cb.lower(cb.coalesce(root.get("aminoAcid"), ""));
            Expression<String> site = cb.lower(cb.coalesce(root.get("site"), ""));
            return cb.or(
                cb.like(aminoAcid, term),
                cb.like(site, term)
            );
        }

        if (fieldId.equalsIgnoreCase("glycosylationType")) {
            Expression<String> type = cb.lower(cb.coalesce(root.get("glycosylationType"), ""));
            Expression<String> subType = cb.lower(cb.coalesce(root.get("glycosylationSubType"), ""));
            return cb.or(
                cb.like(type, term),
                cb.like(subType, term)
            );
        }
    	return super.buildFilterPredicate(root, cb, f);
    }
    
    @Override
    protected List<Order> defaultSortOrders(Root<DatasetGlycoproteinMetadataRecord> root, CriteriaBuilder cb) {
        return List.of(
            cb.asc(root.get("uniProtId")),
            cb.asc(root.get("glytoucanId")),
            cb.asc(root.get("site")),
            cb.asc(root.get("aminoAcid")),
            cb.asc(root.get("id"))
        );
    }
}