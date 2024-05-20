package org.glygen.tablemaker.util;

import org.eurocarbdb.MolecularFramework.io.SugarImporterException;
import org.eurocarbdb.MolecularFramework.sugar.Sugar;
import org.eurocarbdb.MolecularFramework.util.visitor.GlycoVisitorException;
import org.eurocarbdb.application.glycanbuilder.ResidueType;
import org.eurocarbdb.application.glycanbuilder.dataset.ResidueDictionary;
import org.eurocarbdb.application.glycanbuilder.massutil.IonCloud;
import org.eurocarbdb.application.glycanbuilder.massutil.MassOptions;
import org.glycoinfo.WURCSFramework.io.GlycoCT.GlycoVisitorValidationForWURCS;
import org.glycoinfo.WURCSFramework.io.GlycoCT.WURCSExporterGlycoCT;
import org.glycoinfo.WURCSFramework.util.WURCSException;
import org.glycoinfo.WURCSFramework.util.validation.WURCSValidator;
import org.glycoinfo.application.glycanbuilder.converterWURCS2.WURCS2Parser;
import org.glygen.tablemaker.exception.DataNotFoundException;
import org.glygen.tablemaker.persistence.glycan.Glycan;
import org.glygen.tablemaker.persistence.glycan.RegistrationStatus;
import org.slf4j.LoggerFactory;

public class SequenceUtils {
	static org.slf4j.Logger logger = LoggerFactory.getLogger(SequenceUtils.class);
	
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
		} catch (Exception e) { 
			// error
			glycan.setStatus(RegistrationStatus.ERROR);
			glycan.setError (e.getMessage());
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
}
