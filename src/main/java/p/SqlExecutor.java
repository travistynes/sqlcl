package p;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.sql.DataSource;
import java.sql.ResultSetMetaData;
import java.util.List;
import java.util.ArrayList;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

@Component
public class SqlExecutor {
	private static final Logger log = LoggerFactory.getLogger(SqlExecutor.class);

	@Autowired
	private DataSource dataSource;

	/*
	 * Executes the sql statement.
	 * Returns the number of rows selected or updated.
	 */
	public int execute(String sql) throws Exception {
		int rows = 0;

		try(Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
			boolean results = ps.execute();

			if(results) {
				ResultSet rs = ps.getResultSet();
				ResultSetMetaData m = rs.getMetaData();

				int cc = m.getColumnCount();
				List<String> cols = new ArrayList<>();
				for(int i = 1; i < m.getColumnCount() + 1; i++) {
					String colName = m.getColumnName(i).toLowerCase();
					cols.add(colName);
				}

				while(rs.next()) {
					rows++;
					for(String col : cols) {
						log.info(col + ": " + rs.getString(col));
					}

					log.info("----------\n");
				}

				log.info(rows == 0 ? "No data." : (rows + " row(s)"));
			} else {
				rows = ps.getUpdateCount();

				log.info("Updated rows: " + rows);
			}
		}

		return rows;
	}
}
