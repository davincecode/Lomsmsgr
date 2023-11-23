package controller;

import com.jfoenix.controls.JFXListView;
import database.OnLimeDB;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import server.Server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.sql.Timestamp;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class ClientFormController implements Initializable {
    public Text txtLabel;
    public Text txtLabel1;
    public AnchorPane pane;
    public ScrollPane scrollPain;
    public VBox vBox;
    public TextField txtMsg;
    public JFXListView<String> usersList;


    private Socket socket;
    private DataInputStream dataInputStream;
    private DataOutputStream dataOutputStream;
    private String clientName = "Client";
    private Server server;
    private OnLimeDB databaseConnector;
    private String receiverUsername;

    public void updateUsersList() {
        List<String> loggedInUsers = server.getLoggedInUsers();
        usersList.getItems().clear();
        usersList.getItems().addAll(loggedInUsers);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        txtLabel.setText(clientName);
        txtLabel1.setText(clientName);

        databaseConnector = new OnLimeDB();
//        User selectedUser = getSelectedUser();
//        receiverUsername = selectedUser.getUsername();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    socket = new Socket("localhost", 3001);
                    dataInputStream = new DataInputStream(socket.getInputStream());
                    dataOutputStream = new DataOutputStream(socket.getOutputStream());
                    System.out.println("Client connected");
                    ServerFormController.receiveMessage(clientName+" joined.");

                    while (socket.isConnected()){
                        String receivingMsg = dataInputStream.readUTF();
                        receiveMessage(receivingMsg, ClientFormController.this.vBox);
                    }
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }).start();

        this.vBox.heightProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number oldValue, Number newValue) {
                scrollPain.setVvalue((Double) newValue);
            }
        });

        try {
            server = Server.getInstance();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        updateUsersList();

    }

    public void shutdown() {
        // cleanup code here...
        ServerFormController.receiveMessage(clientName+" left.");
    }

    public void txtMsgOnAction(ActionEvent actionEvent) {
        sendButtonOnAction(actionEvent);
    }

    public void sendButtonOnAction(ActionEvent actionEvent) {
//        String receiverUsername = databaseConnector.getReceiverUsername(clientName);
        System.out.println("Sender: " + clientName); // Debug print
        System.out.println("Receiver: " + receiverUsername); // Debug print
        sendMsg(txtMsg.getText(), receiverUsername);
    }


    private void sendMsg(String msgToSend, String receiverUsername) {
        if (!msgToSend.isEmpty()){
            HBox hBox = new HBox();
            hBox.setAlignment(Pos.CENTER_RIGHT);
            hBox.setPadding(new Insets(5, 5, 0, 10));

            Text text = new Text(msgToSend);
            text.setStyle("-fx-font-size: 14");
            TextFlow textFlow = new TextFlow(text);

            textFlow.setStyle("-fx-background-color: #0693e3; -fx-font-weight: bold; -fx-color: white; -fx-background-radius: 20px");
            textFlow.setPadding(new Insets(5, 10, 5, 10));
            text.setFill(Color.color(1, 1, 1));

            hBox.getChildren().add(textFlow);

            HBox hBoxTime = new HBox();
            hBoxTime.setAlignment(Pos.CENTER_RIGHT);
            hBoxTime.setPadding(new Insets(0, 5, 5, 10));
            String stringTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
            Text time = new Text(stringTime);
            time.setStyle("-fx-font-size: 8");

            hBoxTime.getChildren().add(time);

            vBox.getChildren().add(hBox);
            vBox.getChildren().add(hBoxTime);

            try {
                dataOutputStream.writeUTF(clientName + "-" + msgToSend);
                dataOutputStream.flush();

                // Store the message in the database
                Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                databaseConnector.storeMessageInDB(clientName, receiverUsername, msgToSend, timestamp);
            } catch (IOException e) {
                e.printStackTrace();
            }

            txtMsg.clear();
        }
    }

    public static void receiveMessage(String msg, VBox vBox) throws IOException {
        String name = msg.split("-")[0];
        String msgFromServer = msg.split("-")[1];

        HBox hBox = new HBox();
        hBox.setAlignment(Pos.CENTER_LEFT);
        hBox.setPadding(new Insets(5,5,5,10));

        HBox hBoxName = new HBox();
        hBoxName.setAlignment(Pos.CENTER_LEFT);
        Text textName = new Text(name);
        TextFlow textFlowName = new TextFlow(textName);
        hBoxName.getChildren().add(textFlowName);

        Text text = new Text(msgFromServer);
        TextFlow textFlow = new TextFlow(text);
        textFlow.setStyle("-fx-background-color: #abb8c3; -fx-font-weight: bold; -fx-background-radius: 20px");
        textFlow.setPadding(new Insets(5,10,5,10));
        text.setFill(Color.color(0,0,0));

        hBox.getChildren().add(textFlow);

        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                vBox.getChildren().add(hBoxName);
                vBox.getChildren().add(hBox);
            }
        });
    }

    public void setClientName(String name) {
        clientName = name;
    }

}
