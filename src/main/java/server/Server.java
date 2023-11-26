package server;

import client.ClientHandler;
import database.OnLimeDB;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server {
    private static Server server;
    private ServerSocket serverSocket;
    private List<ClientHandler> clients = new ArrayList<>();
    private ObservableList<String> loggedInUsers = FXCollections.observableArrayList();
    private OnLimeDB onLimeDB = new OnLimeDB();

    private Server() throws IOException {
        serverSocket = new ServerSocket(3001);
    }

    public static Server getInstance() throws IOException {
        return server!=null? server:(server=new Server());
    }

    public void makeSocket(){
        while (!serverSocket.isClosed()){
            try{
                Socket socket = serverSocket.accept();

                // Get all usernames from the database and add them to the usersList
                List<String> allUsernames = onLimeDB.getAllUsernames();
                loggedInUsers.addAll(allUsernames);
                System.out.println("Logged in users: " + loggedInUsers);
                // print all usernames
                for (String username : allUsernames) {
                    System.out.println(username);
                }

                ClientHandler clientHandler = new ClientHandler(socket, clients, this);

                clients.add(clientHandler);
                System.out.println("client socket accepted "+socket.toString());
            } catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    public List<String> getAllUsernames() {
        System.out.println("Logged in users: " + loggedInUsers);
        return new ArrayList<>(loggedInUsers);
    }

    public ObservableList<String> getLoggedInUsers() {
        return loggedInUsers;
    }

    public void userLoggedIn(String username) {
        loggedInUsers.add(username);
        printLoggedInUsers();
    }

    public void printLoggedInUsers() {
        System.out.println("Logged-in users:");
        for (String username : loggedInUsers) {
            System.out.println(username);
        }
    }

    public void userLoggedOut(String username) {
        loggedInUsers.remove(username);
    }
}