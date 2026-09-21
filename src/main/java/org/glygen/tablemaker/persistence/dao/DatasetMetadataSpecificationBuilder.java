package org.glygen.tablemaker.persistence.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.glygen.tablemaker.persistence.dataset.DatasetGlycoproteinMetadataRecord;
import org.glygen.tablemaker.persistence.dataset.DatasetMetadataRecord;
import org.springframework.stereotype.Component;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Root;

@Component
public class DatasetMetadataSpecificationBuilder 
        extends AbstractMetadataSpecificationBuilder<DatasetMetadataRecord> {

    private static final Map<String, String> NATIVE_FIELD_PATHS = Map.of(
        "glytoucanId", "glytoucanId",
        "sampleType", "metadataGroup.sampleType"
    );

    @Override
    protected Map<String, String> nativeFieldPaths() { return NATIVE_FIELD_PATHS; }

	@Override
	protected Map<String, List<String>> nestedSortPaths() {
		return new HashMap<>();
	}
	
	@Override
    protected List<Order> defaultSortOrders(Root<DatasetMetadataRecord> root, CriteriaBuilder cb) {
        return List.of(
            cb.asc(root.get("glytoucanId")),
            cb.asc(root.get("id"))
        );
    }
}