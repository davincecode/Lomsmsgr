package server;

import client.ClientHandler;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server {
    private ServerSocket serverSocket;
    private Socket socket;
    private static Server server;
    private ObservableList<String> loggedInUsers = FXCollections.observableArrayList();
    private List<ClientHandler> clients = new ArrayList<>();

    public Server() throws IOException {
        serverSocket = new ServerSocket(3001);

        loggedInUsers.addListener(new ListChangeListener<String>() {
            @Override
            public void onChanged(Change<? extends String> change) {
                while (change.next()) {
                    if (change.wasAdded()) {
                        // handle user logged in
                    } else if (change.wasRemoved()) {
                        // handle user logged out
                    }
                }
            }
        });
    }

    public static Server getInstance() throws IOException {
        return server!=null? server:(server=new Server());
    }

    public void makeSocket(){
        while (!serverSocket.isClosed()){
            try{
                socket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(socket,clients);
                clients.add(clientHandler);
                System.out.println("client socket accepted "+socket.toString());
            } catch (IOException e){
                e.printStackTrace();
            }
        }
    }

    public void userLoggedIn(String username) {
        loggedInUsers.add(username);
        printLoggedInUsers();
    }

    public void userLoggedOut(String username) {
        loggedInUsers.remove(username);
        printLoggedInUsers();
    }

    public ObservableList<String> getLoggedInUsers() {
        return loggedInUsers;
    }

    public void printLoggedInUsers() {
        System.out.println("Logged-in users:");
        for (String username : loggedInUsers) {
            System.out.println(username);
        }
    }
}