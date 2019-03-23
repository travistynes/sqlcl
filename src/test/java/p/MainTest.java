package p;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.Ignore;
import java.sql.Date;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.io.IOUtils;
import java.nio.charset.StandardCharsets;

/**
 * The @ActiveProfile specifies the properties file that will be loaded.
 * When a profile is not set, it will default to the application.properties
 * and try to make connections to the external database.
 *
 * Classpath resources and files will be loaded from /src/main/resources, but will
 * be overridden by those of the same name if they exist in /src/test/resources.
 */
@RunWith(SpringRunner.class)
@ActiveProfiles({"unittest"})
@SpringBootTest
public class MainTest {
	private static final Logger log = LoggerFactory.getLogger(MainTest.class);

	@Autowired
	private DataSource dataSource;

	@Autowired
	private JdbcTemplate jdbc;
	
	@Autowired
	private SqlExecutor sqlExecutor;

	@BeforeClass
	public static void beforeClass() throws Exception {
		// Add data to stdin for the CommandLineRunner to process.
		System.setIn(IOUtils.toInputStream("select current_timestamp ts", StandardCharsets.UTF_8));
	}

	// Runs before each test method.
	@Before
	public void before() {
		ResourceDatabasePopulator rdp = new ResourceDatabasePopulator();
		rdp.addScripts(
				//new ClassPathResource("sql/ddl.sql")
			      );
		rdp.execute(dataSource);
	}

	// Runs after each test method.
	@After
	public void after() {

	}

	@Test
	public void testMultipleStatements() throws Exception {
		String sql = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("sql/multi_statements.sql"), StandardCharsets.UTF_8);

		sqlExecutor.query(sql);
	}
}
