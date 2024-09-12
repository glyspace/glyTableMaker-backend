package org.glygen.tablemaker.service;

import java.util.Random;

import org.glygen.tablemaker.persistence.dao.DatasetRepository;
import org.glygen.tablemaker.persistence.dataset.Dataset;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;

@Transactional
@Service
public class DatasetManagerImpl implements DatasetManager {
	
	Random random = new Random();
	
	final private DatasetRepository datasetRepository;
	
	public DatasetManagerImpl(DatasetRepository datasetRepository) {
		this.datasetRepository = datasetRepository;
	}

	@Override
	public Dataset saveDataset(Dataset d) {
		String identifier = generateUniqueIdentifier ();
		d.setDatasetIdentifier(identifier);
		
		// TODO need to save version/publication etc separately first???
		return datasetRepository.save(d);
	}
	
	private String generateUniqueIdentifier () {
		boolean unique = false;
		String identifier = null;
		do {
			identifier = "TD" + (1000000 + random.nextInt(9999999));
			long resultSize = datasetRepository.countByDatasetIdentifier(identifier);
			unique = resultSize == 0;
		} while (!unique);
		
		return identifier;
	}

}
