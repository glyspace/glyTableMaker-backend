package org.glygen.tablemaker.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ResidueUtil {

    private static final Pattern DELIMITER_PATTERN = Pattern.compile("[-|]");

    public static String buildResidue(String aminoAcid, String site, boolean pad) {
        if (isBlank(aminoAcid) || isBlank(site)) {
            return safe(aminoAcid) + safe(site);
        }

        boolean hasDelimiter = DELIMITER_PATTERN.matcher(aminoAcid).find()
                || DELIMITER_PATTERN.matcher(site).find();

        if (!hasDelimiter) {
        	if (pad)
        		return aminoAcid + padSite(site);
        	else
        		return aminoAcid + site;
        }

        Matcher aaMatch = DELIMITER_PATTERN.matcher(aminoAcid);
        String aaDelim = aaMatch.find() ? aaMatch.group() : null;
        Matcher siteMatch = DELIMITER_PATTERN.matcher(site);
        String siteDelim = siteMatch.find() ? siteMatch.group() : null;
        String delimiter = aaDelim != null ? aaDelim : siteDelim;

        String[] aaParts = DELIMITER_PATTERN.split(aminoAcid);
        String[] siteParts = DELIMITER_PATTERN.split(site);

        if (aaParts.length != siteParts.length) {
        	if (pad)
        		return aminoAcid + padSite(site);
        	else
        		return aminoAcid + site;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < aaParts.length; i++) {
            if (i > 0) sb.append(delimiter);
            if (pad) {
            	sb.append(aaParts[i].trim()).append(padSite(siteParts[i].trim()));
            } else {
            	sb.append(aaParts[i].trim()).append(siteParts[i].trim());
            }
        }
        return sb.toString();
    }

    private static String padSite(String sitePart) {
        if (sitePart == null || sitePart.isEmpty()) return "";
        try {
            int num = Integer.parseInt(sitePart.trim());
            return String.format("%06d", num);
        } catch (NumberFormatException e) {
            return sitePart;
        }
    }

    private static boolean isBlank(String s) { return s == null || s.isEmpty(); }
    private static String safe(String s) { return s == null ? "" : s; }
}
