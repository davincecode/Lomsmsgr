package utils;

import database.OnLimeDB;
import javafx.beans.property.StringProperty;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.DataOutputStream;
import java.io.IOException;

public class UserListCell extends ListCell<String> {

    private ListView<String> friendsList;
    private ListView<String> directMessage;
    private DataOutputStream dataOutputStream;
    private TextField txtMsg;
    private TextField txtMsgFriends;
    private TextField txtMsgDM;
    private VBox vBox;
    private VBox vBoxFriends;
    private VBox vBoxDM;
    private TabPane tabPane;
    private Tab home;
    private Tab addFriends;
    private Tab directMsg;
    private StringProperty clientNameProperty;
    private OnLimeDB onLimeDB;

    public UserListCell(ListView<String> friendsList, ListView<String> directMessage, DataOutputStream dataOutputStream, TextField txtMsg, TextField txtMsgFriends, TextField txtMsgDM, VBox vBox, VBox vBoxFriends, VBox vBoxDM, TabPane tabPane, Tab home, Tab addFriends, Tab directMsg, StringProperty clientNameProperty, OnLimeDB onLimeDB) {
        this.friendsList = friendsList;
        this.directMessage = directMessage;
        this.dataOutputStream = dataOutputStream;
        this.txtMsg = txtMsg;
        this.txtMsgFriends = txtMsgFriends;
        this.txtMsgDM = txtMsgDM;
        this.vBox = vBox;
        this.vBoxFriends = vBoxFriends;
        this.vBoxDM = vBoxDM;
        this.tabPane = tabPane;
        this.home = home;
        this.addFriends = addFriends;
        this.directMsg = directMsg;
        this.clientNameProperty = clientNameProperty;
        this.onLimeDB = onLimeDB;
    }
    
    

    @Override
    protected void updateItem(String username, boolean empty) {
        super.updateItem(username, empty);

        if (empty || username == null) {
            setText(null);
            setGraphic(null);
        } else {
            setText(username);
            setGraphic(createButtons(username));
        }
    }

    /*
    * This method is used to create a horizontal box (HBox)
    * that contains three buttons: "Add Friend", "Message", and "Delete".
    */
    private HBox createButtons(String username) {
        Button addFriendButton = new Button("Add Friend");
        addFriendButton.setOnAction(event -> handleButtonClick(username, "Add Friend"));

        Button messageButton = new Button("Message");
        messageButton.setOnAction(event -> handleButtonClick(username, "Message"));

        Button deleteButton = new Button("Delete");
        deleteButton.setOnAction(event -> {
            int messageId = onLimeDB.getUserId(username);
            int senderId = onLimeDB.getUserId(clientNameProperty.get());
            onLimeDB.deleteBroadcastMessage(messageId, senderId);
            getListView().getItems().remove(getItem());
            System.out.println("Deleted " + messageId);
        });

        HBox hbox = new HBox(10);
        hbox.getChildren().addAll(addFriendButton, messageButton, deleteButton); // Add deleteButton here

        return hbox;
    }

    private void handleButtonClick(String username, String buttonLabel) {
        // Get the message from the txtMsg text field
        String message = txtMsg.getText();
        // Get the message from the txtMsgFriends text field
        String messageFriends = txtMsgFriends.getText();
        // Get the message from the txtMsgDM text field
        String messageDM = txtMsgDM.getText();

        if ("Add Friend".equals(buttonLabel)) {
            System.out.println("Adding " + username + " as a friend.");
            if (!friendsList.getItems().contains(username)) {
                friendsList.getItems().add(username);
            }
        } else if ("Message".equals(buttonLabel)) {
            // Determine the destination container based on the selected user
            VBox destinationVBox = getDestinationVBox();

            // Add the sender's name to the message
            Label senderLabel = new Label(clientNameProperty.get());
            destinationVBox.getChildren().add(senderLabel);

            // Add the message to the appropriate container
            Label messageLabel = new Label(message);
            destinationVBox.getChildren().add(messageLabel);

            // If a message is entered, send it to each friend
            for (String friend : friendsList.getItems()) {
                System.out.println("Sending a message to " + friend);
                if (!directMessage.getItems().contains(friend)) {
                    directMessage.getItems().add(friend);
                }
                try {
                    // Use the appropriate message based on the selected tab
                    Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();
                    String messageToSend = selectedTab == home ? message : selectedTab == addFriends ? messageFriends : messageDM;

                    dataOutputStream.writeUTF(clientNameProperty.get() + ": " + messageToSend);
                    dataOutputStream.flush();
                    System.out.println("Message sent to server for user: " + username);


                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private VBox getDestinationVBox() {
        // Get the currently selected tab
        Tab selectedTab = tabPane.getSelectionModel().getSelectedItem();

        // Determine the destination VBox based on the selected tab
        if (selectedTab == home) {
            return vBox;
        } else if (selectedTab == addFriends) {
            return vBoxFriends;
        } else if (selectedTab == directMsg) {
            return vBoxDM;
        } else {
            // Default to vBox if no tab is selected
            return vBox;
        }
    }
}