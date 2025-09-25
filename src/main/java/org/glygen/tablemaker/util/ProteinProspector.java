package org.glygen.tablemaker.util;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Picture;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.eurocarbdb.application.glycanbuilder.BuilderWorkspace;
import org.eurocarbdb.application.glycanbuilder.Glycan;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.eurocarbdb.application.glycanbuilder.renderutil.GlycanRendererAWT;
import org.eurocarbdb.application.glycanbuilder.util.GraphicOptions;
import org.glycoinfo.GlycanCompositionConverter.conversion.CompositionConverter;
import org.glycoinfo.GlycanCompositionConverter.conversion.ConversionException;
import org.glycoinfo.GlycanCompositionConverter.structure.Composition;
import org.glycoinfo.GlycanCompositionConverter.utils.CompositionParseException;
import org.glycoinfo.GlycanCompositionConverter.utils.CompositionUtils;
import org.glycoinfo.GlycanCompositionConverter.utils.DictionaryException;
import org.glycoinfo.WURCSFramework.util.WURCSException;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.WURCS2Parser;
import org.glygen.tablemaker.controller.DataController;
import org.glygen.tablemaker.controller.TableController;
import org.glygen.tablemaker.exception.GlytoucanAPIFailedException;
import org.grits.toolbox.glycanarray.om.util.ExcelWriterHelper;

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
	
	static BuilderWorkspace glycanWorkspace = new BuilderWorkspace(new GlycanRendererAWT());
    static {       
            glycanWorkspace.initData();
            // Set orientation of glycan: RL - right to left, LR - left to right, TB - top to bottom, BT - bottom to top
            glycanWorkspace.getGraphicOptions().ORIENTATION = GraphicOptions.RL;
            // Set flag to show information such as linkage positions and anomers
            glycanWorkspace.getGraphicOptions().SHOW_INFO = true;
            // Set flag to show mass
            glycanWorkspace.getGraphicOptions().SHOW_MASSES = false;
            // Set flag to show reducing end
            glycanWorkspace.getGraphicOptions().SHOW_REDEND = true;

            glycanWorkspace.setDisplay(GraphicOptions.DISPLAY_NORMALINFO);
            glycanWorkspace.setNotation(GraphicOptions.NOTATION_SNFG);
    }
    
    static  StringBuilder patternBuilder;
    static {
    	List<String> monosacchrides = new ArrayList<String>(prospectorMapping.keySet());
        Collections.sort(monosacchrides, Collections.reverseOrder());
        
        patternBuilder = new StringBuilder();
        for (String comp : monosacchrides) {
        	if (patternBuilder.length() > 0) patternBuilder.append("|");
            patternBuilder.append(comp);
        }
    }
	
	void addGlytoucanIds (String filename, GlytoucanUtil util) throws EncryptedDocumentException, IOException {
		File file1 = new File (filename);
		Workbook workbook = WorkbookFactory.create(file1);
		Sheet glycans = workbook.getSheetAt(0);
		
		// Create a cell style with a yellow background
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        
        // Create a cell style with a yellow background
        CellStyle redStyle = workbook.createCellStyle();
        redStyle.setFillForegroundColor(IndexedColors.RED.getIndex());
        redStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        
        
        Map<String, String> processed = new HashMap<>();
        Set<String> registered = new HashSet<>();
		Iterator<Row> rowIterator = glycans.iterator();
		int rowCount = 0;
		while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            rowCount++;
            if (rowCount <= 2) {
            	continue;
            }
            
            if (rowCount%100 == 0)
            	System.out.println ("Processed " + rowCount);
            Cell modCell = row.getCell(0);
            if (modCell.getCellType() == CellType.BLANK) {
            	// row is empty, exiting
            	break;
            }
            String input = modCell.getStringCellValue();
           
            try {
            	String output = parseModification (input);
	            Composition compo = CompositionUtils.parse(output);
	       
	            //BASE type
	            try {
	            	boolean highlight = false;
		            String wurcs = CompositionConverter.toWURCS(compo);
		            wurcs = DataController.toUnknownForm(wurcs);
	            	
	            	Cell glytoucanCell = row.getCell(2);
	            	Cell newGlytoucanCell = row.getCell(3);
	            	if (newGlytoucanCell == null) {
	            		newGlytoucanCell = row.createCell(3);
	            	} else if (newGlytoucanCell.getCellType() == CellType.BLANK) {  // created in a previous run, but there was no accession number
						highlight = true;
					} else {
						// already filled in, skip
						continue;
					}
    	            
    	            String glytoucan = null;
    	            if (processed.get(wurcs) == null) {
    	            	glytoucan = util.getAccessionNumber(wurcs);
    	            	processed.put(wurcs, glytoucan);
    	            } else {
    	            	glytoucan = processed.get(wurcs);
    	            }
    	            
    	            if (glytoucan != null) {
    	            	newGlytoucanCell.setCellValue(glytoucan);
    	            	if (highlight) newGlytoucanCell.setCellStyle(style);
    	            	if (glytoucanCell.getCellType() != CellType.BLANK) {
    	            		String existing = glytoucanCell.getStringCellValue();
    	            		if (existing != null && !existing.equalsIgnoreCase(glytoucan)) {
    	            			// highlight
    	            			glytoucanCell.setCellStyle(redStyle);
    	            		}
    	            	}
    	            } else {
    	            	System.out.println ("Row " + rowCount + ". There is no accession number for " + wurcs);
    	            	System.out.println ("Registering...");
    	            	// registering
    	            	if (!registered.contains(wurcs)) {
	    	            	glytoucan = util.registerGlycan(wurcs);
	    	            	registered.add(wurcs);
	    	            	if (glytoucan == null) {
	    	    	        	// error 
	    	    	        	System.err.println ("registration failed for row " + rowCount + " WURCS: " + wurcs);
	    	    	        } else if (glytoucan.length() > 10) {
	    	    	            // this is new registration, hash returned
	    	    	            System.out.println ("Registered " + wurcs + " at row " + rowCount);
	    	    	        } else {
	    	    	        	processed.put(wurcs, glytoucan);
	    	    	        	newGlytoucanCell.setCellValue(glytoucan);
	    	    	            newGlytoucanCell.setCellStyle(style);
	    	    	            if (glytoucanCell.getCellType() != CellType.BLANK) {
	        	            		String existing = glytoucanCell.getStringCellValue();
	        	            		if (existing != null && !existing.equalsIgnoreCase(glytoucan)) {
	        	            			// highlight
	        	            			glytoucanCell.setCellStyle(redStyle);
	        	            		}
	        	            	}
	    	    	        }
    	            	}
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
			} catch (IllegalArgumentException e) {
    			System.err.println ("Unmatched components at row: " + rowCount + " " + e.getMessage());
    			continue;
    		}
		}
		
		try (FileOutputStream fileOut = new FileOutputStream(filename + ".updated")) {
		    workbook.write(fileOut);
		}
		
		//workbook.write(new FileOutputStream(file1));
	}
	
	public static String parseModification (String input) throws IllegalArgumentException {
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
			throw new IllegalArgumentException(String.join(", ", unmatched));
		}
		
		return output.toString();

	}
	
	public void generateCartoonSheet (String filename) throws FileNotFoundException, IOException {
		File file1 = new File (filename);
		Workbook workbook = WorkbookFactory.create(file1);
		
		ExcelWriterHelper helper = new ExcelWriterHelper();
		List<Picture> m_lPictures = new ArrayList<>(); 
		
		List<String> monosacchrides = new ArrayList<String>(prospectorMapping.keySet());
        Collections.sort(monosacchrides, Collections.reverseOrder());
        
        StringBuilder patternBuilder = new StringBuilder();
        for (String comp : monosacchrides) {
        	if (patternBuilder.length() > 0) patternBuilder.append("|");
            patternBuilder.append(comp);
        }
		
		Sheet cartoons = workbook.createSheet("Glycans with Cartoons");
		Sheet glycans = workbook.getSheetAt(0);
		Row header = cartoons.createRow(0);
        header = cartoons.createRow(1);
    	Cell cell1 = header.createCell(0);
    	cell1.setCellValue("Modification");
    	cell1.setCellStyle(glycans.getRow(1).getCell(0).getCellStyle());
    	cell1 = header.createCell(1);
    	cell1.setCellValue("Monoisotopic Mass");
    	cell1.setCellStyle(glycans.getRow(1).getCell(1).getCellStyle());
    	cell1 = header.createCell(2);
    	cell1.setCellValue("Glytoucan ID");
    	cell1.setCellStyle(glycans.getRow(1).getCell(2).getCellStyle());
    	cell1 = header.createCell(3);
    	cell1.setCellValue("");
    	cell1 = header.createCell(4);
    	cell1.setCellValue("Cartoon");
    	cell1.setCellStyle(glycans.getRow(1).getCell(0).getCellStyle());
    	
    	Iterator<Row> rowIterator = glycans.iterator();
		int rowCount = 0;
		while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            rowCount++;
            if (rowCount <= 2) {
            	continue;
            }
            
            if (rowCount%100 == 0)
            	System.out.println ("Processed " + rowCount);
            
            Cell modCell = row.getCell(0);
            if (modCell.getCellType() == CellType.BLANK) {
            	// row is empty, exiting
            	break;
            }
            String input = modCell.getStringCellValue();
            
            Row newRow = cartoons.createRow(rowCount-1);
            for (Cell sourceCell: row) {
            	Cell newCell = newRow.createCell(sourceCell.getColumnIndex());
            	// Copy value and formula based on cell type
                switch (sourceCell.getCellType()) {
                    case STRING:
                    	newCell.setCellValue(sourceCell.getStringCellValue());
                        break;
                    case NUMERIC:
                    	newCell.setCellValue(sourceCell.getNumericCellValue());
                        break;
                    case FORMULA:
                    	newCell.setCellFormula(sourceCell.getCellFormula());
                        break;
                    case BOOLEAN:
                    	newCell.setCellValue(sourceCell.getBooleanCellValue());
                        break;
                    case ERROR:
                    	newCell.setCellValue(sourceCell.getErrorCellValue());
                        break;
                    case BLANK:
                        // No need to do anything for blank cells
                        break;
                    default:
                        break;
                }
                newCell.setCellStyle(sourceCell.getCellStyle());
            }
            //create cartoon column
            Cell cartoonCell = newRow.createCell(4);
            try {
            	String output = parseModification (input);
                Composition compo = CompositionUtils.parse(output);
	            String wurcs = CompositionConverter.toWURCS(compo);
	            wurcs = DataController.toUnknownForm(wurcs);
	            byte [] cartoon = createImage(wurcs);
	            if (cartoon != null) {
	            	InputStream is = new ByteArrayInputStream(cartoon);
			        try {
						BufferedImage newBi = ImageIO.read(is);
						//newBi = TableController.scale (newBi, 0.8);
    				    helper.writeCellImage(workbook, cartoons, newRow.getRowNum(), cartoonCell.getColumnIndex(), newBi, m_lPictures);
					} catch (Exception e) {
						System.err.println ("Could not write cartoon for row " + rowCount + " Reason:" + e.getMessage());
					}
	            }
            } catch (IllegalArgumentException e) {
            	System.err.println ("Unmatched components at row: " + rowCount + " " + e.getMessage());
    			continue;
            } catch (Exception e) {
            	System.err.println ("Error generating Wurcs for " + input + " " + e.getMessage());
            }
		}
		
		try (FileOutputStream fileOut = new FileOutputStream("ProteinProspector-withcartoons.xlsx")) {
		    workbook.write(fileOut);
		}
	}
	
	private byte[] createImage (String wurcs) {
		try {
			WURCS2Parser t_wurcsparser = new WURCS2Parser();
	        Glycan glycanObject = t_wurcsparser.readGlycan(wurcs, new MassOptions());
	    
	        if (glycanObject != null) {
	        	BufferedImage t_image = glycanWorkspace.getGlycanRenderer().getImage(glycanObject, true, false, true, 1.0D);
	        	ByteArrayOutputStream baos = new ByteArrayOutputStream();
	        	ImageIO.write(t_image, "jpg", baos);
	        	byte[] bytes = baos.toByteArray();
	        	
	        	return bytes;
	        } 
		} catch (Exception e) {
			System.err.println ("could not generate image for " + wurcs + " Reason:" + e.getMessage());
		}
        return null; 
	}
	
	public static void main(String[] args) {
		if (args.length > 0) {
			try {
				GlytoucanUtil util = GlytoucanUtil.getInstance();
				util.setApiKey("6d9fbfb1c0a52cbbffae7c113395a203ae0e3995a455c42ff3932862cbf7e62a");
		        util.setUserId("ff2dda587eb4597ab1dfb995b520e99b7ef68d7786af0f3ea626555e2c609c3d");
		        ProteinProspector p = new ProteinProspector();
				//p.addGlytoucanIds(args[0], util);
				p.generateCartoonSheet(args[0]);
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
