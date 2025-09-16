package org.glygen.tablemaker.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.common.usermodel.Hyperlink;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.eurocarbdb.MolecularFramework.sugar.Anomer;
import org.eurocarbdb.MolecularFramework.sugar.GlycoNode;
import org.eurocarbdb.MolecularFramework.sugar.GlycoconjugateException;
import org.eurocarbdb.MolecularFramework.sugar.Monosaccharide;
import org.eurocarbdb.MolecularFramework.sugar.Sugar;
import org.glycoinfo.GlycanCompositionConverter.conversion.CompositionConverter;
import org.glycoinfo.GlycanCompositionConverter.conversion.ConversionException;
import org.glycoinfo.GlycanCompositionConverter.structure.Composition;
import org.glycoinfo.GlycanCompositionConverter.utils.CompositionParseException;
import org.glycoinfo.GlycanCompositionConverter.utils.CompositionUtils;
import org.glycoinfo.GlycanCompositionConverter.utils.DictionaryException;
import org.glycoinfo.GlycanFormatconverter.util.exchange.SugarToWURCSGraph.SugarToWURCSGraph;
import org.glycoinfo.WURCSFramework.util.WURCSException;
import org.glycoinfo.WURCSFramework.util.WURCSFactory;
import org.glycoinfo.WURCSFramework.util.exchange.WURCSExchangeException;
import org.glygen.tablemaker.exception.GlytoucanAPIFailedException;
import org.hibernate.query.SortDirection;

public class ProteinProspector {
	
	static Map<String, String> prospectorMapping = new HashMap<>();
	
	static {
		// proteinprospector to WURCS composition mapping
		prospectorMapping.put("Fuc", "Fuc");
		prospectorMapping.put("Galactosyl", "Gal");
		prospectorMapping.put("galactosyl", "Gal");
		prospectorMapping.put("HexNAc", "HexNAc");
		prospectorMapping.put("Glucosyl", "Glc");
		prospectorMapping.put("Phospho", "p");
		prospectorMapping.put("Sulfo", "S");
		prospectorMapping.put("NeuAc", "Neu5Ac");
		prospectorMapping.put("NeuGc", "Neu5Gc");
		prospectorMapping.put("Hex", "Hex");
		prospectorMapping.put("Xyl", "Xyl");
		prospectorMapping.put("Ac", "Ac");
		
	}
	
	void addGlytoucanIds (String filename) throws EncryptedDocumentException, IOException {
		GlytoucanUtil util = GlytoucanUtil.getInstance();
		File file1 = new File (filename);
		Workbook workbook = WorkbookFactory.create(file1);
		Sheet glycans = workbook.getSheetAt(0);
		
		// Create a cell style with a yellow background
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        
        List<String> monosacchrides = new ArrayList<String>(prospectorMapping.keySet());
        Collections.sort(monosacchrides, Collections.reverseOrder());
        
        StringBuilder patternBuilder = new StringBuilder();
        for (String comp : monosacchrides) {
        	if (patternBuilder.length() > 0) patternBuilder.append("|");
            patternBuilder.append(comp);
        }
        
		Iterator<Row> rowIterator = glycans.iterator();
		int rowCount = 0;
		while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            rowCount++;
            if (rowCount <= 2) {
            	continue;
            }
            Cell modCell = row.getCell(0);
            if (modCell.getCellType() == CellType.BLANK) {
            	// row is empty, exiting
            	break;
            }
            String input = modCell.getStringCellValue();
            if (input.contains("+")) {
            	input = input.substring(0, input.indexOf("+"));
            }
            
            Map<String, Integer> counts = new LinkedHashMap<>();

	        // Match components followed by optional digits
			Pattern pattern = Pattern.compile("(" + patternBuilder + ")(\\d*)");
			Matcher matcher = pattern.matcher(input);
			
			// Track matched regions
			List<int[]> matchedRanges = new ArrayList<>();
			
			while (matcher.find()) {
			    String name = matcher.group(1);
			    String countStr = matcher.group(2);
			    int count = countStr.isEmpty() ? 1 : Integer.parseInt(countStr);
			    counts.put(name, counts.getOrDefault(name, 0) + count);
			    matchedRanges.add(new int[]{matcher.start(), matcher.end()});
			}

	        // Check for unmatched segments
	        List<String> unmatched = new ArrayList<>();
	        int lastEnd = 0;
	        for (int[] range : matchedRanges) {
	            if (range[0] > lastEnd) {
	                unmatched.add(input.substring(lastEnd, range[0]));
	            }
	            lastEnd = range[1];
	        }
	        if (lastEnd < input.length()) {
	            unmatched.add(input.substring(lastEnd));
	        }

			// Build output string
			StringBuilder output = new StringBuilder();
			for (Map.Entry<String, Integer> entry : counts.entrySet()) {
			    if (output.length() > 0) output.append("|");
			    String mapped = prospectorMapping.get(entry.getKey());
			    output.append(mapped).append(":").append(entry.getValue());
			}
			
			if (!unmatched.isEmpty()) {
				System.err.println ("Unmatched components at row: " + rowCount + " " + String.join(", ", unmatched));
				continue;
			}

            try {
	            Composition compo = CompositionUtils.parse(output.toString());
	            // GLYGEN Composition: Unknown anomer, unknown ringsize, but closed ring.
            	SugarToWURCSGraph t_s2w = new SugarToWURCSGraph();
    			try {
    				Sugar sugar = CompositionConverter.toSugar(compo);
    				for (GlycoNode node: sugar.getNodes()) {
    					if (node instanceof Monosaccharide) {
    						Monosaccharide m = ((Monosaccharide)node);
    						m.setRing(-1, -1);
    						m.setAnomer(Anomer.Unknown);
    					}
    				}
    				t_s2w.start(sugar);
    			} catch (WURCSExchangeException | GlycoconjugateException e) {
    				throw new ConversionException("Error in converting composition to Sugar object.", e);
    			}
    			try {
    				WURCSFactory factory = new WURCSFactory(t_s2w.getGraph());
    				String wurcs = factory.getWURCS();
    				
    				Cell glytoucanCell = row.getCell(2);
    	            Cell newGlytoucanCell = row.createCell(3);
    	            
    	            String glytoucan = util.getAccessionNumber(wurcs);
    	            if (glytoucan != null) {
    	            	newGlytoucanCell.setCellValue(glytoucan);
    	            	if (glytoucanCell.getCellType() != CellType.BLANK) {
    	            		String existing = glytoucanCell.getStringCellValue();
    	            		if (existing != null && !existing.equalsIgnoreCase(glytoucan)) {
    	            			// highlight
    	            			newGlytoucanCell.setCellStyle(style);
    	            		}
    	            	}
    	            } else {
    	            	System.out.println ("Row " + rowCount + ". There is no accession number for " + wurcs);
    	            }
    			} catch (WURCSException e) {
    				throw new ConversionException("Error in encoding the composition in WURCS.", e);
    			}
            } catch (DictionaryException e) {
				System.err.println ("Could not parse composition: " +  input + " Reason: " + e);
				e.printStackTrace();
			} catch (CompositionParseException e) {
				System.err.println ("Could not parse composition: " +  input + " Reason: " + e);
				e.printStackTrace();
			} catch (ConversionException e) {
				System.err.println ("Could not create WURCS string: " +  e);
				e.printStackTrace();
			} catch (GlytoucanAPIFailedException e) {
				System.err.println ("Glytoucan retrieval failed: " +  e);
				e.printStackTrace();
			}
		}
		
		try (FileOutputStream fileOut = new FileOutputStream(filename + ".updated")) {
		    workbook.write(fileOut);
		}
		
		//workbook.write(new FileOutputStream(file1));
	}
	
	public static void main(String[] args) {
		if (args.length > 0) {
			try {
				new ProteinProspector().addGlytoucanIds(args[0]);
			} catch (EncryptedDocumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}
