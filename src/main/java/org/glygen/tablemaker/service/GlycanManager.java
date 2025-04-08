package org.glygen.tablemaker.service;

import java.util.Collection;
import java.util.List;

import org.glygen.tablemaker.persistence.BatchUploadEntity;
import org.glygen.tablemaker.persistence.UserEntity;
import org.glygen.tablemaker.persistence.glycan.Glycan;
import org.glygen.tablemaker.persistence.glycan.GlycanTag;
import org.glygen.tablemaker.persistence.protein.Glycoprotein;

public interface GlycanManager {
	void addTagToGlycans (Collection<Glycan> glycans, String tag, UserEntity user);
	List<GlycanTag> getTags(UserEntity user);
	Glycan addUploadToGlycan(Glycan glycan, BatchUploadEntity upload, Boolean isNew, UserEntity user);
	void setGlycanTags (Glycan glycan, List<String> tags, UserEntity user);
	void setGlycoproteinTags (Glycoprotein glycoprotein, List<String> tags, UserEntity user);
	void deleteBatchUpload(BatchUploadEntity upload);
	Glycoprotein saveGlycoProtein(Glycoprotein p);
	void addTagToGlycoproteins(Collection<Glycoprotein> allGlycoproteins, String tag, UserEntity user);
	Glycoprotein addUploadToGlycoprotein(Glycoprotein gp, BatchUploadEntity upload, Boolean isNew, UserEntity user);
}
