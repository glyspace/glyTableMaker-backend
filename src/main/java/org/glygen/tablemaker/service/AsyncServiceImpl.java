package org.glygen.tablemaker.service;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.glycoinfo.GlycanCompositionConverter.conversion.CompositionConverter;
import org.glycoinfo.GlycanCompositionConverter.structure.Composition;
import org.glygen.tablemaker.controller.DataController;
import org.glygen.tablemaker.exception.BatchUploadException;
import org.glygen.tablemaker.exception.DuplicateException;
import org.glygen.tablemaker.persistence.BatchUploadEntity;
import org.glygen.tablemaker.persistence.ErrorReportEntity;
import org.glygen.tablemaker.persistence.UploadErrorEntity;
import org.glygen.tablemaker.persistence.UserEntity;
import org.glygen.tablemaker.persistence.dao.GlycanRepository;
import org.glygen.tablemaker.persistence.dao.GlycoproteinRepository;
import org.glygen.tablemaker.persistence.glycan.Glycan;
import org.glygen.tablemaker.persistence.glycan.RegistrationStatus;
import org.glygen.tablemaker.persistence.glycan.UploadStatus;
import org.glygen.tablemaker.persistence.protein.Glycoprotein;
import org.glygen.tablemaker.persistence.protein.GlycoproteinSiteType;
import org.glygen.tablemaker.persistence.protein.MultipleGlycanOrder;
import org.glygen.tablemaker.persistence.protein.Position;
import org.glygen.tablemaker.persistence.protein.SitePosition;
import org.glygen.tablemaker.util.GlytoucanUtil;
import org.glygen.tablemaker.util.SequenceUtils;
import org.glygen.tablemaker.util.UniProtUtil;
import org.glygen.tablemaker.view.ExcelFileWrapper;
import org.glygen.tablemaker.view.GlycanInSiteView;
import org.glygen.tablemaker.view.GlycanView;
import org.glygen.tablemaker.view.GlycoproteinView;
import org.glygen.tablemaker.view.SequenceFormat;
import org.glygen.tablemaker.view.SiteView;
import org.glygen.tablemaker.view.SuccessResponse;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class AsyncServiceImpl implements AsyncService {
	 static Logger logger = org.slf4j.LoggerFactory.getLogger(AsyncServiceImpl.class);
	
	final private GlycanRepository glycanRepository;
	final private GlycanManager glycanManager;
	final private GlycoproteinRepository glycoproteinRepository;
	final private ErrorReportingService errorReportingService;
	
	@Value("${spring.file.imagedirectory}")
    String imageLocation;
	
	GlycanTypes glycanTypeList;
	
	public AsyncServiceImpl(GlycanRepository repository, GlycanManager glycanManager, GlycoproteinRepository glycoproteinRepository, ErrorReportingService errorReportingService) {
		this.glycanRepository = repository;
		this.glycanManager = glycanManager;
		this.glycoproteinRepository = glycoproteinRepository;
		this.errorReportingService = errorReportingService;
		
		try {
            ClassPathResource resource = new ClassPathResource("glycantypes.json");
            ObjectMapper objectMapper = new ObjectMapper();
            glycanTypeList = objectMapper.readValue(resource.getInputStream(), GlycanTypes.class);
        } catch (IOException e) {
            logger.warn("Could not load glycan types configuration file", e);
        }
	}

	@Override
	@Async("GlygenAsyncExecutor")
	public CompletableFuture<SuccessResponse<BatchUploadEntity>> addGlycanFromTextFile(byte[] contents, 
			BatchUploadEntity upload, UserEntity user, SequenceFormat format,
			String delimeter, String tag) {
		try {
            ByteArrayInputStream stream = new   ByteArrayInputStream(contents);
            String fileAsString = IOUtils.toString(stream, StandardCharsets.UTF_8.name());
            String[] structures = fileAsString.split(delimeter);
            int count = 0;
            int countSuccess = 0;
            List<UploadErrorEntity> errors = new ArrayList<>();
            List<Glycan> allGlycans = new ArrayList<>();
            for (String sequence: structures) {
                if (sequence == null || sequence.trim().isEmpty())
                    continue;
                count++;
                try {
                	//  parse and add glycan
                	GlycanView g = new GlycanView();
                	g.setFormat(format);
                	g.setSequence(sequence);
                	Glycan glycan = new Glycan();
                	DataController.parseAndRegisterGlycan(glycan, g, glycanRepository, errorReportingService, user);
                	// save the glycan
                    glycan.setDateCreated(new Date());
                    glycan.setUser(user);
                    Glycan added = glycanManager.addUploadToGlycan(glycan, upload, true, user);
                    allGlycans.add(added);
                    if (added != null) {
                        BufferedImage t_image = DataController.createImageForGlycan(added);
                        if (t_image != null) {
                            String filename = added.getGlycanId() + ".png";
                            //save the image into a file
                            logger.debug("Adding image to " + imageLocation);
                            File imageFile = new File(imageLocation + File.separator + filename);
                            try {
                                ImageIO.write(t_image, "png", imageFile);
                            } catch (IOException e) {
                                logger.error("could not write cartoon image to file", e);
                            }
                        } else {
                            logger.warn ("Glycan image cannot be generated for glycan " + added.getGlycanId());
                        }
                    }
                	countSuccess++;
                } catch (DuplicateException e) {
                	//errors.add(new UploadErrorEntity(count+"", "duplicate", sequence));
                	if (e.getDuplicate() != null && e.getDuplicate() instanceof Glycan) {
                		Glycan existing = (Glycan) e.getDuplicate();
                		if (allGlycans.contains(existing)) {
                			// duplicate within the file
                			// error
                			errors.add(new UploadErrorEntity(count+"", "This sequence is a duplicate in the file", sequence));
                		} else {
                			glycanManager.addUploadToGlycan(existing, upload, false, user);
                			allGlycans.add(existing);
                		}
                		
                	}
                } catch (Exception e) {
                	errors.add(new UploadErrorEntity(count+"", e.getMessage(), sequence));
                }
            }
            if (tag != null && !tag.trim().isEmpty()) {
            	glycanManager.addTagToGlycans(allGlycans, tag, user);
            }
            if (!errors.isEmpty()) {
            	return CompletableFuture.failedFuture(new BatchUploadException("There are errors in the file", errors));
            }
            return CompletableFuture.completedFuture (new SuccessResponse<BatchUploadEntity>(upload, countSuccess + " out of " + count + " glycans are added successfully"));
		} catch (IOException e) {
			List<UploadErrorEntity> errors = new ArrayList<>();
			errors.add(new UploadErrorEntity(null, "File is not valid. Reason: " + e.getMessage(), null));
            return CompletableFuture.failedFuture(new BatchUploadException("File is not valid.", errors));
        }
	}

	@Override
	@Async("GlygenAsyncExecutor")
	public CompletableFuture<SuccessResponse<BatchUploadEntity>> addGlycanFromExcelFile(File file, BatchUploadEntity upload,
			UserEntity user, ExcelFileWrapper excelParameters, String tag) {
		
		try {
			List<UploadErrorEntity> errors = new ArrayList<>();
            List<Glycan> allGlycans = new ArrayList<>();
            GlytoucanUtil glytoucanUtil = GlytoucanUtil.getInstance();
			Workbook workbook = WorkbookFactory.create(file);
			int sheetNo = 0;
			int columnNo = 0;
			int rowNo = 0;
			if (excelParameters != null) {
				if (excelParameters.getSheetNumber() != null) 
					sheetNo = excelParameters.getSheetNumber()-1;   // 0 based index
				else if (excelParameters.getSheetName() != null && !excelParameters.getSheetName().isEmpty()) {
					sheetNo = workbook.getSheetIndex(excelParameters.getSheetName().trim());
				}
	 			if (excelParameters.getColumnNo() != null) 
					columnNo = excelParameters.getColumnNo()-1;
				if (excelParameters.getStartRow() != null) 
					rowNo = excelParameters.getStartRow()-1;
			}
			
			if (sheetNo == -1) {
				errors.add(new UploadErrorEntity("-1", "Sheet name " + excelParameters.getSheetName() + " is invalid", null));
				return CompletableFuture.failedFuture(new BatchUploadException("Sheet name " + excelParameters.getSheetName() + " is invalid.", errors));
			}
			Sheet sheet = workbook.getSheetAt(sheetNo);
			Iterator<Row> rowIterator = sheet.iterator();
			boolean started = false;
			int count = 0;
            int countSuccess = 0;
	        while (rowIterator.hasNext()) {
	            Row row = rowIterator.next();
	            if (row.getRowNum() == rowNo) {
	            	started = true;
	            }
	            if (started) {
	            	count++;
	            	Cell glytoucanIdCell = row.getCell(columnNo);
	            	String sequence = glytoucanUtil.retrieveGlycan(glytoucanIdCell.getStringCellValue().trim());
	            	if (sequence == null) {
	            		errors.add(new UploadErrorEntity(count+"", "GlyTouCan Id " + glytoucanIdCell.getStringCellValue() + " is not valid!", null));
	            		continue;
	            	}
	                try {
	                	//  parse and add glycan
	                	GlycanView g = new GlycanView();
	                	g.setFormat(SequenceFormat.WURCS);
	                	g.setSequence(sequence);
	                	Glycan glycan = new Glycan();
	                	glycan.setGlytoucanID(glytoucanIdCell.getStringCellValue().trim());
	                	glycan.setStatus(RegistrationStatus.ALREADY_IN_GLYTOUCAN);
	                	DataController.parseAndRegisterGlycan(glycan, g, glycanRepository, errorReportingService, user);
	                	// save the glycan
	                    glycan.setDateCreated(new Date());
	                    glycan.setUser(user);
	                    Glycan added = glycanManager.addUploadToGlycan(glycan, upload, true, user);
	                    allGlycans.add(added);
	                    if (added != null) {
	                        BufferedImage t_image = DataController.createImageForGlycan(added);
	                        if (t_image != null) {
	                            String filename = added.getGlycanId() + ".png";
	                            //save the image into a file
	                            logger.debug("Adding image to " + imageLocation);
	                            File imageFile = new File(imageLocation + File.separator + filename);
	                            try {
	                                ImageIO.write(t_image, "png", imageFile);
	                            } catch (IOException e) {
	                                logger.error("could not write cartoon image to file", e);
	                            }
	                        } else {
	                            logger.warn ("Glycan image cannot be generated for glycan " + added.getGlycanId());
	                        }
	                    }
	                	countSuccess++;
	                } catch (DuplicateException e) {
	                	if (e.getDuplicate() != null && e.getDuplicate() instanceof Glycan) {
	                		Glycan existing = (Glycan) e.getDuplicate();
	                		if (allGlycans.contains(existing)) {
	                			// duplicate within the file
	                			// error
	                			errors.add(new UploadErrorEntity(count+"", "This sequence is a duplicate in the file", sequence));
	                		} else {
	                			glycanManager.addUploadToGlycan(existing, upload, false, user);
	                			allGlycans.add(existing);
	                		}
	                		
	                	}
	                } catch (Exception e) {
	                	errors.add(new UploadErrorEntity(count+"", e.getMessage(), sequence));
	                }
	            	
	            }
	        }
	        if (tag != null && !tag.trim().isEmpty()) {
            	glycanManager.addTagToGlycans(allGlycans, tag, user);
            }
            if (!errors.isEmpty()) {
            	return CompletableFuture.failedFuture(new BatchUploadException("There are errors in the file", errors));
            }
	        
	        return CompletableFuture.completedFuture (new SuccessResponse<BatchUploadEntity>(upload, countSuccess + " out of " + count + " glycans are added successfully"));
		} catch (Exception e) {
			List<UploadErrorEntity> errors = new ArrayList<>();
			errors.add(new UploadErrorEntity(null, "File is not valid. Reason: " + e.getMessage(), null));
            return CompletableFuture.failedFuture(new BatchUploadException("File is not valid.", errors));
		}
	}

	@Override
	@Async("GlygenAsyncExecutor")
	public CompletableFuture<SuccessResponse<BatchUploadEntity>> addGlycoproteinFromByonicFile(File file, 
			BatchUploadEntity upload, UserEntity user, String delimeter, String tag, MultipleGlycanOrder order) {
		try {
			List<UploadErrorEntity> errors = new ArrayList<>();
            List<Glycoprotein> allGlycoproteins = new ArrayList<>();
           
            String line;
            List<Glycan> allGlycans = new ArrayList<>();
            Map<GlycoproteinView, List<ByonicRow>> proteinMap = new HashMap<>();
            Map<String, GlycoproteinView> uniprotMap = new HashMap<>();
            Map<String, Glycan> glycanSequenceMap = new HashMap<>();
            int posCol = -1;
            int glycanCol = -1;
            int proteinCol = -1;
            int modCol = -1;
            int seqCol = -1;
            
            String headers = "";
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            	boolean dataStart = false;
            	int count = 0;
                while ((line = br.readLine()) != null) {
                    if (line.startsWith("1 |")) {
                    	String[] cols = headers.split(delimeter);
                    	dataStart = true;
                    	// check the header line to see if required columns exist
	                    for (int i=0; i < cols.length; i++) {
	                    	String col = cols[i].replaceAll("\"", "");
	                    	if (col.equalsIgnoreCase("Pos.")) {
	                    		posCol = i;
	                    	}
	                    	if (col.equalsIgnoreCase("Glycans")) {
	                    		glycanCol = i;
	                    	}
	                    	if(col.equalsIgnoreCase("Mods(variable)")) {
	                    		modCol = i;
	                    	}
	                    	if(col.equalsIgnoreCase("Sequence(unformatted)")) {
	                    		seqCol = i;
	                    	}
	                    	if(col.equalsIgnoreCase("ProteinName")) {
	                    		proteinCol = i;
	                    	}
	                    }
	                    if (posCol == -1 || glycanCol == -1 || modCol == -1 || seqCol == -1 || proteinCol == -1) {
	                    	// ERROR
	                    	String message = "";
	                    	if (posCol == -1) message += "Position column (Pos.) is not found";
	                    	if (glycanCol == -1) message += " Glycan column (Glycans) is not found";
	                    	if (proteinCol == -1) message += "Protein column (Protein Name) is not found";
	                    	if (modCol == -1) message += " Modification column (Mod (variable)) is not found";
	                    	if (seqCol == -1) message += " Sequence column (Sequence (unformatted)) is not found";
	                    	errors.add (new UploadErrorEntity(null, message, line));
	                    	return CompletableFuture.failedFuture(new BatchUploadException("Required columns cannot be located", errors));
	                    }
                    }
                    if (!dataStart) {
                    	headers += line;
	                } else {
	                	String[] data = line.split(delimeter);
                    	// extract all glycans and register them
                    	String glycanData = data[glycanCol];
                    	String[] glycans = glycanData.trim().split (";");
                    	for (String comp: glycans) {
                    		try {
        						// parse and register glycans
                        		Composition compo = SequenceUtils.getWurcsCompositionFromByonic(comp.trim());
            					String strWURCS = CompositionConverter.toWURCS(compo);
        	                	//  parse and add glycan
        	                	GlycanView g = new GlycanView();
        	                	g.setFormat(SequenceFormat.WURCS);
        	                	g.setSequence(strWURCS);
        	                	Glycan glycan = new Glycan();
        	                	DataController.parseAndRegisterGlycan(glycan, g, glycanRepository, errorReportingService, user);
        	                	// save the glycan
        	                    glycan.setDateCreated(new Date());
        	                    glycan.setUser(user);
        	                    allGlycans.add(glycan);
        	                    glycanSequenceMap.put (comp.trim(), glycan);
        	                    Glycan added = glycanManager.addUploadToGlycan(glycan, upload, true, user);
        	                    if (added != null) {
        	                        BufferedImage t_image = DataController.createImageForGlycan(added);
        	                        if (t_image != null) {
        	                            String filename = added.getGlycanId() + ".png";
        	                            //save the image into a file
        	                            logger.debug("Adding image to " + imageLocation);
        	                            File imageFile = new File(imageLocation + File.separator + filename);
        	                            try {
        	                                ImageIO.write(t_image, "png", imageFile);
        	                            } catch (IOException e) {
        	                                logger.error("could not write cartoon image to file", e);
        	                            }
        	                        } else {
        	                            logger.warn ("Glycan image cannot be generated for glycan " + added.getGlycanId());
        	                        }
        	                    }
        	                } catch (DuplicateException e) {
        	                	//errors.add(new UploadErrorEntity(count+"", "duplicate", sequence));
        	                	if (e.getDuplicate() != null && e.getDuplicate() instanceof Glycan) {
        	                		Glycan existing = (Glycan) e.getDuplicate();
        	                		if (!allGlycans.contains(existing)) {
        	                			glycanManager.addUploadToGlycan(existing, upload, false, user);
        	                			allGlycans.add(existing);
        	                			glycanSequenceMap.put(comp.trim(), existing);
        	                		}
        	                	}
        	                } catch (Exception e) {
        	                	errors.add(new UploadErrorEntity(count+"", e.getMessage(), comp));
        	                }
                    	}
                    	
                    	// extract info for glycoprotein
                    	String proteinData = data[proteinCol];
                    	String uniProtId = proteinData.substring(proteinData.indexOf("|")+1, proteinData.lastIndexOf("|"));
                    	String version = "last";
                    	if (proteinData.contains("SV=")) {
                    		version = proteinData.substring(proteinData.indexOf("SV=")+3).trim();
                    	}
                    	// check if it is valid
                    	try {
                        	GlycoproteinView protein = null;
                    		if (!uniprotMap.containsKey(uniProtId)) {
	                    		protein = UniProtUtil.getProteinFromUniProt(uniProtId);
	                    		try {
	                    			String sequence = UniProtUtil.getSequenceFromUniProt(uniProtId, version);
	                    			protein.setSequence(sequence);
	                    			protein.setSequenceVersion(version);
	                    		} catch (IOException e) {
	                    			errors.add(new UploadErrorEntity(count+"", "Could not get the version specific sequence. Reason: " + e.getMessage(), null));
	                    			logger.warn("Could not get the version specific sequence", e);
	                    			protein.setSequenceVersion("last");
	                    		}
	                    		
	                    		//protein.setName(uniProtId + "-" + file.getName());
	                        	uniprotMap.put(uniProtId, protein);
                    		} else {
                    			protein = uniprotMap.get(uniProtId);
                    		}
                    		if (!proteinMap.containsKey(protein)) {
                        		proteinMap.put(protein, new ArrayList<ByonicRow>());
                        	}
                    		ByonicRow row = new ByonicRow();
                        	row.setRow(data);
                        	row.setRowNo(count);
                        	proteinMap.get(protein).add(row);
                    	} catch (Exception e) {
                    		// cannot find the protein in UniProt
                    		errors.add(new UploadErrorEntity(count+"", "Could not find protein " + uniProtId + " in UniProt database. Reason: " + e.getMessage(), null));
                    	}
                    	
                    }
                    if (dataStart) count++;
                }

            } catch (IOException e) {
            	errors.add(new UploadErrorEntity(null, e.getMessage(), null));
            	return CompletableFuture.failedFuture(new BatchUploadException("There are errors in the file", errors));
            }
            
            if (tag != null && !tag.trim().isEmpty()) {
            	glycanManager.addTagToGlycans(allGlycans, tag, user);
            }
            
            for (Glycan glycan: allGlycans) {
	            if (glycan.getGlytoucanID() == null || glycan.getGlytoucanID().isEmpty()) {
	        		// the process needs to wait! 
	        		// set the status, waiting
	        		upload.setStatus(UploadStatus.WAITING);	
	        		break;
	        	}
			}
            if (upload.getStatus() == UploadStatus.WAITING) {
            	// save the upload and retry later
            	return CompletableFuture.completedFuture (new SuccessResponse<BatchUploadEntity>(upload, "Waiting for glytoucan registration"));
            }
            
            // create glycoproteins
            createGlycoproteins (allGlycoproteins, proteinMap, glycanSequenceMap, posCol, glycanCol, proteinCol, 
            		modCol, seqCol, order, upload, errors, user); 
            
			if (tag != null && !tag.trim().isEmpty()) {
            	glycanManager.addTagToGlycoproteins(allGlycoproteins, tag, user);
            }
            
			if (!errors.isEmpty()) {
            	return CompletableFuture.failedFuture(new BatchUploadException("There are errors in the file", errors));
            }
			
		} catch (Exception e) {
			List<UploadErrorEntity> errors = new ArrayList<>();
			errors.add(new UploadErrorEntity(null, "File is not valid. Reason: " + e.getMessage(), null));
            return CompletableFuture.failedFuture(new BatchUploadException("File is not valid.", errors));
		}
		return CompletableFuture.completedFuture (new SuccessResponse<BatchUploadEntity>(upload, "Completed the upload"));
	}

	private void createGlycoproteins(List<Glycoprotein> allGlycoproteins, Map<GlycoproteinView, List<ByonicRow>> proteinMap,
			Map<String, Glycan> glycanSequenceMap, int posCol, int glycanCol, int proteinCol, int modCol, int seqCol, 
			MultipleGlycanOrder order, BatchUploadEntity upload, List<UploadErrorEntity> errors, UserEntity user) {
		for (GlycoproteinView protein: proteinMap.keySet()) {
			try {
				protein.setSites(new ArrayList<>());
				List<ByonicRow> rows = proteinMap.get(protein);
				for (ByonicRow br: rows) {
					String[] row = br.getRow();
					String pos = row[posCol];
					String mod = row[modCol];
					String sequence = row[seqCol];
					try {
						// find the position in the protein sequence and check whether it matches the value in sequence column
						sequence = sequence.substring(sequence.indexOf(".")+1);
						sequence = sequence.replaceAll("\\[.*?\\]", "");
						sequence = sequence.replaceAll("\\.", "");
						sequence = sequence.replaceAll("-", "");
						int position = Integer.parseInt(pos);
						String sub = protein.getSequence().substring(position-1);   // position starts from 1
						if (!sub.contains(sequence)) {
							// ERROR
							errors.add(new UploadErrorEntity((br.getRowNo()+1)+"", "Unable to find peptide " + sequence + " at position " + pos + " in Protein " + protein.getUniprotId(), row[seqCol]));
							continue;
						}
						String glycanColumn = row[glycanCol];
						String[] glycanList = glycanColumn.split(";");
						String[] modList = mod.split(";");
						List<String> acceptedList = new ArrayList<>();
						for (String m: modList) {
							String t = m.substring(m.indexOf("(")+1, m.indexOf("/")).trim();
							if (glycanTypeList != null && glycanTypeList.accepted.contains(t)) {
								acceptedList.add(m.trim());
							}
							// check if glycanType is in the dictionary
							if (glycanTypeList != null && !glycanTypeList.accepted.contains(t) && !glycanTypeList.ignored.contains(t)) {
								errors.add(new UploadErrorEntity(br.getRowNo()+"", "Glycan type " + t + " is not found in the dictionary. Contact the admininstrator to update the dictionary", null));
								ErrorReportEntity error = new ErrorReportEntity();
			    				error.setMessage("Glycan type " + t + " is not found in the dictionary.");
			    				error.setDetails("Error occurred in file " + upload.getFilename() + " at row " + br.getRowNo());
			    				error.setDateReported(new Date());
			    				error.setTicketLabel("ByonicDictionary");
			    				errorReportingService.reportError(error);
							}
						}
						if (!acceptedList.isEmpty()) {
							modList = acceptedList.toArray(new String[acceptedList.size()]);
						}
						if (glycanList.length != modList.length) {
							// ERROR
							errors.add(new UploadErrorEntity((br.getRowNo()+1)+"", "Number of glycans in Glycans column does not match the number given in mods column", null));
							continue;
						}
						int i=0;
						for (String seq: glycanList) {
							switch (order) {
							case ALTERNATIVE:
								// create a new Site
								SiteView site = new SiteView();
								if (modList.length == 1) {
									// single site,
									site.setType(GlycoproteinSiteType.EXPLICIT);
								} else {
									site.setType(GlycoproteinSiteType.ALTERNATIVE);
								}
								SitePosition sp = new SitePosition();
								List<Position> pList = new ArrayList<>();
								sp.setPositionList(pList);
								String glycanType = null;
								boolean doNotAssign = false;
								for (String m: modList) {
									String offsetStr = m.substring(0, m.indexOf("("));
									char amino = offsetStr.charAt(0);
									String t = m.substring(m.indexOf("(")+1, m.indexOf("/")).trim();
									if (glycanType != null && !glycanType.equalsIgnoreCase(t)) {
										doNotAssign = true;
									}
									glycanType = t;
									try {
										int offset = Integer.parseInt(offsetStr.substring(1));
										if (sequence.charAt(offset-1) != amino) {
											// ERROR
											errors.add(new UploadErrorEntity((br.getRowNo()+1)+"", "Aminoacid " + amino + " is not found in the given position " + offset, null));
											break;
										} else {
											Position p = new Position();
											p.setAminoAcid(amino+"");
											p.setLocation(Long.valueOf(position + offset-1));
											pList.add(p);
											site.setPosition(sp);
											site.setGlycans(new ArrayList<>());
											GlycanInSiteView gsv = new GlycanInSiteView();
											gsv.setGlycan(glycanSequenceMap.get(seq.trim()));
											gsv.setType("Glycan");
											if (!doNotAssign) {
												if (glycanType.equalsIgnoreCase("OGlycan")) {
													gsv.setGlycosylationType("O-linked");
												} else if (glycanType.equalsIgnoreCase("NGlycan")) {
													gsv.setGlycosylationType("N-linked");
												}
											}
											site.getGlycans().add(gsv);	
										}
									} catch (NumberFormatException e) {
										// ERROR
										errors.add(new UploadErrorEntity((br.getRowNo()+1)+"", "Position given cannot be converted: " + offsetStr, null));
										break;
									}
								}
								if (!protein.getSites().contains(site)) {
									protein.getSites().add(site);
								}
						
								break;
							case BYONICORDER:
								String offsetStr = modList[i].substring(0, modList[i].indexOf("("));
								char amino = offsetStr.charAt(0);
								glycanType = modList[i].substring(modList[i].indexOf("(")+1, modList[i].indexOf("/")).trim();
								try {
									int offset = Integer.parseInt(offsetStr.substring(1));
									if (sequence.charAt(offset-1) != amino) {
										// ERROR
										errors.add(new UploadErrorEntity((br.getRowNo()+1)+"", "Aminoacid " + amino + " is not found in the given position " + offset, null));
									} else {
										// create a new Site
										site = new SiteView();
										site.setType(GlycoproteinSiteType.EXPLICIT);
										sp = new SitePosition();
										Position p = new Position();
										p.setAminoAcid(amino+"");
										p.setLocation(Long.valueOf(position + offset-1));
										pList = new ArrayList<>();
										pList.add(p);
										sp.setPositionList(pList);
										site.setPosition(sp);
										site.setGlycans(new ArrayList<>());
										GlycanInSiteView gsv = new GlycanInSiteView();
										gsv.setGlycan(glycanSequenceMap.get(seq.trim()));
										gsv.setType("Glycan");
										site.getGlycans().add(gsv);		
										if (glycanType.equalsIgnoreCase("OGlycan")) {
											gsv.setGlycosylationType("O-linked");
										} else if (glycanType.equalsIgnoreCase("NGlycan")) {
											gsv.setGlycosylationType("N-linked");
										}
										if (!protein.getSites().contains(site)) {
											protein.getSites().add(site);
										}
									}
								} catch (NumberFormatException e) {
									// ERROR
									errors.add(new UploadErrorEntity((br.getRowNo()+1)+"", "Position given cannot be converted: " + offsetStr, null));
								}
								break;
							case RANGE:
								// create a new Site
								site = new SiteView();
								site.setType(GlycoproteinSiteType.RANGE);
								sp = new SitePosition();
								pList = new ArrayList<>();
								sp.setPositionList(pList);
								Position start = new Position();
								start.setLocation(Long.valueOf(position));
								pList.add(start);
								Position end = new Position();
								end.setLocation(Long.valueOf(position + sequence.length()));
								pList.add(end);
								site.setPosition(sp);
								site.setGlycans(new ArrayList<>());
								GlycanInSiteView gsv = new GlycanInSiteView();
								gsv.setGlycan(glycanSequenceMap.get(seq.trim()));
								gsv.setType("Glycan");
								site.getGlycans().add(gsv);	
								glycanType = null;
								doNotAssign = false;
								for (String m: modList) {
									String t = m.substring(m.indexOf("(")+1, m.indexOf("/")).trim();
									if (glycanType != null && !glycanType.equalsIgnoreCase(t)) {
										doNotAssign = true;
										break;
									}
									glycanType = t;
								}
								if (!doNotAssign && glycanType != null) {
									if (glycanType.equalsIgnoreCase("OGlycan")) {
										gsv.setGlycosylationType("O-linked");
									} else if (glycanType.equalsIgnoreCase("NGlycan")) {
										gsv.setGlycosylationType("N-linked");
									}
								}
								if (!protein.getSites().contains(site)) {
									protein.getSites().add(site);
								}
								
								break;
							}
							i++;
						}
					} catch (NumberFormatException | IndexOutOfBoundsException e) {
						errors.add(new UploadErrorEntity(null, "Unable to find peptide " + row[seqCol] + " at position " + pos + " in Protein " + protein.getUniprotId() + " Error occured: " + e.getMessage(), row[seqCol]));
						continue;
					}
				}
				Glycoprotein saved = DataController.addGlycoprotein(protein, user, glycoproteinRepository);
				Glycoprotein updated = glycanManager.addUploadToGlycoprotein(saved, upload, true, user);
				allGlycoproteins.add(updated);
			} catch (DuplicateException e) {
				if (e.getDuplicate() != null && e.getDuplicate() instanceof Glycoprotein) {
            		Glycoprotein existing = (Glycoprotein) e.getDuplicate();
            		if (!allGlycoproteins.contains(existing)) {
            			glycanManager.addUploadToGlycoprotein(existing, upload, false, user);
            			allGlycoproteins.add(existing);
            		}
            	}
			} catch (Exception e) {
				errors.add(new UploadErrorEntity(null, e.getMessage(), protein.getUniprotId()));
			}
		}
	}   
}

class ByonicRow {
	int rowNo;
	String[] row;
	
	public int getRowNo() {
		return rowNo;
	}
	public void setRowNo(int rowNo) {
		this.rowNo = rowNo;
	}
	public String[] getRow() {
		return row;
	}
	public void setRow(String[] row) {
		this.row = row;
	}
}

class GlycanTypes {
	List<String> accepted;
	List<String> ignored;
	
	public List<String> getAccepted() {
		return accepted;
	}
	public void setAccepted(List<String> accepted) {
		this.accepted = accepted;
	}
	public List<String> getIgnored() {
		return ignored;
	}
	public void setIgnored(List<String> ignored) {
		this.ignored = ignored;
	}
}
