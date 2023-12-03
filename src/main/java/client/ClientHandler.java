package client;

import server.Server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.util.List;

public class ClientHandler {
    private Socket socket;
    private List<ClientHandler> clients;
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;
    private String msg = "";
    private String username;
    private Server server;

    public ClientHandler(Socket socket, List<ClientHandler> clients, Server server) {
        try {
            this.server = server;
            this.socket = socket;
            this.clients = clients;
            this.dataInputStream = new DataInputStream(socket.getInputStream());
            this.dataOutputStream = new DataOutputStream(socket.getOutputStream());
            server.userLoggedIn(username);
        } catch (IOException e) {
            e.printStackTrace();
        }

        new Thread(() -> {
            try {
                while (socket.isConnected()) {
                    try {
                        msg = dataInputStream.readUTF();
                        for (ClientHandler clientHandler : clients) {
                            if (clientHandler.socket.getPort() != socket.getPort()) {
                                clientHandler.dataOutputStream.writeUTF(msg);
                                clientHandler.dataOutputStream.flush();
                            }
                        }
                    } catch (EOFException e) {
                        // Handle EOFException (client disconnected)
                        handleClientDisconnect();
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    // Handle client disconnect
    private void handleClientDisconnect() {
        System.out.println("Client disconnected: " + socket);
        clients.remove(this);
        server.userLoggedOut(username);
    }

    // Send a message to the client to update their friendsList
    public void updateFriendsList(String newFriendUsername) throws IOException {
        // Send a message to the client to update their friendsList
        dataOutputStream.writeUTF("UPDATE_FRIENDS_LIST-" + newFriendUsername);
    }
}