package org.glygen.tablemaker;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

import com.ulisesbocchio.jasyptspringboot.environment.StandardEncryptableEnvironment;

@SpringBootApplication
public class TablemakerApplication {

	public static void main(String[] args) {
		new SpringApplicationBuilder()
	    .environment(new StandardEncryptableEnvironment())
	    .sources(TablemakerApplication.class).run(args);
	}

}
