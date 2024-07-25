package org.glygen.tablemaker.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.glygen.tablemaker.persistence.ColumnVisibillitySetting;
import org.glygen.tablemaker.persistence.SettingEntity;
import org.glygen.tablemaker.persistence.TableMakerTable;
import org.glygen.tablemaker.persistence.UserEntity;
import org.glygen.tablemaker.persistence.dao.ColumnSettingsRepository;
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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/setting")
public class SettingController {

	static Logger logger = org.slf4j.LoggerFactory.getLogger(SettingController.class);
	
	final private ColumnSettingsRepository columnRepository;
	final private SettingRepository settingRepository;
	final private UserRepository userRepository;
	
	public SettingController(SettingRepository settingRepository, ColumnSettingsRepository columnRepository, UserRepository userRepository) {
		this.columnRepository = columnRepository;
		this.settingRepository = settingRepository;
		this.userRepository = userRepository;
	}
	
	@Operation(summary = "Get settings for the user", security = { @SecurityRequirement(name = "bearer-key") })
    @GetMapping("/getsettings")
    public ResponseEntity<SuccessResponse> getSettings() {
		// get user info
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserEntity user = null;
        if (auth != null) { 
            user = userRepository.findByUsernameIgnoreCase(auth.getName());
        }
        
        List<SettingEntity> settings = settingRepository.findAllByUser(user);
        return new ResponseEntity<SuccessResponse>(
    			new SuccessResponse (settings, "settings retrieved"), HttpStatus.OK);
	}
	
	@Operation(summary = "Get column settings for the user for the given table", security = { @SecurityRequirement(name = "bearer-key") })
    @GetMapping("/getcolumnsettings")
    public ResponseEntity<SuccessResponse> getColumnSettings(
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
        return new ResponseEntity<SuccessResponse>(
    			new SuccessResponse (settings, "column settings retrieved"), HttpStatus.OK);
	}
	
	@Operation(summary = "Get all column settings for the user", security = { @SecurityRequirement(name = "bearer-key") })
    @GetMapping("/getallcolumnsettings")
    public ResponseEntity<SuccessResponse> getAllColumnSettings() {
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
        
        return new ResponseEntity<SuccessResponse>(
    			new SuccessResponse (columnSettings, "column settings retrieved"), HttpStatus.OK);
	}
	
	@Operation(summary = "update setting", security = { @SecurityRequirement(name = "bearer-key") })
    @PostMapping("/updatesetting")
    public ResponseEntity<SuccessResponse> updateSetting(@Valid @RequestBody SettingEntity setting) {
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
        
        return new ResponseEntity<>(new SuccessResponse(saved, "setting updated"), HttpStatus.OK);
	}
	
	@Operation(summary = "update column settings", security = { @SecurityRequirement(name = "bearer-key") })
    @PostMapping("/updatecolumnsetting")
    public ResponseEntity<SuccessResponse> updateColumnSetting(
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
        
        return new ResponseEntity<>(new SuccessResponse(settings, "column setting updated"), HttpStatus.OK);
	}
}
