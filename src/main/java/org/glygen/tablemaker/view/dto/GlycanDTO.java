package org.glygen.tablemaker.view.dto;

import java.util.Date;
import java.util.List;

import org.glygen.tablemaker.persistence.glycan.GlycanTag;

public class GlycanDTO {
	String glytoucanID;
    String wurcs;
    String glycoCT;
    String gws;
    String glytoucanHash;
    Double mass;
    Date dateCreated;
    Date dateAdded;    // date added to the collection
    List<GlycanTag> tags;
	public String getGlytoucanID() {
		return glytoucanID;
	}
	public void setGlytoucanID(String glytoucanID) {
		this.glytoucanID = glytoucanID;
	}
	public String getWurcs() {
		return wurcs;
	}
	public void setWurcs(String wurcs) {
		this.wurcs = wurcs;
	}
	public String getGlycoCT() {
		return glycoCT;
	}
	public void setGlycoCT(String glycoCT) {
		this.glycoCT = glycoCT;
	}
	public String getGws() {
		return gws;
	}
	public void setGws(String gws) {
		this.gws = gws;
	}
	public String getGlytoucanHash() {
		return glytoucanHash;
	}
	public void setGlytoucanHash(String glytoucanHash) {
		this.glytoucanHash = glytoucanHash;
	}
	public Double getMass() {
		return mass;
	}
	public void setMass(Double mass) {
		this.mass = mass;
	}
	public Date getDateCreated() {
		return dateCreated;
	}
	public void setDateCreated(Date dateCreated) {
		this.dateCreated = dateCreated;
	}
	public Date getDateAdded() {
		return dateAdded;
	}
	public void setDateAdded(Date dateAdded) {
		this.dateAdded = dateAdded;
	}
	public List<GlycanTag> getTags() {
		return tags;
	}
	public void setTags(List<GlycanTag> tags) {
		this.tags = tags;
	}
}
