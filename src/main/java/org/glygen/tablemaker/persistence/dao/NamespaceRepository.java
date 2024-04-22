package org.glygen.tablemaker.persistence.dao;

import org.glygen.tablemaker.persistence.glycan.Namespace;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NamespaceRepository extends JpaRepository<Namespace, Long> {
	
	public Namespace findByNameIgnoreCase(String name);

}
