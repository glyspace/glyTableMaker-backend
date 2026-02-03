package org.glygen.tablemaker;

import org.glygen.tablemaker.config.NamespaceHandler;
import org.glygen.tablemaker.persistence.RoleEntity;
import org.glygen.tablemaker.persistence.dao.RoleRepository;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.ulisesbocchio.jasyptspringboot.environment.StandardEncryptableEnvironment;

@SpringBootApplication
@EnableScheduling
public class TablemakerApplication {

    @Autowired
    RoleRepository roleRepository;
   
	public static void main(String[] args) {
		new SpringApplicationBuilder()
	    .environment(new StandardEncryptableEnvironment())
	    .sources(TablemakerApplication.class).run(args);
	}
	
	@Bean
	InitializingBean sendDatabase() {
	    return () -> {
	        RoleEntity admin = roleRepository.findByRoleName ("ROLE_" + RoleEntity.ADMIN);
	        if (admin == null) roleRepository.save(new RoleEntity("ROLE_" + RoleEntity.ADMIN));
	        RoleEntity user = roleRepository.findByRoleName ("ROLE_" + RoleEntity.USER);
	        if (user == null) roleRepository.save(new RoleEntity("ROLE_" + RoleEntity.USER));
	        RoleEntity software = roleRepository.findByRoleName ("ROLE_" + RoleEntity.SOFTWARE);
	        if (software == null) roleRepository.save(new RoleEntity("ROLE_" + RoleEntity.SOFTWARE));
	        RoleEntity moderator = roleRepository.findByRoleName ("ROLE_" + RoleEntity.MODERATOR);
	        if (moderator == null) roleRepository.save(new RoleEntity("ROLE_" + RoleEntity.MODERATOR));
	      };
	}
	
	@EventListener(ApplicationReadyEvent.class)
	public void doSomethingAfterStartup() {
	    NamespaceHandler.loadNamespaces();
	}
}
