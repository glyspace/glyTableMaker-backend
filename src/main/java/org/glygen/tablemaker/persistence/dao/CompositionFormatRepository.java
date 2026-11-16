package org.glygen.tablemaker.persistence.dao;

import java.util.List;
import java.util.Optional;

import org.glygen.tablemaker.persistence.UserEntity;
import org.glygen.tablemaker.persistence.glycan.CompositionFormatDescriptor;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompositionFormatRepository extends JpaRepository<CompositionFormatDescriptor, Long> {
	List<CompositionFormatDescriptor> findByNameEqualsIgnoreCaseAndSystem (String name, Boolean system);  // return system defined composition formats

	List<CompositionFormatDescriptor> findBySystem(boolean system);
	List<CompositionFormatDescriptor> findByUser(UserEntity user);

	Optional<CompositionFormatDescriptor> findByIdAndUser(Long id, UserEntity user);

	List<CompositionFormatDescriptor> findByNameEqualsIgnoreCaseAndUser(String name, UserEntity user);
}
