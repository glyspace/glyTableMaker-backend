package org.glygen.tablemaker.view;

public class GlymageRequest {
    private String seq;
    private String notation = "snfg";
    private String display;
    private String redend;
    private String orientation = "RL";
    private String opaque = "n";
    private String imageFormat = "png";

    private double scale=1.0;

	public String getSeq() {
		return seq;
	}

	public void setSeq(String seq) {
		this.seq = seq;
	}

	public String getNotation() {
		return notation;
	}

	public void setNotation(String notation) {
		this.notation = notation;
	}

	public String getDisplay() {
		return display;
	}

	public void setDisplay(String display) {
		this.display = display;
	}

	public String getRedend() {
		return redend;
	}

	public void setRedend(String redend) {
		this.redend = redend;
	}

	public String getOrientation() {
		return orientation;
	}

	public void setOrientation(String orientation) {
		this.orientation = orientation;
	}

	public String getOpaque() {
		return opaque;
	}

	public void setOpaque(String opaque) {
		this.opaque = opaque;
	}

	public String getImageFormat() {
		return imageFormat;
	}

	public void setImageFormat(String imageFormat) {
		this.imageFormat = imageFormat;
	}

	public double getScale() {
		return scale;
	}

	public void setScale(double scale) {
		this.scale = scale;
	}

    
}