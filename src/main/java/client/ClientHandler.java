package client;

import controller.ClientFormController;
import controller.ServerFormController;
import javafx.application.Platform;
import javafx.scene.layout.VBox;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.List;

public class ClientHandler {
    private Socket socket;
    public List<ClientHandler> clients;
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;
    private String msg = "";
    private VBox vBox;
    private ClientFormController clientFormController;
    private List<ClientHandler> allClients;
    private String clientName;
    public String getClientName() {
        return this.clientName;
    }

    public ClientHandler(Socket socket, List<ClientHandler> clients, VBox vBox, ClientFormController clientFormController, String clientName) {
        try {
            this.socket = socket;
            this.clients = clients;
            this.vBox = vBox;
            this.clientName = clientName;
            this.allClients = allClients;
            this.clientFormController = clientFormController;
            this.dataInputStream = new DataInputStream(socket.getInputStream());
            this.dataOutputStream = new DataOutputStream(socket.getOutputStream());
        }catch (IOException e){
            e.printStackTrace();
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    while (socket.isConnected()){
                        String receivingMsg = dataInputStream.readUTF();
                        String[] parts = receivingMsg.split("-");
                        if (parts.length >= 3) {
                            // Direct message
                            String senderName = parts[0];
                            String receiverName = parts[1];
                            String msg = parts[2];
                            if (clientName != null && clientName.equals(receiverName)) {
                                // This client is the receiver of the message
                                Platform.runLater(() -> {
                                    try {
                                        clientFormController.receiveMessage(senderName + ": " + msg, vBox);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                });
                            }
                        } else if (parts.length == 2) {
                            // Broadcast message
                            Platform.runLater(() -> {
                                try {
                                    clientFormController.receiveMessage(receivingMsg, vBox);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            });
                        }
                    }
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }).start();

    }

    public void run() {
        try {
            while (socket.isConnected()) {
                String receivingMsg = dataInputStream.readUTF();
                if (receivingMsg.startsWith("BROADCAST-")) {
                    // Broadcast message
                    String msg = receivingMsg.substring(10);
                    String senderName = msg.split("-")[0];
                    String actualMsg = msg.substring(senderName.length() + 1); // Remove the sender's name from the message
                    Platform.runLater(() -> {
                        try {
                            clientFormController.receiveMessage(senderName + ": " + actualMsg, vBox);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                } else {
                    // Direct message
                    String[] parts = receivingMsg.split("-");
                    if (parts.length == 3) {
                        String senderName = parts[0];
                        String receiverName = parts[1];
                        String msg = parts[2];
                        if (clientName.equals(receiverName)) {
                            // This client is the receiver of the message
                            Platform.runLater(() -> {
                                try {
                                    clientFormController.receiveMessage(senderName + ": " + msg, vBox);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            });
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public void receiveMessage(String message) {
        System.out.println("Received message: " + message); // Print the received message
        System.out.println("vBox before: " + vBox.getChildren()); // Print the state of vBox before adding the message

        Platform.runLater(() -> {
            if (message.startsWith("BROADCAST-")) {
                // Broadcast message
                String msg = message.substring(10); // Remove "BROADCAST-" from the start of the message
                String senderName = msg.split("-")[0]; // Extract the sender's name
                String actualMsg = msg.substring(senderName.length() + 1); // Remove the sender's name from the message
                ServerFormController.receiveMessage(senderName + ": " + actualMsg);
                for (ClientHandler client : allClients) {
                    try {
                        ClientFormController.receiveMessage(senderName + ": " + actualMsg, client.vBox);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            } else {
                // Direct message
                ServerFormController.receiveMessage(message);
                try {
                    ClientFormController.receiveMessage(message, vBox);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        System.out.println("vBox after: " + vBox.getChildren()); // Print the state of vBox after adding the message
    }
}