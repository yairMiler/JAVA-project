package DB;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHandler {
    private final Connection conn;

    public DatabaseHandler(String filePath) throws SQLException {
        String url = "jdbc:sqlite:" + filePath;
        conn = DriverManager.getConnection(url);
        ensureTable();
    }

    private void ensureTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS players ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "username TEXT UNIQUE, "
                + "wins INTEGER DEFAULT 0)";
        try (Statement st = conn.createStatement()) { st.execute(sql); }
    }

    // increment wins (insert new user or update)
    public void addWin(String username) throws SQLException {
        // Uses SQLite "ON CONFLICT" compact form:
        String sql = "INSERT INTO players(username, wins) VALUES (?, 1) "
                + "ON CONFLICT(username) DO UPDATE SET wins = wins + 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.executeUpdate();
        }
    }

    public List<String> getLeaderboard() throws SQLException {
        List<String> out = new ArrayList<>();
        String sql = "SELECT username, wins FROM players ORDER BY wins DESC, username";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) out.add(rs.getString("username") + " : " + rs.getInt("wins"));
        }
        return out;
    }

    public void close() throws SQLException { if (conn != null) conn.close(); }
}
