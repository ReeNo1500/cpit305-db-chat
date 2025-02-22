package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


public class ServerApp {

    static Connection conn;
    static List<Client> clients;

    public static void main(String[] args) throws NoSuchAlgorithmException, SQLException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        Connection conn = DriverManager.getConnection("jdbc:sqlite:src/server/data.db");
        clients = new ArrayList<>();
        try (ServerSocket server = new ServerSocket(5555)) {

            while (true) {

                Socket client = server.accept();
                new thread(client).start();

                DataInputStream dis = new DataInputStream(client.getInputStream());
                DataOutputStream dos = new DataOutputStream(client.getOutputStream());

                String username = dis.readUTF();
                String password = dis.readUTF();

                md.update(password.getBytes());
                password = init.App.byte2hex(md.digest());

                if (checkLogin(username, password)) {
                    dos.writeUTF("success");
                    Client c = new Client(username, getFullName(username), client, dis, dos);
                    clients.add(c);
                    Sender sender = new Sender(c);
                    sender.start();
                    Receiver receiver = new Receiver(c, clients);
                    receiver.start();

                    new Sender(c).start();
                    new Receiver(c, clients).start();
                } else {
                    dos.writeUTF("fail");
                }
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    public static String getFullName(String username) throws SQLException {
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM clients WHERE username LIKE ?;");
        ps.setString(1, "%" + username + "%");
        String fullname = "";
        if (ps.execute()) {
            ResultSet rs = ps.getResultSet();
            while (rs.next()) {
                fullname = rs.getString("name");

            }
        }
        return fullname;
    }

    public static boolean checkLogin(String username, String password) throws SQLException {
        String usernameRes = "";
        String passRes = "";
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM clients WHERE username = ? AND password = ?;");
        ps.setString(1, username);
        ps.setString(2, password);
        if (ps.execute()) {

            ResultSet rs = ps.getResultSet();
            while (rs.next()) {
                usernameRes = rs.getString("username");
                passRes = rs.getString("password");

            }
        }
        return username.equalsIgnoreCase(usernameRes) && password.equalsIgnoreCase(passRes);
    }
}
