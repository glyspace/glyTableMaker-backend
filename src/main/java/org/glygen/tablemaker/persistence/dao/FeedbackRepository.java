package org.glygen.tablemaker.persistence.dao;

import org.glygen.tablemaker.persistence.FeedbackEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepository extends JpaRepository<FeedbackEntity, Long> {

}
