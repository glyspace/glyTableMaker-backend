package org.glygen.tablemaker.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.glyspace.glytablemaker.composition.CompositionFormat;
import org.glyspace.glytablemaker.composition.ResidueType;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


public class CompositionFormatValidator {

    /**
     * Validates the definition and returns it normalized: texts and order entries trimmed, delimiters null when
     * empty, closing delimiter dropped when there is no opening one. Messages are added to errors; the returned
     * object (null when the definition cannot even be read) is only to be stored when errors is empty.
     * An unknown residue (not a ResidueType constant) makes the definition unreadable.
     */
    public static CompositionFormat validateDefinition(JsonNode definition, List<String> errors, ObjectMapper mapper) {
        if (definition == null || !definition.isObject()) {
            errors.add("Definition is required");
            return null;
        }

        CompositionFormat parsed;
        try {
            parsed = mapper.treeToValue(definition, CompositionFormat.class);
        } catch (JsonProcessingException e) {
            errors.add("Definition is not valid (unknown residue or wrong structure)");
            return null;
        }

        // residue texts: not blank after trimming, each text and each residue used once
        Map<String, ResidueType> texts = new LinkedHashMap<>();
        if (parsed.getStringToResidue() != null) {
            for (Map.Entry<String, ResidueType> e : parsed.getStringToResidue().entrySet()) {
                String text = e.getKey() == null ? "" : e.getKey().trim();
                if (text.isEmpty() || e.getValue() == null) {
                    errors.add("A residue text cannot be blank");
                } else if (texts.put(text, e.getValue()) != null) {
                    errors.add("The text \"" + text + "\" is used for more than one residue");
                }
            }
        }
        if (texts.isEmpty()) {
            errors.add("At least one residue must have a text");
        }
        if (new HashSet<>(texts.values()).size() != texts.size()) {
            errors.add("A residue has more than one text");
        }
        parsed.setStringToResidue(texts);

        // order: null (no order) or exactly the defined texts, each once
        List<String> order = parsed.getAllowedOrder();
        if (order != null) {
            List<String> trimmed = new ArrayList<>();
            for (String t : order) {
                trimmed.add(t == null ? "" : t.trim());
            }
            if (trimmed.size() != new HashSet<>(trimmed).size()) {
                errors.add("The order lists a text more than once");
            }
            if (!new HashSet<>(trimmed).equals(texts.keySet())) {
                errors.add("The order must list every defined residue text, and nothing else");
            }
            parsed.setAllowedOrder(trimmed);
        }

        // delimiters: no whitespace; empty means none; a closing delimiter needs an opening one
        String left = emptyToNull(parsed.getOpeningDelimiter());
        String right = emptyToNull(parsed.getClosingDelimiter());
        if (left != null && containsWhitespace(left)) {
            errors.add("The opening character cannot contain spaces");
        }
        if (right != null && containsWhitespace(right)) {
            errors.add("The closing character cannot contain spaces");
        }
        if (left == null) {
            right = null;
        }
        parsed.setOpeningDelimiter(left);
        parsed.setClosingDelimiter(right);
        return parsed;
    }

    private static String emptyToNull(String s) {
        return s == null || s.isEmpty() ? null : s;
    }

    private static boolean containsWhitespace(String s) {
        return s.codePoints().anyMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c));
    }
}
