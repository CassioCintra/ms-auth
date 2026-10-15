package io.github.cassio.ms_auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import io.github.cassio.ms_auth.infra.BootstrapProperties;

@SpringBootApplication
@EnableConfigurationProperties(BootstrapProperties.class)
public class MsAuthApplication {

	public static void main(String[] args) {
		SpringApplication.run(MsAuthApplication.class, args);
	}

}
