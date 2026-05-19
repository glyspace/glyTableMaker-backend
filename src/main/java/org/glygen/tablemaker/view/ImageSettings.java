package org.glygen.tablemaker.view;

public class ImageSettings {
	int display = 2; // 0: compact, 1: normal, 2: extended
	double scale = 1.0;
	boolean redEnd = true; // show reducing end or not
	
	public int getDisplay() {
		return display;
	}
	public void setDisplay(int display) {
		this.display = display;
	}
	public double getScale() {
		return scale;
	}
	public void setScale(double scale) {
		this.scale = scale;
	}
	public boolean isRedEnd() {
		return redEnd;
	}
	public void setRedEnd(boolean redEnd) {
		this.redEnd = redEnd;
	}

}
