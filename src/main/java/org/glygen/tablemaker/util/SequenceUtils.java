package org.glygen.tablemaker.util;

import java.util.HashMap;
import java.util.Map;

import org.eurocarbdb.MolecularFramework.io.SugarImporterException;
import org.eurocarbdb.MolecularFramework.sugar.Sugar;
import org.eurocarbdb.MolecularFramework.util.visitor.GlycoVisitorException;
import org.eurocarbdb.application.glycanbuilder.ResidueType;
import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;
import org.eurocarbdb.application.glycanbuilder.massutil.IonCloud;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.glycoinfo.GlycanCompositionConverter.structure.Composition;
import org.glycoinfo.GlycanCompositionConverter.utils.CompositionParseException;
import org.glycoinfo.GlycanCompositionConverter.utils.CompositionUtils;
import org.glycoinfo.GlycanCompositionConverter.utils.DictionaryException;
import org.glycoinfo.WURCSFramework.io.GlycoCT.GlycoVisitorValidationForWURCS;
import org.glycoinfo.WURCSFramework.io.GlycoCT.WURCSExporterGlycoCT;
import org.glycoinfo.WURCSFramework.util.WURCSException;
import org.glycoinfo.WURCSFramework.util.validation.WURCSValidator;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.WURCS2Parser;
import org.glygen.tablemaker.exception.DataNotFoundException;
import org.glygen.tablemaker.exception.GlytoucanFailedException;
import org.glygen.tablemaker.persistence.glycan.Glycan;
import org.glygen.tablemaker.persistence.glycan.RegistrationStatus;
import org.slf4j.LoggerFactory;

public class SequenceUtils {
	static org.slf4j.Logger logger = LoggerFactory.getLogger(SequenceUtils.class);
	
	static Map<String, String> singleLetterMapping = new HashMap<>();
	
	static {
		singleLetterMapping.put("P", "P");
		singleLetterMapping.put("X", "Pen");
		singleLetterMapping.put("F", "Fuc");
		singleLetterMapping.put("H", "Hex");
		singleLetterMapping.put("HexA", "HexA");
		singleLetterMapping.put("GlcA", "GlcA");
		singleLetterMapping.put("N", "HexNAc");
		singleLetterMapping.put("G", "Neu5Gc");
		singleLetterMapping.put("S", "Neu5Ac");
		singleLetterMapping.put("Kdn", "Kdn");
		singleLetterMapping.put("Sl", "NeuAcLac");
		
		/*singleLetterMapping.put("Sa", "Fuc");
		singleLetterMapping.put("Sm", "Fuc");
		singleLetterMapping.put("Ga", "Fuc");
		singleLetterMapping.put("Sd", "Fuc");
		singleLetterMapping.put("Se", "Fuc");
		singleLetterMapping.put("Gd", "Fuc");
		singleLetterMapping.put("Ge", "Fuc");
		singleLetterMapping.put("HexAe", "Fuc");*/
	}
	
/**	
	Ethyl esterified Hexuronic acid	HexAe
	lactonized n-acetylneuraminic acid	Sl
	ammonia amidated n-acetylneuraminic acid	Sa
	methyl esterified n-acetylneuraminic acid	Sm
	ammonia amidated n-glycolylneuraminic acid	Ga
	dimethylamidated n-acetylneuraminic acid	Sd
	ethyl esterified n-acetylneuraminic acid	Se
	dimethylamidated n-glycolylneuraminic acid	Gd
	ethyl esterified n-glycolylneuraminic acid	Ge
**/
	
	public static void registerGlycan (Glycan glycan) {
    	// if not, register
        // store the hash and update status 
		try {
	        String glyToucanId = GlytoucanUtil.getInstance().registerGlycan(glycan.getWurcs());
	        logger.info("Got glytoucan id after registering the glycan: " + glyToucanId);
	    
	        if (glyToucanId == null) {
	        	// error 
	        	glycan.setStatus(RegistrationStatus.ERROR);
	        	glycan.setError("Internal Server Error!");
	        } else if (glyToucanId.length() > 10) {
	            // this is new registration, hash returned
	            String glyToucanHash = glyToucanId;
	            glycan.setGlytoucanHash(glyToucanHash);
	            logger.info("got glytoucan hash, no accession number!");
	            glycan.setStatus(RegistrationStatus.NEWLY_SUBMITTED_FOR_REGISTRATION);
	        } else {
	            glycan.setGlytoucanID(glyToucanId);
	            glycan.setStatus(RegistrationStatus.NEWLY_REGISTERED);
	        }
		} catch (GlytoucanFailedException e) { 
			// error
			glycan.setStatus(RegistrationStatus.ERROR);
			glycan.setError (e.getMessage());
			glycan.setErrorJson(e.getErrorJson());
		}
    }
	
	public static Double computeMassFromWurcs (String wurcs) throws Exception {
		WURCS2Parser t_wurcsparser = new WURCS2Parser();
        MassOptions massOptions = new MassOptions();
        massOptions.setDerivatization(MassOptions.NO_DERIVATIZATION);
        massOptions.setIsotope(MassOptions.ISOTOPE_MONO);
        massOptions.ION_CLOUD = new IonCloud();
        massOptions.NEUTRAL_EXCHANGES = new IonCloud();
        ResidueType m_residueFreeEnd = ResidueDictionary.findResidueType("freeEnd");
        massOptions.setReducingEndType(m_residueFreeEnd);
        org.eurocarbdb.application.glycanbuilder.Glycan glycanObject = t_wurcsparser.readGlycan(wurcs, massOptions);
        return computeMass(glycanObject);
	}
    
    public static Double computeMass (org.eurocarbdb.application.glycanbuilder.Glycan glycanObject) {
        if (glycanObject != null) {
            MassOptions massOptions = new MassOptions();
            massOptions.setDerivatization(MassOptions.NO_DERIVATIZATION);
            massOptions.setIsotope(MassOptions.ISOTOPE_MONO);
            massOptions.ION_CLOUD = new IonCloud();
            massOptions.NEUTRAL_EXCHANGES = new IonCloud();
            ResidueType m_residueFreeEnd = ResidueDictionary.findResidueType("freeEnd");
            massOptions.setReducingEndType(m_residueFreeEnd);
            glycanObject.setMassOptions(massOptions);
            return glycanObject.computeMass();
        } 
        return null;
    }
    
    public static void getWurcsAndGlytoucanID (Glycan glycan, Sugar sugar) throws GlycoVisitorException { 
        String wurcs = glycan.getWurcs();
        if (wurcs == null) {
            try {
                WURCSExporterGlycoCT exporter = new WURCSExporterGlycoCT();
                exporter.start(glycan.getGlycoCT());
                wurcs = exporter.getWURCS();
                // validate first
                WURCSValidator validator = new WURCSValidator();
                validator.start(wurcs);
                if (validator.getReport().hasError()) {
                    String errorMessage = "";
                    for (String error: validator.getReport().getErrors()) {
                        errorMessage += error + ", ";
                    }
                    errorMessage = errorMessage.substring(0, errorMessage.lastIndexOf(","));
                    throw new IllegalArgumentException ("WURCS conversion error. Reason " + errorMessage);
                } 
            } catch (WURCSException | SugarImporterException | GlycoVisitorException e) {
                logger.warn ("cannot convert sequence into Wurcs to check glytoucan", e);
                logger.info("Glycan sequence that failed:\n" + glycan.getGlycoCT().trim());
                String [] codes;
                if (sugar != null) {
                    // run the validator to get the detailed error messages
                    GlycoVisitorValidationForWURCS t_validationWURCS = new GlycoVisitorValidationForWURCS();
                    t_validationWURCS.start(sugar);
                    codes = t_validationWURCS.getErrors().toArray(new String[0]);
                } else {
                    codes = new String[] {"Sequence: " + glycan.getGlycoCT(), "Error Message: " + e.getMessage()};
                }
                throw new IllegalArgumentException ("WURCS conversion error. Reason " + String.join(",", codes));
            }
            glycan.setWurcs(wurcs);
        } 
        try {
	        String glyToucanId = GlytoucanUtil.getInstance().getAccessionNumber(wurcs);
	        if (glyToucanId != null) {
	            glycan.setGlytoucanID(glyToucanId);
	            glycan.setStatus(RegistrationStatus.ALREADY_IN_GLYTOUCAN);
	        }
        } catch (Exception e) {
        	logger.error("failed to check glytoucan for accession number", e);
        	throw new IllegalArgumentException ("Failed to access glytoucan ID for " + wurcs + "\n. Reason: " + e.getMessage());
        }
    }
    
    public static String getSequenceFromGlytoucan (String glytoucanId) {
        try {
            String wurcsSequence = GlytoucanUtil.getInstance().retrieveGlycan(glytoucanId.trim());
            if (wurcsSequence == null) {
                // cannot be found in Glytoucan
                throw new DataNotFoundException("Glycan with accession number " + glytoucanId + " cannot be found");
            } 
            return wurcsSequence;           
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid Input: Glytoucan ID " + glytoucanId + " failed. Reason: " + e.getMessage());
        }
    }
    
    public static Composition getWurcsCompsitionFromByonic (String sequence) throws DictionaryException, CompositionParseException {
    	if (sequence != null) {
    		String[] monoWithCountList = sequence.split("\\)");
    		String composition  = "";
    		for (String monoWithCount: monoWithCountList) {
    			String mono = monoWithCount.substring(0, monoWithCount.indexOf("("));
    			String count = monoWithCount.substring(monoWithCount.indexOf("(")+1);
    			if (mono.equalsIgnoreCase("methyl")) {
    				mono = "Me";
    			}
    			if (mono.equalsIgnoreCase("phospho")) {
    				mono = "P";
    			}
    			composition += mono + ":" + count + "|";
    		}
    		if (composition.endsWith("|"))
    			composition = composition.substring(0, composition.length()-1);
    		
    		Composition compo = CompositionUtils.parse(composition);
    		return compo;
    	}
    	
    	return null;
    }
    
    public static Composition getWurcsCompsitionFromCondensed (String sequence) throws DictionaryException, CompositionParseException {
    	if (sequence != null) {
    		
    		String composition  = "";
    		// split by numbers
    		String splitRE = "(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)";
    		String[] monoWithCountList = sequence.split(splitRE);
    		boolean number = false;
    		for (String item: monoWithCountList) {
    			if (number) {
    				composition += ":" + item + "|";
    				number = false;
    			} else {
    				if (singleLetterMapping.get(item) != null)
    					composition += singleLetterMapping.get(item);
    				else {
    					throw new CompositionParseException ("Unrecognized string " + item + " in the condenced sequence");
    				}
    				number = true;
    			}
    		}
    		
    		if (composition.endsWith("|"))
    			composition = composition.substring(0, composition.length()-1);
    		
    		Composition compo = CompositionUtils.parse(composition);
    		return compo;
    	}
    	
    	return null;
    }
    
    public static void main(String[] args) {
		// test byonic export
    	
    	try {
			Composition c = getWurcsCompsitionFromByonic("HexNac(2)dHex(1)");
			System.out.println (c.toString());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
    	
    	// test Juan's condensed sequence
    	try {
    		
    		Composition c = getWurcsCompsitionFromCondensed("H2N2F1");
			System.out.println (c.toString());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}
}
