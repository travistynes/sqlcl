package p;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.sql.DataSource;
import java.sql.DatabaseMetaData;
import java.sql.ResultSetMetaData;
import java.sql.JDBCType;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.apache.commons.io.IOUtils;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import org.apache.commons.lang3.StringUtils;

@Component
public class SqlExecutor {
	private static final Logger log = LoggerFactory.getLogger(SqlExecutor.class);

	@Autowired
	private DataSource dataSource;

	@Value("${spring.profiles.active}")
	private String profile;

	// List tables in schema
	@Value("${tables:#{null}}")
	private String tables;

	// Describe table schema
	@Value("${describe:#{null}}")
	private String describe;

	// List table indices
	@Value("${index:#{null}}")
	private String index;

	// List table indices using user defined query
	@Value("${idx:#{null}}")
	private String idx;

	// List table indices
	@Value("${indexQuery:#{null}}")
	private String indexQuery;
	
	// Output field separator.
	@Value("${fs:,}")
	private String fieldSeparator;

	// Result set direction (row list or columns).
	@Value("${dir:row}")
	private String resultSetDirection;

	// Print statement before results.
	@Value("${printStatement:true}")
	private boolean printStatement;

	// Print row numbers.
	@Value("${printRowNumbers:true}")
	private boolean printRowNumbers;

	// Write records to output after reaching limit.
	@Value("${flushSize:50}")
	private int flushSize;

	// Commit or rollback transaction
	@Value("${commit:true}")
	private boolean commit;

	public void execute() throws Exception {
		if(describe != null) {
			this.describeTable();
		} else if(tables != null) {
			this.listTables();
		} else if(index != null) {
			this.listIndex(false);
		} else if(idx != null) {
			this.listIndex(true);
		} else {
			String sql = IOUtils.toString(System.in, StandardCharsets.UTF_8);
			this.query(sql);
		}
	}

	private List<String> getStatements(String in) {
		return Arrays.asList(in.split(";"));
	}

	/*
	 * List tables.
	 */
	private void listTables() throws Exception {
		String[] parts = tables.split("\\.");

		String catalog = null;
		String schema = parts[0];
		String tableNamePattern = parts.length == 1 ? null : parts[1];
		String[] types = {"TABLE"};

		try(Connection c = dataSource.getConnection()) {
			DatabaseMetaData metaData = c.getMetaData();
			ResultSet rs = metaData.getTables(catalog, schema, tableNamePattern, types);

			int rows = 0;
			StringBuilder data = new StringBuilder();

			while(rs.next()) {
				rows++;

				String tableName = rs.getString("table_name");

				data.append(tableName);
				data.append("\n");
			}

			if(rows == 0) {
				log.info("No data.");
			} else {
				data.deleteCharAt(data.length() - 1); // Delete last newline char.
				log.info(data.toString());
			}
		}
	}

	/*
	 * Describe table schema.
	 */
	private void describeTable() throws Exception {
		String[] parts = describe.split("\\.");

		String catalog = null;
		String schema = parts.length == 1 ? null : parts[0];
		String table = parts.length == 1 ? parts[0] : parts[1];
		String columnNamePattern = null;

		try(Connection c = dataSource.getConnection()) {
			DatabaseMetaData metaData = c.getMetaData();
			ResultSet rs = metaData.getColumns(catalog, schema, table, columnNamePattern);

			int rows = 0;
			StringBuilder colData = new StringBuilder();

			while(rs.next()) {
				rows++;

				if(rows > 1) {
					colData.append("\n");
				}

				String columnName = rs.getString("column_name");
				String typeName = JDBCType.valueOf(rs.getInt("data_type")).getName();
				int columnSize = rs.getInt("column_size");
				int decimalDigits = rs.getInt("decimal_digits");
				boolean isNullable = rs.getString("is_nullable").equalsIgnoreCase("YES") ? true : false;
				boolean isAutoIncrement = rs.getString("is_autoincrement").equalsIgnoreCase("YES") ? true : false;

				colData.append(columnName.toUpperCase() + " ");
				colData.append(typeName.toLowerCase() + " ");
				colData.append(columnSize + "," + decimalDigits + " ");
				colData.append(isNullable ? "null" : "not null");
			}

			if(rows == 0) {
				log.info("No data.");
			} else {
				log.info(colData.toString());
			}
		}
	}

	/**
	 * This method will use the database metadata to get index info.
	 * I've had problems with insufficient privileges using this method, while being
	 * able to manually query tables for index info without issue. As a workaround,
	 * the --idx option will use a query in the properties file instead
	 * of using the JDBC database metadata. Use the --idx option in environments where
	 * necessary privileges cannot be granted to use the --index option.
	 */
	private void listIndex(boolean userQuery) throws Exception {
		String[] parts = userQuery ? idx.split("\\.") : index.split("\\.");

		String catalog = null;
		String schema = parts.length == 1 ? null : parts[0];
		String table = parts.length == 1 ? parts[0] : parts[1];
		boolean unique = false;
		boolean approximate = false;

		log.info("Index list: " + (parts.length == 1 ? table : schema + "." + table));
		log.info("--------------------------------");

		try(Connection c = dataSource.getConnection()) {
			if(!userQuery) {
				DatabaseMetaData metaData = c.getMetaData();
				ResultSet rs = metaData.getIndexInfo(catalog, schema, table, unique, approximate);
				this.processIndex(rs);
			} else if(indexQuery != null) {
				try(PreparedStatement ps = c.prepareStatement(indexQuery)) {
					if(schema == null) {
						ps.setNull(1, java.sql.Types.VARCHAR);
					} else {
						ps.setString(1, schema);
					}

					ps.setString(2, table);

					ResultSet rs = ps.executeQuery();
					this.processIndex(rs);
				}
			} else {
				log.info("User defined indexQuery must be set in properties file.");
				return;
			}
		}
	}

	private void processIndex(ResultSet rs) throws Exception {
		int rows = 0;
		StringBuilder data = new StringBuilder();
		String currentIndex = "";

		while(rs.next()) {
			rows++;

			String indexName = rs.getString("index_name");
			boolean isUnique = !rs.getBoolean("non_unique");

			if(indexName == null) {
				// Index name will be null when index type is statistics.
				continue;
			}

			if(!currentIndex.equals(indexName)) {
				// New index
				if(rows > 1) {
					data.append("--------\n");
				}

				currentIndex = indexName;
				data.append("Name: " + currentIndex + (isUnique ? " (unique)" : "") + "\n");
			}

			String columnPosition = rs.getString("ordinal_position");
			String columnName = rs.getString("column_name");
			String order = rs.getString("asc_or_desc");

			data.append(columnPosition + ") " + columnName + " ");
			data.append(order + "\n");
		}

		if(rows == 0) {
			log.info("No data.");
		} else {
			// Strip last newline char.
			String output = StringUtils.stripEnd(data.toString(), "\n");

			log.info(output);
			log.info("--------------------------------");
			log.info("Total index count: " + rows);
		}
	}

	/*
	 * Executes the sql statements.
	 */
	public void query(String sql) throws Exception {
		List<String> statements = getStatements(sql);

		try(Connection c = dataSource.getConnection()) {
			c.setAutoCommit(false);

			/*
			 * Try-with-resources will ensure the connection object is closed
			 * before catch and finally run, so we need this try block in order
			 * to call commit and rollback.
			 */
			try {
				for(int statementIdx = 0; statementIdx < statements.size(); statementIdx++) {
					String statement = statements.get(statementIdx).trim();

					if(statement.length() == 0) {
						continue;
					}

					StringBuilder lines = new StringBuilder();
					for(String line : Arrays.asList(statement.split("\n"))) {
						// Strip leading comment characters // in case of SQLJ commented query.
						lines.append(StringUtils.stripStart(line, "//"));

						// Add newline back to preserve formatting.
						lines.append("\n");
					}

					// Remove final newline so we don't log a blank line.
					statement = StringUtils.stripEnd(lines.toString(), "\n");

					if(statementIdx > 0) {
						// Print statement separator from previous results.
						log.info("================================");
					}

					if(printStatement) {
						log.info(statement);
					}

					try(PreparedStatement ps = c.prepareStatement(statement)) {
						long start = System.currentTimeMillis();

						boolean results = ps.execute();

						long stop = System.currentTimeMillis();
						Duration queryDuration = Duration.of(stop - start, ChronoUnit.MILLIS);

						if(results) {
							start = System.currentTimeMillis();

							ResultSet rs = ps.getResultSet();
							int rows = dumpResults(rs);

							stop = System.currentTimeMillis();
							Duration fetchDuration = Duration.of(stop - start, ChronoUnit.MILLIS);
							if(rows > 0 || printStatement) {
								// Separate results (or statement with empty results) from stats.
								log.info("--------------------------------");
							}

							log.info("Rows: " + rows + ", Query: " + queryDuration.toString() + ", Fetch: " + fetchDuration.toString());
						} else {
							int affectedRows = ps.getUpdateCount();

							if(printStatement) {
								// Separate statement from stats.
								log.info("--------------------------------");
							}

							log.info("Affected rows: " + affectedRows + ", Duration: " + queryDuration.toString());
						}
					}
				}

				if(commit) {
					c.commit();
				} else {
					c.rollback();
				}
			} catch(Exception e) {
				c.rollback();
				log.info("Transaction rolled back.");

				throw e;
			} finally {
				c.setAutoCommit(true);
			}
		}
	}

	private int dumpResults(ResultSet rs) throws Exception {
		int rows = 0;

		ResultSetMetaData m = rs.getMetaData();

		int cc = m.getColumnCount();
		List<String> columns = new ArrayList<>();
		String columnList = "";

		for(int i = 1; i < cc + 1; i++) {
			String columnName = m.getColumnName(i).toLowerCase();
			columns.add(columnName);
			columnList += columnName + (i < cc ? fieldSeparator : "");
		}

		StringBuilder rowData = new StringBuilder();
		while(rs.next()) {
			rows++;

			if(rows == 1) {
				if(printStatement) {
					// Separate statement from results.
					log.info("--------------------------------");
				}

				if(resultSetDirection.equals("row")) {
					// Write columns.
					log.info(columnList);
				}
			}

			for(int i = 1; i < cc + 1; i++) {
				if(resultSetDirection.equals("row")) {
					if(i == 1 && printRowNumbers) {
						rowData.append(rows + ") ");
					}

					rowData.append(rs.getString(i) + (i < cc ? fieldSeparator : ""));
				} else {
					if(i == 1) {
						rowData.append((rows == 1 ? "" : "\n") + "[Row " + rows + "]\n");
					}

					rowData.append(columns.get(i - 1) + ": " + rs.getString(i) + "\n");
				}
			}

			if(rows % flushSize == 0) {
				// Write records.
				log.info(rowData.toString());
				rowData = new StringBuilder();
			} else {
				if(resultSetDirection.equals("row")) {
					rowData.append("\n");
				}
			}
		}

		if(rows % flushSize > 0) {
			// Write remaining records.
			rowData.deleteCharAt(rowData.length() - 1); // Delete last newline char.
			log.info(rowData.toString());
		}

		return rows;
	}

	public void setAutoCommit(boolean commit) {
		this.commit = commit;
	}
}
