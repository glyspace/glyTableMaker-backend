package org.glygen.tablemaker;

import org.glygen.tablemaker.persistence.RoleEntity;
import org.glygen.tablemaker.persistence.dao.RoleRepository;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;

import com.ulisesbocchio.jasyptspringboot.environment.StandardEncryptableEnvironment;

@SpringBootApplication
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
	      };
	}
}
