package org.glygen.tablemaker.view;

public class NamespaceEntry {
	String label;
	String uri;    // last part of the uri is the id, either ../../<id> or ../../id=<id>
	
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	public String getUri() {
		return uri;
	}
	public void setUri(String uri) {
		this.uri = uri;
	}
}
