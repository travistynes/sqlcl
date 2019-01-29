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
import org.springframework.beans.factory.annotation.Value;

@Component
public class SqlExecutor {
	private static final Logger log = LoggerFactory.getLogger(SqlExecutor.class);

	@Autowired
	private DataSource dataSource;

	@Value("${fs:,}")
	private String fieldSeparator;

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
				String columnList = "";

				for(int i = 1; i < cc + 1; i++) {
					columnList += m.getColumnName(i).toLowerCase() + (i < cc ? fieldSeparator : "");
				}

				StringBuilder rowData = new StringBuilder();
				while(rs.next()) {
					rows++;
					for(int i = 1; i < cc + 1; i++) {
						rowData.append(rs.getString(i) + (i < cc ? fieldSeparator : ""));
					}

					rowData.append("\n");
				}

				if(rows > 0) {
					log.info(columnList);
					log.info(rowData.toString());
				} else {
					log.info("No data.");
				}
			} else {
				rows = ps.getUpdateCount();

				log.info("Updated rows: " + rows);
			}
		}

		return rows;
	}
}
