import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class User implements Serializable {
    private static final long serialVersionUID = 5L;

    private String userName;
    private String password; 
    private String email;
    private String phoneNumber;
    private boolean isAdmin;

    public User(String userName, String password, String email, String phoneNumber, boolean isAdmin) {
        this.userName = userName;
        this.password = password;
        this.email = email;
        this.phoneNumber = (phoneNumber != null) ? phoneNumber : "N/A";
        this.isAdmin = isAdmin;
    }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public boolean isAdmin() { return isAdmin; }

    public static boolean isValidGmail(String email) {
        return email != null && email.toLowerCase().endsWith("@gmail.com") && email.length() > 10;
    }
    public static String hashPassword(String plainPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(plainPassword.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            throw new RuntimeException("Password hashing failed", e);
        }
    }
    public static List<User> loadUsers() {
        Db.initSchema();
        seedDefaultUsersIfEmpty();
        List<User> list = new ArrayList<User>();
        String sql = "SELECT * FROM users ORDER BY user_name";
        try (Statement st = Db.get().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapUser(rs));
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load users", e);
        }
        return list;
    }

    private static void seedDefaultUsersIfEmpty() {
        try (Statement st = Db.get().createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM users")) {
            rs.next();
            if (rs.getInt(1) == 0) {
                insertUser(new User("admin", hashPassword("admin123"), "admin@cinema.com", "01700000000", true));
                insertUser(new User("nirob", hashPassword("1234"), "nirob@gmail.com", "01800000000", false));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to seed default users", e);
        }
    }

    private static User mapUser(ResultSet rs) throws SQLException {
        return new User(rs.getString("user_name"), rs.getString("password_hash"), rs.getString("email"),
                rs.getString("phone_number"), rs.getInt("is_admin") == 1);
    }

    private static void insertUser(User u) {
        String sql = "INSERT INTO users (user_name, password_hash, email, phone_number, is_admin) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = Db.get().prepareStatement(sql)) {
            ps.setString(1, u.getUserName());
            ps.setString(2, u.getPassword());
            ps.setString(3, u.getEmail());
            ps.setString(4, u.getPhoneNumber());
            ps.setInt(5, u.isAdmin() ? 1 : 0);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to insert user", e);
        }
    }

    public static void saveUsers(List<User> users) {
    }

    public static User authenticate(String username, String password) {
        Db.initSchema();
        seedDefaultUsersIfEmpty();
        String hashed = hashPassword(password);
        String sql = "SELECT * FROM users WHERE LOWER(user_name)=LOWER(?) AND password_hash=?";
        try (PreparedStatement ps = Db.get().prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, hashed);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapUser(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to authenticate user", e);
        }
        return null;
    }

    public static boolean registerUser(String username, String password, String email, String phoneNumber) {
        Db.initSchema();
        seedDefaultUsersIfEmpty();
        if (userExists(username)) return false;
        insertUser(new User(username, hashPassword(password), email, phoneNumber, false));
        return true;
    }

    private static boolean userExists(String username) {
        String sql = "SELECT 1 FROM users WHERE LOWER(user_name)=LOWER(?)";
        try (PreparedStatement ps = Db.get().prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check user existence", e);
        }
    }

    public static boolean updateUserProfile(String oldUsername, String newUsername, String newPassword, String newEmail, String newPhone) {
        Db.initSchema();
        if (!oldUsername.equalsIgnoreCase(newUsername) && userExists(newUsername)) {
            return false;
        }
        String passwordHash = null;
        if (newPassword != null && !newPassword.trim().isEmpty()) {
            passwordHash = hashPassword(newPassword);
        }
        String sql = passwordHash != null
                ? "UPDATE users SET user_name=?, password_hash=?, email=?, phone_number=? WHERE LOWER(user_name)=LOWER(?)"
                : "UPDATE users SET user_name=?, email=?, phone_number=? WHERE LOWER(user_name)=LOWER(?)";
        try (PreparedStatement ps = Db.get().prepareStatement(sql)) {
            int i = 1;
            ps.setString(i++, newUsername);
            if (passwordHash != null) ps.setString(i++, passwordHash);
            ps.setString(i++, newEmail);
            ps.setString(i++, newPhone);
            ps.setString(i, oldUsername);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update user profile", e);
        }
    }
}
