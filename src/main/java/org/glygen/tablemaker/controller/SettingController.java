package org.glygen.tablemaker.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.glygen.tablemaker.persistence.ColumnVisibillitySetting;
import org.glygen.tablemaker.persistence.ContributorSetting;
import org.glygen.tablemaker.persistence.SettingEntity;
import org.glygen.tablemaker.persistence.TableMakerTable;
import org.glygen.tablemaker.persistence.UserEntity;
import org.glygen.tablemaker.persistence.dao.ColumnSettingsRepository;
import org.glygen.tablemaker.persistence.dao.ContributorSettingRepository;
import org.glygen.tablemaker.persistence.dao.SettingRepository;
import org.glygen.tablemaker.persistence.dao.UserRepository;
import org.glygen.tablemaker.view.SuccessResponse;
import org.slf4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/setting")
public class SettingController {

	static Logger logger = org.slf4j.LoggerFactory.getLogger(SettingController.class);
	
	final private ColumnSettingsRepository columnRepository;
	final private SettingRepository settingRepository;
	final private UserRepository userRepository;
	final private ContributorSettingRepository contributorSettingRepository;
	
	public SettingController(SettingRepository settingRepository, ColumnSettingsRepository columnRepository, UserRepository userRepository, ContributorSettingRepository contributorSettingRepository) {
		this.columnRepository = columnRepository;
		this.settingRepository = settingRepository;
		this.userRepository = userRepository;
		this.contributorSettingRepository = contributorSettingRepository;
	}
	
	@Operation(summary = "Get settings for the user", security = { @SecurityRequirement(name = "bearer-key") })
    @GetMapping("/getsettings")
    public ResponseEntity<SuccessResponse<List<SettingEntity>>> getSettings() {
		// get user info
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserEntity user = null;
        if (auth != null) { 
            user = userRepository.findByUsernameIgnoreCase(auth.getName());
        }
        
        List<SettingEntity> settings = settingRepository.findAllByUser(user);
        return new ResponseEntity<>(
    			new SuccessResponse<List<SettingEntity>> (settings, "settings retrieved"), HttpStatus.OK);
	}
	
	@Operation(summary = "Get column settings for the user for the given table", security = { @SecurityRequirement(name = "bearer-key") })
    @GetMapping("/getcolumnsettings")
    public ResponseEntity<SuccessResponse<List<ColumnVisibillitySetting>>> getColumnSettings(
    		@Parameter(required=true, description="table name for the column settings") 
    		@RequestParam("tablename")
    		TableMakerTable tableName) {
		// get user info
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserEntity user = null;
        if (auth != null) { 
            user = userRepository.findByUsernameIgnoreCase(auth.getName());
        }
        
        List<ColumnVisibillitySetting> settings = columnRepository.findByTableNameAndUser(tableName, user);
        return new ResponseEntity<>(
    			new SuccessResponse<List<ColumnVisibillitySetting>> (settings, "column settings retrieved"), HttpStatus.OK);
	}
	
	@Operation(summary = "Get all column settings for the user", security = { @SecurityRequirement(name = "bearer-key") })
    @GetMapping("/getallcolumnsettings")
    public ResponseEntity<SuccessResponse<Map<String, List<ColumnVisibillitySetting>>>> getAllColumnSettings() {
		// get user info
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserEntity user = null;
        if (auth != null) { 
            user = userRepository.findByUsernameIgnoreCase(auth.getName());
        }
        
        Map<String, List<ColumnVisibillitySetting>> columnSettings = new HashMap<>();
        for (TableMakerTable table: TableMakerTable.values()) {
        	columnSettings.put(table.name(), new ArrayList<>());
        }
        
        List<ColumnVisibillitySetting> settings = columnRepository.findAllByUser(user);
        for (ColumnVisibillitySetting setting: settings) {
        	if (columnSettings.get(setting.getTableName().name()) != null) {
        		columnSettings.get(setting.getTableName().name()).add(setting);
        		setting.setUser(null); // no need to pass the details
        	} else {
        		throw new EntityNotFoundException("Setting table name " + setting.getTableName().name() + " does not exist!");
        	}
        }
        
        return new ResponseEntity<>(
    			new SuccessResponse<Map<String, List<ColumnVisibillitySetting>>> (columnSettings, "column settings retrieved"), HttpStatus.OK);
	}

	@Operation(summary = "Get default contributor setting for the user", security = { @SecurityRequirement(name = "bearer-key") })
    @GetMapping("/getdefaultcontributorsetting")
    public ResponseEntity<SuccessResponse<ContributorSetting>> getDefaultContributorSetting() {
		// get user info
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserEntity user = null;
        if (auth != null) { 
            user = userRepository.findByUsernameIgnoreCase(auth.getName());
        }
        
        
        List<ContributorSetting> settings = contributorSettingRepository.findAllByUserAndDefaultSetting(user, true);
        if (settings.size() > 0) {
        	ContributorSetting defaultSetting = settings.get(0);
        	defaultSetting.setUser(null);
        	return new ResponseEntity<>(
        			new SuccessResponse<ContributorSetting> (defaultSetting, "default contributor setting retrieved"), HttpStatus.OK);
        } else {
    		throw new EntityNotFoundException("Default contribuor setting for user " + user.getUsername() + " does not exist!");
    	}
	}
	
	private static ContributorSetting toView(ContributorSetting s) {
		ContributorSetting v = new ContributorSetting();
		v.setId(s.getId());
		v.setName(s.getName());
		v.setContributor(s.getContributor());
		v.setDefaultSetting(s.getDefaultSetting());
		return v;
	}
	
	@Operation(summary = "Get all contributor settings for the user", security = { @SecurityRequirement(name = "bearer-key") })
    @GetMapping("/getallcontributorsettings")
    public ResponseEntity<SuccessResponse<List<ContributorSetting>>> getAllContributorSettings() {
		// get user info
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserEntity user = null;
        if (auth != null) { 
            user = userRepository.findByUsernameIgnoreCase(auth.getName());
        }
        
        
        List<ContributorSetting> settings = contributorSettingRepository.findAllByUser(user)
        		.stream().map(SettingController::toView).collect(Collectors.toList());
        	return new ResponseEntity<>(
        			new SuccessResponse<List<ContributorSetting>> (settings, "all contributor settings retrieved"), HttpStatus.OK);
	}
	
	@Operation(summary = "Replace the contributor settings of the user with the given list", security = { @SecurityRequirement(name = "bearer-key") })
    @PostMapping("/updatecontributorsettings")
    @Transactional
    public ResponseEntity<SuccessResponse<List<ContributorSetting>>> updateContributorSettings(
    		@RequestBody List<ContributorSetting> settings) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserEntity user = null;
        if (auth != null) { 
            user = userRepository.findByUsernameIgnoreCase(auth.getName());
        }
        
        ObjectMapper mapper = new ObjectMapper();
        Set<String> names = new LinkedHashSet<>();
        for (ContributorSetting s: settings) {
        	if (s.getName() == null || s.getName().isBlank()) {
        		throw new IllegalArgumentException("Contributor setting name is required");
        	}
        	if (!names.add(s.getName().trim().toLowerCase())) {
        		throw new IllegalArgumentException("Contributor setting name '" + s.getName() + "' is not unique");
        	}
        	try {
        		mapper.readTree(s.getContributor());
        	} catch (Exception e) {
        		throw new IllegalArgumentException("Contributor information for '" + s.getName() + "' is not valid JSON");
        	}
        }
        
        boolean defaultSeen = false;
        for (ContributorSetting s: settings) {
        	boolean isDefault = Boolean.TRUE.equals(s.getDefaultSetting()) && !defaultSeen;
        	s.setDefaultSetting(isDefault);
        	defaultSeen = defaultSeen || isDefault;
        }
        if (!settings.isEmpty() && !defaultSeen) {
        	settings.get(0).setDefaultSetting(true);
        }
        
        Map<Long, ContributorSetting> existing = new HashMap<>();
        for (ContributorSetting e: contributorSettingRepository.findAllByUser(user)) {
        	existing.put(e.getId(), e);
        }
        
        List<ContributorSetting> result = new ArrayList<>();
        for (ContributorSetting s: settings) {
        	ContributorSetting target = s.getId() != null ? existing.remove(s.getId()) : null;
        	if (target == null) {
        		target = new ContributorSetting();  
        		target.setUser(user);
        	}
        	target.setName(s.getName().trim());
        	target.setContributor(s.getContributor());
        	target.setDefaultSetting(s.getDefaultSetting());
        	result.add(contributorSettingRepository.save(target));
        }
        contributorSettingRepository.deleteAll(existing.values());
        
        return new ResponseEntity<>(
        		new SuccessResponse<List<ContributorSetting>>(result.stream().map(SettingController::toView).collect(Collectors.toList()), 
        				"contributor settings updated"), HttpStatus.OK);
	}
	
	@Operation(summary = "update setting", security = { @SecurityRequirement(name = "bearer-key") })
    @PostMapping("/updatesetting")
    public ResponseEntity<SuccessResponse<SettingEntity>> updateSetting(@Valid @RequestBody SettingEntity setting) {
		// get user info
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserEntity user = null;
        if (auth != null) { 
            user = userRepository.findByUsernameIgnoreCase(auth.getName());
        }
        
        Optional<SettingEntity> existing = settingRepository.findByNameAndUser(setting.getName(), user);
        SettingEntity saved = null;
        if (existing.isPresent()) {
        	existing.get().setValue(setting.getValue());
        	saved = settingRepository.save(existing.get());
        } else {
        	// new setting
        	setting.setUser(user);
        	saved = settingRepository.save(setting);
        }
        
        return new ResponseEntity<>(new SuccessResponse<SettingEntity>(saved, "setting updated"), HttpStatus.OK);
	}
	
	@Operation(summary = "update column settings", security = { @SecurityRequirement(name = "bearer-key") })
    @PostMapping("/updatecolumnsetting")
    public ResponseEntity<SuccessResponse<List<ColumnVisibillitySetting>>> updateColumnSetting(
    		@Valid 
    		@RequestBody 
    		List<ColumnVisibillitySetting> settings) {
		// get user info
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserEntity user = null;
        if (auth != null) { 
            user = userRepository.findByUsernameIgnoreCase(auth.getName());
        }
        
        
        for (ColumnVisibillitySetting setting: settings) {
        	ColumnVisibillitySetting saved = null;
	        List<ColumnVisibillitySetting> existing = columnRepository.findByTableNameAndUser(setting.getTableName(), user);
	        for (ColumnVisibillitySetting s: existing) {
	        	if (s.getColumnName().equalsIgnoreCase(setting.getColumnName())) {
	        		s.setVisible(setting.getVisible());
	        		saved = columnRepository.save (s);
	        		break;
	        	}
	        }
	        
	        if (saved == null) {
	        	// no existing setting
	        	setting.setUser(user);
	        	columnRepository.save(setting);
	        }
        }
        
        return new ResponseEntity<>(new SuccessResponse<List<ColumnVisibillitySetting>>(settings, "column setting updated"), HttpStatus.OK);
	}
}
