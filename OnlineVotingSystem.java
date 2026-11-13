import javax.swing.*;
import java.awt.*;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.sql.*;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class OnlineVotingSystem {
    // Update these values to match your MySQL setup.
    private static final String DB_URL = "jdbc:mysql://localhost:3306/online_voting";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "your_mysql_password";

    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }

    private static String hashPassword(String password) throws Exception {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 256);
        byte[] hash = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec).getEncoded();
        return Base64.getEncoder().encodeToString(salt) + ":" +
               Base64.getEncoder().encodeToString(hash);
    }

    private static boolean verifyPassword(String password, String stored) throws Exception {
        String[] parts = stored.split(":");
        byte[] salt = Base64.getDecoder().decode(parts[0]);
        byte[] expected = Base64.getDecoder().decode(parts[1]);
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, 256);
        byte[] actual = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec).getEncoded();
        if (actual.length != expected.length) return false;
        int diff = 0;
        for (int i = 0; i < actual.length; i++) diff |= actual[i] ^ expected[i];
        return diff == 0;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(OnlineVotingSystem::showHome);
    }

    private static void showHome() {
        JFrame f = new JFrame("Online Voting System");
        f.setSize(420, 300);
        f.setLocationRelativeTo(null);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JLabel title = new JLabel("ONLINE VOTING SYSTEM", SwingConstants.CENTER);
        title.setFont(new Font("Arial", Font.BOLD, 22));
        JButton register = new JButton("Voter Registration");
        JButton login = new JButton("Voter Login");
        JButton results = new JButton("View Results");

        register.addActionListener(e -> registerVoter());
        login.addActionListener(e -> loginVoter());
        results.addActionListener(e -> showResults());

        JPanel p = new JPanel(new GridLayout(4, 1, 12, 12));
        p.setBorder(BorderFactory.createEmptyBorder(25, 45, 25, 45));
        p.add(title); p.add(register); p.add(login); p.add(results);
        f.add(p);
        f.setVisible(true);
    }

    private static void registerVoter() {
        JTextField name = new JTextField();
        JTextField voterId = new JTextField();
        JPasswordField password = new JPasswordField();
        Object[] fields = {"Full name:", name, "Voter ID:", voterId, "Password:", password};

        if (JOptionPane.showConfirmDialog(null, fields, "Voter Registration",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;

        if (name.getText().trim().isEmpty() || voterId.getText().trim().isEmpty()
                || password.getPassword().length == 0) {
            message("Please fill in all fields.");
            return;
        }

        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO voters(name, voter_id, password_hash) VALUES(?,?,?)")) {
            ps.setString(1, name.getText().trim());
            ps.setString(2, voterId.getText().trim());
            ps.setString(3, hashPassword(new String(password.getPassword())));
            ps.executeUpdate();
            message("Registration successful! You can now log in.");
        } catch (SQLIntegrityConstraintViolationException ex) {
            message("This Voter ID is already registered.");
        } catch (Exception ex) {
            message("Registration failed: " + ex.getMessage());
        }
    }

    private static void loginVoter() {
        JTextField voterId = new JTextField();
        JPasswordField password = new JPasswordField();
        Object[] fields = {"Voter ID:", voterId, "Password:", password};

        if (JOptionPane.showConfirmDialog(null, fields, "Voter Login",
                JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;

        try (Connection c = getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT id, password_hash FROM voters WHERE voter_id=?")) {
            ps.setString(1, voterId.getText().trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next() || !verifyPassword(new String(password.getPassword()),
                        rs.getString("password_hash"))) {
                    message("Invalid Voter ID or password.");
                    return;
                }
                openVotingScreen(rs.getInt("id"));
            }
        } catch (Exception ex) {
            message("Login failed: " + ex.getMessage());
        }
    }

    private static void openVotingScreen(int voterDbId) {
        try (Connection c = getConnection();
             PreparedStatement check = c.prepareStatement(
                     "SELECT id FROM votes WHERE voter_id=?")) {
            check.setInt(1, voterDbId);
            try (ResultSet rs = check.executeQuery()) {
                if (rs.next()) {
                    message("You have already voted. Thank you!");
                    return;
                }
            }

            DefaultListModel<Candidate> model = new DefaultListModel<>();
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT id, name, party FROM candidates ORDER BY name")) {
                while (rs.next()) model.addElement(new Candidate(
                        rs.getInt("id"), rs.getString("name"), rs.getString("party")));
            }

            if (model.isEmpty()) {
                message("No candidates found. Add candidates using the SQL setup instructions.");
                return;
            }

            JList<Candidate> list = new JList<>(model);
            list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            list.setVisibleRowCount(6);
            int choice = JOptionPane.showConfirmDialog(null, new JScrollPane(list),
                    "Select one candidate and confirm your vote",
                    JOptionPane.OK_CANCEL_OPTION);
            Candidate selected = list.getSelectedValue();
            if (choice != JOptionPane.OK_OPTION) return;
            if (selected == null) {
                message("Please select a candidate.");
                return;
            }
            if (JOptionPane.showConfirmDialog(null,
                    "Cast your vote for " + selected.name + " (" + selected.party + ")?",
                    "Confirm Vote", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;

            c.setAutoCommit(false);
            try (PreparedStatement insert = c.prepareStatement(
                    "INSERT INTO votes(voter_id, candidate_id) VALUES(?,?)")) {
                insert.setInt(1, voterDbId);
                insert.setInt(2, selected.id);
                insert.executeUpdate();
                c.commit();
                message("Your vote has been recorded successfully!");
            } catch (SQLException ex) {
                c.rollback();
                if (ex.getErrorCode() == 1062) message("A vote has already been recorded for this voter.");
                else message("Could not record vote: " + ex.getMessage());
            } finally {
                c.setAutoCommit(true);
            }
        } catch (Exception ex) {
            message("Unable to open voting screen: " + ex.getMessage());
        }
    }

    private static void showResults() {
        StringBuilder output = new StringBuilder("ELECTION RESULTS\n\n");
        try (Connection c = getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT c.name, c.party, COUNT(v.id) AS total " +
                     "FROM candidates c LEFT JOIN votes v ON c.id=v.candidate_id " +
                     "GROUP BY c.id, c.name, c.party ORDER BY total DESC, c.name")) {
            while (rs.next()) {
                output.append(rs.getString("name")).append(" (")
                      .append(rs.getString("party")).append(") — ")
                      .append(rs.getInt("total")).append(" vote(s)\n");
            }
            if (output.toString().equals("ELECTION RESULTS\n\n"))
                output.append("No candidates available.");
            JTextArea area = new JTextArea(output.toString(), 12, 32);
            area.setEditable(false);
            JOptionPane.showMessageDialog(null, new JScrollPane(area), "Election Results",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            message("Could not load results: " + ex.getMessage());
        }
    }

    private static void message(String text) {
        JOptionPane.showMessageDialog(null, text);
    }

    private static class Candidate {
        int id;
        String name, party;
        Candidate(int id, String name, String party) {
            this.id = id; this.name = name; this.party = party;
        }
        public String toString() { return name + "  |  " + party; }
    }
}
