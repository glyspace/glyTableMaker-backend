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
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof NamespaceEntry) {
			if (label.equalsIgnoreCase(((NamespaceEntry)obj).label)) {
				return uri != null ? uri.equalsIgnoreCase(((NamespaceEntry)obj).uri) : true;
			}
			return false;
		}
		return super.equals(obj);
	}
}
