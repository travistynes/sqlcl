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
import java.util.List;
import java.util.ArrayList;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.apache.commons.io.IOUtils;

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
	//
	// List table indices
	@Value("${index:#{null}}")
	private String index;

	// Output field separator.
	@Value("${fs:,}")
	private String fieldSeparator;

	// Write records to output after reaching limit.
	@Value("${flushSize:50}")
	private int flushSize;

	public void execute() throws Exception {
		if(describe != null) {
			this.describeTable();
		} else if(tables != null) {
			this.listTables();
		} else if(index != null) {
			this.listIndex();
		} else {
			String sql = IOUtils.toString(System.in, "UTF-8");
			this.query(sql);
		}
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
				boolean isNullable = rs.getString("is_nullable") == "YES" ? true : false;
				boolean isAutoIncrement = rs.getString("is_autoincrement") == "YES" ? true : false;

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

	/*
	 * List table indices.
	 */
	private void listIndex() throws Exception {
		String[] parts = index.split("\\.");

		String catalog = null;
		String schema = parts.length == 1 ? null : parts[0];
		String table = parts.length == 1 ? parts[0] : parts[1];
		boolean unique = false;
		boolean approximate = false;

		try(Connection c = dataSource.getConnection()) {
			DatabaseMetaData metaData = c.getMetaData();
			ResultSet rs = metaData.getIndexInfo(catalog, schema, table, unique, approximate);

			int rows = 0;
			StringBuilder data = new StringBuilder();
			String currentIndex = "";

			while(rs.next()) {
				rows++;

				if(rows == 1) {
					log.info(rs.getString("table_schem") + "." + rs.getString("table_name"));
				}

				String indexName = rs.getString("index_name");
				boolean isUnique = !rs.getBoolean("non_unique");

				if(indexName == null) {
					// Index name will be null when index type is statistics.
					continue;
				}

				if(!currentIndex.equals(indexName)) {
					// New index
					currentIndex = indexName;
					data.append("\n" + currentIndex + (isUnique ? " (unique)" : ""));
					data.append("\n--------\n");
				}

				String columnName = rs.getString("column_name");
				String order = rs.getString("asc_or_desc") == "A" ? "asc" : "desc";

				data.append(columnName + " ");
				data.append(order);
				data.append("\n");
			}

			if(rows == 0) {
				log.info("No data.");
			} else {
				log.info(data.toString().toLowerCase());
			}
		}
	}

	/*
	 * Executes the sql statement.
	 */
	public int query(String sql) throws Exception {
		int rows = 0;

		try(Connection c = dataSource.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
			boolean results = ps.execute();

			if(results) {
				ResultSet rs = ps.getResultSet();
				rows = dumpResults(rs);
			} else {
				rows = ps.getUpdateCount();

				log.info("Updated rows: " + rows);
			}
		}

		return rows;
	}

	private int dumpResults(ResultSet rs) throws Exception {
		int rows = 0;

		ResultSetMetaData m = rs.getMetaData();

		int cc = m.getColumnCount();
		String columnList = "";

		for(int i = 1; i < cc + 1; i++) {
			columnList += m.getColumnName(i).toLowerCase() + (i < cc ? fieldSeparator : "");
		}

		StringBuilder rowData = new StringBuilder();
		while(rs.next()) {
			rows++;

			if(rows == 1) {
				// Write columns.
				log.info(columnList);
			}

			for(int i = 1; i < cc + 1; i++) {
				rowData.append(rs.getString(i) + (i < cc ? fieldSeparator : ""));
			}

			if(rows % flushSize == 0) {
				// Write records.
				log.info(rowData.toString());
				rowData = new StringBuilder();
			} else {
				rowData.append("\n");
			}
		}

		if(rows % flushSize > 0) {
			// Write remaining records.
			rowData.deleteCharAt(rowData.length() - 1); // Delete last newline char.
			log.info(rowData.toString());
		}

		if(rows == 0) {
			log.info("No data.");
		}

		return rows;
	}
}
