package org.glygen.tablemaker.persistence.dao;

import java.util.List;

import org.glygen.tablemaker.view.Filter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DatasetRepositoryCustom {
	Page<String> getDataByVersion(Long versionId, String globalFilter, List<Filter> filterList, Pageable pageable);
	Page<String> getData(String datasetId, String globalFilter, List<Filter> filterList, Pageable pageable);
	Page<String> getGlycoproteinData(String datasetId, String globalFilter, List<Filter> filterList, Pageable pageable);
	Page<String> getGlycoproteinDataByVersion(Long versionId, String globalFilter, List<Filter> filterList, Pageable pageable);
}
