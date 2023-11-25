// New
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
    private String clientName;
    public VBox vBox;
    private VBox vBoxDM;
    private ClientFormController clientFormController;
    private List<ClientHandler> allClients;
    public String getClientName() {
        return this.clientName;
    }

    public ClientHandler(Socket socket, List<ClientHandler> clients, VBox vBox, ClientFormController clientFormController, String clientName) {
        try {
            this.socket = socket;
            this.clients = clients;
            this.vBox = vBox;
            this.vBoxDM = vBoxDM;
            this.clientName = clientName;
            this.allClients = allClients;
            this.clientFormController = clientFormController;
            this.dataInputStream = new DataInputStream(socket.getInputStream());
            this.dataOutputStream = new DataOutputStream(socket.getOutputStream());
        }catch (IOException e){
            e.printStackTrace();
        }

        new Thread(new Runnable() {
            private String clientName;

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
                String[] parts = receivingMsg.split("-");
                if (parts.length >= 3) {
                    // Direct message
                    String senderName = parts[0];
                    String receiverName = parts[1];
                    String msg = parts[2];
                    if (clientName.equals(receiverName)) {
                        // This client is the receiver of the message
                        Platform.runLater(() -> {
                            try {
                                clientFormController.receiveMessage(senderName + ": " + msg, clientFormController.vBoxDM);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
                    }
                } else if (parts.length == 2 && parts[1].equals("BROADCAST")) {
                    // Broadcast message
                    String senderName = parts[0];
                    String msg = parts[1];
                    Platform.runLater(() -> {
                        try {
                            clientFormController.receiveMessage(senderName + ": " + msg, clientFormController.vBox);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
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
            if (message.contains("BROADCAST-")) {
                // Broadcast message
                String[] parts = message.split("-");
                String senderName = parts[0]; // Extract the sender's name
                String msg = parts[2]; // Extract the message
                try {
                    ClientFormController.receiveMessage(senderName + ": " + msg, vBox);
                } catch (IOException e) {
                    throw new RuntimeException(e);
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
