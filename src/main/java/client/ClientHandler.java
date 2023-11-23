package client;

import controller.ClientFormController;
import javafx.application.Platform;
import javafx.scene.layout.VBox;

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
    private String clientName;
    private VBox vBox;
    private ClientFormController clientFormController;

    public ClientHandler(Socket socket, List<ClientHandler> clients, VBox vBox, ClientFormController clientFormController) {
        try {
            this.socket = socket;
            this.clients = clients;
            this.vBox = vBox;
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
                        if (parts.length == 3) {
                            // Direct message
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

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }
}