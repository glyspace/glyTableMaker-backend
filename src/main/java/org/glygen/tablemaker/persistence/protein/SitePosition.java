package org.glygen.tablemaker.persistence.protein;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SitePosition {
	
	List<Position> positionList;
	
	public List<Position> getPositionList() {
		return positionList;
	}
	public void setPositionList(List<Position> positionList) {
		this.positionList = positionList;
	}
	
	@Override
	public String toString() {
		ObjectMapper om = new ObjectMapper();
		try {
			return om.writeValueAsString(this);
		} catch (JsonProcessingException e) {
			return super.toString();
		}
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof SitePosition) {
			if (((SitePosition) obj).getPositionList() != null && positionList != null && 
					((SitePosition) obj).getPositionList().size() == positionList.size()) {
				boolean allFound = true;
				for (Position p: positionList) {
					if (!((SitePosition) obj).getPositionList().contains(p))
						allFound = false;
				}
				return allFound;
			}
		}
		return super.equals(obj);
	}
}
