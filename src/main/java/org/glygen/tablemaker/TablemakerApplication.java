package org.glygen.tablemaker;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.glygen.tablemaker.config.NamespaceHandler;
import org.glygen.tablemaker.persistence.RoleEntity;
import org.glygen.tablemaker.persistence.dao.CompositionFormatRepository;
import org.glygen.tablemaker.persistence.dao.RoleRepository;
import org.glygen.tablemaker.persistence.glycan.CompositionFormatDescriptor;
import org.glyspace.glytablemaker.composition.CompositionFormat;
import org.glyspace.glytablemaker.composition.ResidueType;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ulisesbocchio.jasyptspringboot.environment.StandardEncryptableEnvironment;

@SpringBootApplication
@EnableScheduling
public class TablemakerApplication {

    @Autowired
    RoleRepository roleRepository;
    
    @Autowired
    CompositionFormatRepository compFormatRepository;
    
    @Autowired
    ObjectMapper mapper;
   
	public static void main(String[] args) {
		new SpringApplicationBuilder()
	    .environment(new StandardEncryptableEnvironment())
	    .sources(TablemakerApplication.class).run(args);
	}
	
	@Bean
	InitializingBean sendDatabase() {
	    return () -> {
	        RoleEntity admin = roleRepository.findByRoleName (RoleEntity.ADMIN);
	        if (admin == null) roleRepository.save(new RoleEntity(RoleEntity.ADMIN));
	        RoleEntity user = roleRepository.findByRoleName (RoleEntity.USER);
	        if (user == null) roleRepository.save(new RoleEntity(RoleEntity.USER));
	        RoleEntity software = roleRepository.findByRoleName (RoleEntity.SOFTWARE);
	        if (software == null) roleRepository.save(new RoleEntity(RoleEntity.SOFTWARE));
	        RoleEntity moderator = roleRepository.findByRoleName (RoleEntity.MODERATOR);
	        if (moderator == null) roleRepository.save(new RoleEntity(RoleEntity.MODERATOR));
	        addSystemCompositionFormats();
	     };
	}
	
	private void addSystemCompositionFormats() {
		List<CompositionFormatDescriptor> formats = compFormatRepository.findByNameEqualsIgnoreCaseAndSystem("byonic format", true);
        if (formats == null || formats.size() == 0) {
        	// save bynoic format
        	CompositionFormatDescriptor format = new CompositionFormatDescriptor();
        	format.setDateCreated(new Date());
        	format.setName("Byonic Format");
        	format.setDescription("Residue names are written in full and every count is enclosed in round brackets, including a count of one");
        	format.setExample("HexNAc(2)Hex(5)Fuc(1)NeuAc(1)");
        	format.setSystem(true);
        	format.setUser(null);
        	CompositionFormat byonic = new CompositionFormat();
        	Map<String, ResidueType> residueMap = new HashMap<String, ResidueType>();
        	residueMap.put("Hex", ResidueType.HEX);
        	residueMap.put("Glc", ResidueType.GLC);
        	residueMap.put("Gal", ResidueType.GAL);
        	residueMap.put("Man", ResidueType.MAN);
        	residueMap.put("HexNAc", ResidueType.HEXNAC);
        	residueMap.put("GlcNAc", ResidueType.GLCNAC);
        	residueMap.put("GalNAc", ResidueType.GALNAC);
        	residueMap.put("dHex", ResidueType.DHEX);
        	residueMap.put("Fuc", ResidueType.FUC);
        	residueMap.put("IdoA", ResidueType.IDOA);
        	residueMap.put("Kdn", ResidueType.KDN);
        	residueMap.put("HexA", ResidueType.HEXA);
        	residueMap.put("GlcA", ResidueType.GLCA);
        	// differences
        	residueMap.put("Pent", ResidueType.PEN);
        	residueMap.put("NeuAc", ResidueType.NEUAC);
        	residueMap.put("NeuGc", ResidueType.NEUGC);
        	residueMap.put("Acetyl", ResidueType.Ac);
        	//residueMap.put("DiNAcBac", "Bac");
        	residueMap.put("Phospo", ResidueType.PHOSPHATE);
        	residueMap.put("Sulfo", ResidueType.SULFATE);
        	byonic.setStringToResidue(residueMap);
        	byonic.setAllowNumberOmission(false);
        	byonic.setOpeningDelimiter("(");
        	byonic.setClosingDelimiter(")");
        	format.setDefinition(mapper.valueToTree(byonic));
        	compFormatRepository.save(format);
        }
        formats = compFormatRepository.findByNameEqualsIgnoreCaseAndSystem("single letter code (wuhrer)", true);
        if (formats == null || formats.size() == 0) {
        	// save single letter format
        	CompositionFormatDescriptor format = new CompositionFormatDescriptor();
        	format.setDateCreated(new Date());
        	format.setName("Single Letter Code (Wuhrer)");
        	format.setDescription("Each residue is a single letter followed by its count, with no delimiters, and a count of one is left out");
        	format.setExample("H5N4FS");
        	format.setSystem(true);
        	format.setUser(null);
        	CompositionFormat single = new CompositionFormat();
        	Map<String, ResidueType> residueMap = new HashMap<String, ResidueType>();
        	residueMap.put("p", ResidueType.PHOSPHATE);
        	residueMap.put("P", ResidueType.PEN);
        	residueMap.put("F", ResidueType.DHEX);
        	residueMap.put("H", ResidueType.HEX);
        	residueMap.put("A", ResidueType.HEXA);
        	residueMap.put("N", ResidueType.HEXNAC);
        	residueMap.put("G", ResidueType.NEUGC);
        	residueMap.put("S", ResidueType.NEUAC);
        	residueMap.put("K", ResidueType.KDN);
        	residueMap.put("O", ResidueType.KDO);
        	//residueMap.put("Sl", "NeuAcLac");
        	residueMap.put("s", ResidueType.SULFATE);
        	residueMap.put("a", ResidueType.Ac);
        	residueMap.put("m", ResidueType.ME);
        	
        	single.setStringToResidue(residueMap);
        	single.setAllowNumberOmission(true);
        	JsonNode node = mapper.valueToTree(single);
        	format.setDefinition(node);
        	compFormatRepository.save(format);
        }
	}

	@EventListener(ApplicationReadyEvent.class)
	public void doSomethingAfterStartup() {
	    NamespaceHandler.loadNamespaces();
	}
}
