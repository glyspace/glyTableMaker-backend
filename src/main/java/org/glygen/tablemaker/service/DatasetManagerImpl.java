package org.glygen.tablemaker.service;

import java.util.Random;

import org.glygen.tablemaker.persistence.dao.DatabaseResourceRepository;
import org.glygen.tablemaker.persistence.dao.DatasetRepository;
import org.glygen.tablemaker.persistence.dao.GrantRepository;
import org.glygen.tablemaker.persistence.dao.PublicationRepository;
import org.glygen.tablemaker.persistence.dataset.DatabaseResource;
import org.glygen.tablemaker.persistence.dataset.DatabaseResourceDataset;
import org.glygen.tablemaker.persistence.dataset.Dataset;
import org.glygen.tablemaker.persistence.dataset.DatasetVersion;
import org.glygen.tablemaker.persistence.dataset.Grant;
import org.glygen.tablemaker.persistence.dataset.Publication;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;

@Transactional
@Service
public class DatasetManagerImpl implements DatasetManager {
	
	Random random = new Random();
	
	final private DatasetRepository datasetRepository;
	final private PublicationRepository publicationRepository;
	final private GrantRepository grantRepository;
	final private DatabaseResourceRepository dataResourceRepository;
	
	public DatasetManagerImpl(DatasetRepository datasetRepository, PublicationRepository publicationRepository, GrantRepository grantRepository, DatabaseResourceRepository dataResourceRepository) {
		this.datasetRepository = datasetRepository;
		this.publicationRepository = publicationRepository;
		this.grantRepository = grantRepository;
		this.dataResourceRepository = dataResourceRepository;
	}

	@Override
	public Dataset saveDataset(Dataset d) {
		
		
		boolean protein = false;
		for (DatasetVersion version: d.getVersions()) {
			if (version.getPublications() != null) {
				for (Publication p: version.getPublications()) {
					if (p.getId() == null) {
						Publication saved = publicationRepository.save(p);
						p.setId(saved.getId());
					}
				}
			}
			if (version.getGlycoproteinData() != null && !version.getGlycoproteinData().isEmpty()) {
				protein = true;
			}
		}
		
		if (d.getDatasetIdentifier() == null) {
			String identifier = generateUniqueIdentifier (protein);
			d.setDatasetIdentifier(identifier);
		}
		
		if (d.getAssociatedPapers() != null) {
			for (Publication p: d.getAssociatedPapers()) {
				if (p.getId() == null) {
					Publication saved = publicationRepository.save(p);
					p.setId(saved.getId());
				}
			}
		}
		
		if (d.getGrants() != null) {
			for (Grant g: d.getGrants()) {
				if (g.getId() == null) {
					Grant saved = grantRepository.save(g);
					g.setId(saved.getId());
				}
			}
		}
		
		if (d.getAssociatedDatasources() != null) {
			for (DatabaseResource dr: d.getAssociatedDatasources()) {
				if (dr.getId() == null) {
					DatabaseResource saved = dataResourceRepository.save(dr);
					dr.setId(saved.getId());
				}
				
			}
		}
		
		if (d.getIntegratedIn() != null) {
			for (DatabaseResourceDataset drd : d.getIntegratedIn()) {
				if (drd.getResource().getId() == null) {
					drd.setDataset(d);
					DatabaseResource saved = dataResourceRepository.save (drd.getResource());
					drd.getResource().setId(saved.getId());
				}
			}
		}
		
		return datasetRepository.save(d);
	}
	
	private String generateUniqueIdentifier (boolean protein) {
		boolean unique = false;
		String identifier = null;
		String prefix = "";
		do {
			if (protein) {
				prefix = "TP";
			} else {
				prefix = "TG";
			}
			identifier = prefix + (1000000 + random.nextInt(9999999));
			long resultSize = datasetRepository.countByDatasetIdentifier(identifier);
			unique = resultSize == 0;
		} while (!unique);
		
		return identifier;
	}

}
