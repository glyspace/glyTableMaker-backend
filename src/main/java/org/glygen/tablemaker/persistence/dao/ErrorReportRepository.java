package org.glygen.tablemaker.persistence.dao;

import java.util.Date;
import java.util.List;

import org.glygen.tablemaker.persistence.ErrorReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ErrorReportRepository extends JpaRepository<ErrorReportEntity, String> {

	@Query("SELECT e FROM ErrorReportEntity e WHERE e.message = :message AND FUNCTION('DATE', e.dateReported) = FUNCTION('DATE', :reportedDate)")
	List<ErrorReportEntity> findByMessageAndDateReported(@Param("message") String message, @Param("reportedDate") Date reportedDate);

	//List<ErrorReportEntity> findByMessageAndDateReported (String message, Date reportedDate);
}
