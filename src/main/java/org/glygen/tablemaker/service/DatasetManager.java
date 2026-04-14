package org.glygen.tablemaker.service;

import org.glygen.tablemaker.persistence.TransferRequest;
import org.glygen.tablemaker.persistence.UserEntity;
import org.glygen.tablemaker.persistence.dataset.Dataset;

public interface DatasetManager {
	
	Dataset saveDataset (Dataset d);
	void transferDataset(Dataset d, UserEntity recipient, TransferRequest r);
}
