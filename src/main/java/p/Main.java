package p;

import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
import org.apache.commons.io.IOUtils;

@SpringBootApplication
public class Main {

	private static final Logger log = LoggerFactory.getLogger(Main.class);

	public static void main(String[] args) throws Exception {
		log.debug("Application starting.");
		SpringApplication.run(Main.class, args);

		log.debug("Application started.");
	}

	/**
	 * Called after container has constructed the Main instance.
	 */
	@PostConstruct
	private void init() throws Exception {
		log.debug("Application created. Running post initialization.");

//		String ts = jdbc.queryForObject("select current_timestamp ts", (rs, rn) -> {
//			return rs.getString("ts");
//		});

		log.debug("Initialization complete.");
	}

	/**
	 * Shutdown hook.
	 */
	@PreDestroy
	private void shutdown() {
		log.debug("Application shutdown.");
	}
}
