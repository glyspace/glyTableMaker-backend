package org.glygen.tablemaker.persistence.dao;

import java.util.List;

import org.glygen.tablemaker.persistence.TransferRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferRequestRepository extends JpaRepository<TransferRequest, Long> {
	List<TransferRequest> findByDatasetHash (String hash);
	List<TransferRequest> findByDatasetIdentifier(String identifier);
}
