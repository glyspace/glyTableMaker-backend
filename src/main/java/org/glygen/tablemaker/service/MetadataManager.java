package org.glygen.tablemaker.service;

import org.glygen.tablemaker.persistence.glycan.Datatype;
import org.glygen.tablemaker.persistence.glycan.DatatypeCategory;

public interface MetadataManager {
	
	Datatype addDatatypeToCategory (Datatype d, DatatypeCategory cat);

}
