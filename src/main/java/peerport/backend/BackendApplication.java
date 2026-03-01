package peerport.backend;

import java.util.Collections;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SpringBootApplication
@RestController
public class BackendApplication {
	protected static final Logger logger = LoggerFactory.getLogger(BackendApplication.class);

	public static void main(String[] args) {
		SpringApplication.run(BackendApplication.class, args);
		logger.info("PeerPort backend started successfully. Link: http://localhost:8080");
	}

	// Authentication
	@GetMapping("/user")
	public Map<String, Object> user(@AuthenticationPrincipal OAuth2User principal) {
		if (principal == null) {
			logger.warn("Unauthorized access attempt to /user endpoint.");
			return Collections.singletonMap("error", "Unauthorized");
		}
		return Collections.singletonMap("name", principal.getAttribute("name"));
	}

}
