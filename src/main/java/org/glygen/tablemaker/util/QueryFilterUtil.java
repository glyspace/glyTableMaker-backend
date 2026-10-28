package org.glygen.tablemaker.util;

public class QueryFilterUtil {
	public static String contains(String s) {
        String escaped = s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
