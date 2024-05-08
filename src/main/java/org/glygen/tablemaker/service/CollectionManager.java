package org.glygen.tablemaker.service;

import org.glygen.tablemaker.persistence.glycan.Collection;

public interface CollectionManager {
	Collection saveCollectionWithMetadata (Collection c);
}
