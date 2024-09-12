package org.glygen.tablemaker.persistence.dao;

import org.glygen.tablemaker.persistence.dataset.Publication;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PublicationRepository extends JpaRepository<Publication, Long> {

}
