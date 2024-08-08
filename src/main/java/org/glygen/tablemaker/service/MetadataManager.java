package org.glygen.tablemaker.service;

import java.util.List;

import org.glygen.tablemaker.persistence.glycan.Datatype;
import org.glygen.tablemaker.persistence.glycan.DatatypeCategory;
import org.glygen.tablemaker.persistence.glycan.Metadata;

public interface MetadataManager {
	void deleteDatatypeCategory (DatatypeCategory cat);
	void deleteDatatype(Datatype dat);
	List<Metadata> getMetadata(Datatype dat);
	Datatype addDatatypeToCategory(Datatype d, DatatypeCategory cat, Boolean mandatory);

}
