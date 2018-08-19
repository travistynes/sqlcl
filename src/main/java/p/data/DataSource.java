package p.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Value;

/**
 * Database access beans.
 *
 * @author travis
 */
@Configuration
public class DataSource {

	private static final Logger log = LoggerFactory.getLogger(DataSource.class);

	@Bean
	@ConfigurationProperties(prefix = "db")
	public javax.sql.DataSource pgDS() {
		javax.sql.DataSource ds =  DataSourceBuilder.create().build();

		return ds;
	}

	@Bean
	public JdbcTemplate jdbc(javax.sql.DataSource ds) {
		return new JdbcTemplate(ds);
	}
}
