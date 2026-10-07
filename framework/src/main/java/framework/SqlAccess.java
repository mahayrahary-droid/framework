package framework;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Small JDBC helper for controllers that need SQL without an ORM.
 */
public final class SqlAccess {

    private SqlAccess() {
    }

    public static Connection connect(String url, String user, String password) throws SQLException {
        return java.sql.DriverManager.getConnection(url, user, password);
    }

    public static List<Map<String, Object>> query(
            Connection connection,
            String sql,
            Object... parameters) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindParameters(statement, parameters);
            try (ResultSet resultSet = statement.executeQuery()) {
                return readRows(resultSet);
            }
        }
    }

    public static int update(Connection connection, String sql, Object... parameters) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            bindParameters(statement, parameters);
            return statement.executeUpdate();
        }
    }

    private static void bindParameters(PreparedStatement statement, Object[] parameters) throws SQLException {
        for (int i = 0; i < parameters.length; i++) {
            statement.setObject(i + 1, parameters[i]);
        }
    }

    private static List<Map<String, Object>> readRows(ResultSet resultSet) throws SQLException {
        ResultSetMetaData metadata = resultSet.getMetaData();
        List<Map<String, Object>> rows = new ArrayList<>();
        while (resultSet.next()) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (int column = 1; column <= metadata.getColumnCount(); column++) {
                row.put(metadata.getColumnLabel(column), resultSet.getObject(column));
            }
            rows.add(row);
        }
        return rows;
    }
}
