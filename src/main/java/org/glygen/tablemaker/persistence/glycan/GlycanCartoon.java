package org.glygen.tablemaker.persistence.glycan;

import org.glygen.tablemaker.view.ImageSettings;

public class GlycanCartoon {
	
	byte[] compactRedEnd;
	byte[] extendedRedEnd;
	byte[] compactNoRedEnd;
	byte[] extendedNoRedEnd;
	
	public byte[] getCartoon (ImageSettings setting) {
		if (setting == null) {
			return extendedRedEnd;
		}
		if (setting.isRedEnd()) {
			if (setting.getDisplay() == 0) {
				return compactRedEnd;
			} else {
				return extendedRedEnd;
			}
		} else {
			if (setting.getDisplay() == 0) {
				return compactNoRedEnd;
			} else {
				return extendedNoRedEnd;
			}
		}
	}

	public byte[] getCompactRedEnd() {
		return compactRedEnd;
	}

	public void setCompactRedEnd(byte[] compactRedEnd) {
		this.compactRedEnd = compactRedEnd;
	}

	public byte[] getExtendedRedEnd() {
		return extendedRedEnd;
	}

	public void setExtendedRedEnd(byte[] extendedRedEnd) {
		this.extendedRedEnd = extendedRedEnd;
	}

	public byte[] getCompactNoRedEnd() {
		return compactNoRedEnd;
	}

	public void setCompactNoRedEnd(byte[] compactNoRedEnd) {
		this.compactNoRedEnd = compactNoRedEnd;
	}

	public byte[] getExtendedNoRedEnd() {
		return extendedNoRedEnd;
	}

	public void setExtendedNoRedEnd(byte[] extendedNoRedEnd) {
		this.extendedNoRedEnd = extendedNoRedEnd;
	}
	
	

}
