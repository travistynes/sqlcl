package p;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.io.IOUtils;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.sql.DataSource;
import java.sql.ResultSetMetaData;
import java.util.List;
import java.util.ArrayList;

@Component
public class CLR implements CommandLineRunner {
	private static final Logger log = LoggerFactory.getLogger(CLR.class);

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private DataSource dataSource;

	@Override
	public void run(String... args) throws Exception {
		String sql = IOUtils.toString(System.in, "UTF-8");

		try(Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
			ResultSet rs = ps.executeQuery();
			ResultSetMetaData m = rs.getMetaData();

			int cc = m.getColumnCount();
			List<String> cols = new ArrayList<>();
			for(int i = 1; i < m.getColumnCount() + 1; i++) {
				String colName = m.getColumnName(i).toLowerCase();
				cols.add(colName);
			}

			int i = 0;
			while(rs.next()) {
				i++;
				for(String col : cols) {
					log.info(col + ": " + rs.getString(col));
				}

				log.info("----------\n");
			}

			log.info(i == 0 ? "No data." : (i + " row(s)"));
		}
	}
}
