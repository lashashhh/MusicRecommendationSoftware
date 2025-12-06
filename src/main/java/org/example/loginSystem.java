package org.example;

import org.mindrot.jbcrypt.BCrypt;
import java.util.Scanner;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class loginSystem {
    private final Connection testConnection;
    private static int currentUserId;

    public static void setCurrentUserId(int id){
        currentUserId = id;
    }
    public static int getCurrentUserId(){
        return currentUserId;
    }

    public loginSystem(){
        this.testConnection = null;
    }

    private Connection getConnection() throws Exception {
        if (testConnection != null) {
            return testConnection;
        }
        return Database.connect();
    }
    public void start() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.print("\n1. Sign Up\n2, Sign In\n3. Exit");
            System.out.print("\nEnter your choice: ");
            int choice = scanner.nextInt();
            scanner.nextLine();

            switch (choice) {
                case 1:
                    signUp(scanner);
                    break;
                case 2:
                    logIn(scanner);
                    break;
                case 3:
                    return;
                default:
                    System.out.println("Invalid choice! Please try again... ");
                    break;
            }
        }
    }

    private void signUp(Scanner scanner) {
        System.out.print("\nEnter your username: ");
        String username = scanner.nextLine().trim();
        try (Connection conn = getConnection()){
            PreparedStatement checkStmt= conn.prepareStatement("SELECT password_hash FROM users WHERE username = ?");
            checkStmt.setString(1, username);
            ResultSet rs = checkStmt.executeQuery();

            if(rs.next()){
                System.out.println("Username already exists! Pick different one!");
                return;
            }

            System.out.print("\nEnter your password: ");
            String password = scanner.nextLine().trim();
            String hashed = BCrypt.hashpw(password, BCrypt.gensalt());

            PreparedStatement insertStmt = conn.prepareStatement("INSERT INTO users (username, password_hash) VALUES (?, ?)");
            insertStmt.setString(1, username);
            insertStmt.setString(2, hashed);
            insertStmt.executeUpdate();

            System.out.println("User " + username + " registered successfully!");
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private void logIn(Scanner scanner) {
        System.out.print("Enter your username: ");
        String username = scanner.nextLine();
        try (Connection conn = getConnection()){
            var stmt = conn.prepareStatement("SELECT password_hash FROM users WHERE username = ?");
            stmt.setString(1, username);
            var rs = stmt.executeQuery();

            if (!rs.next()){
                System.out.println("Username not found!");
                return;
            }

            String storedHash = rs.getString("password_hash");

            int attempts = 3;
            while (attempts != 0){
                System.out.print("Enter your password: ");
                String password = scanner.nextLine();

                if (BCrypt.checkpw(password, storedHash)) {
                    System.out.println("Log in successful!");
                    return;
                } else {
                   if(attempts != 1) {
                        attempts--;
                        System.out.println("Wrong password, you have " + attempts + " attempts left!");
                   } else {
                        System.out.println("Too many failed attempts. Access denied.");
                        return;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public boolean tryLogin(String username, String password) throws Exception {
            try (Connection conn = getConnection()) {
                var stmt = conn.prepareStatement("SELECT id, password_hash FROM users WHERE username = ?");
                stmt.setString(1, username);
                var rs = stmt.executeQuery();
                if (!rs.next()) return false;

                String storedHash = rs.getString("password_hash");
                boolean ok = BCrypt.checkpw(password, storedHash);
                if (ok) {
                    int uid = rs.getInt("id");
                    loginSystem.setCurrentUserId(uid);
                }
                return ok;
            }
    }

    public boolean trySignup(String username, String password) throws Exception{
        try (Connection conn = getConnection()) {
            PreparedStatement checkStmt = conn.prepareStatement("SELECT username FROM users WHERE username = ?");
            checkStmt.setString(1, username);
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next()) return false;
            String hashed = BCrypt.hashpw(password, BCrypt.gensalt());
            PreparedStatement insertStmt = conn.prepareStatement("INSERT INTO users (username, password_hash) VALUES (?, ?)");
            insertStmt.setString(1, username);
            insertStmt.setString(2, hashed);
            insertStmt.executeUpdate();
            return true;
        }
    }
}