package client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.List;

public class ClientHandler {
    private Socket socket;
    private List<ClientHandler> clients;
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;
    private String msg = "";
    private String username = "DefaultUsername";  // Initial username

    public ClientHandler(Socket socket, List<ClientHandler> clients) {
        try {
            this.socket = socket;
            this.clients = clients;
            this.dataInputStream = new DataInputStream(socket.getInputStream());
            this.dataOutputStream = new DataOutputStream(socket.getOutputStream());

            // Send the initial username to the client upon connection
            sendMessage("USERNAME:" + username);
        } catch (IOException e) {
            e.printStackTrace();
        }

        new Thread(() -> {
            try {
                while (socket.isConnected()) {
                    msg = dataInputStream.readUTF();

                    // Check if the received message is a username update
                    if (msg.startsWith("UPDATE_USERNAME:")) {
                        // Extract the new username
                        String newUsername = msg.substring("UPDATE_USERNAME:".length());

                        // Update the username and notify the client
                        updateUsername(newUsername);
                    } else {
                        // Broadcast the message to other clients
                        broadcastMessage(msg);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void updateUsername(String newUsername) {
        this.username = newUsername;
        // Send a message to the client indicating the username update
        sendMessage("USERNAME_UPDATED:" + newUsername);
    }

    private void broadcastMessage(String message) {
        for (ClientHandler clientHandler : clients) {
            if (clientHandler.socket.getPort() != socket.getPort()) {
                clientHandler.sendMessage(message);
            }
        }
    }

    private void sendMessage(String message) {
        try {
            dataOutputStream.writeUTF(message);
            dataOutputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
