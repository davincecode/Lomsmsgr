package utils;

import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;

import java.io.DataOutputStream;
import java.io.IOException;

public class UserListCell extends ListCell<String> {

    private ListView<String> friendsList;
    private ListView<String> directMessage;
    private DataOutputStream dataOutputStream;

    public UserListCell(ListView<String> friendsList, ListView<String> directMessage, DataOutputStream dataOutputStream) {
        this.friendsList = friendsList;
        this.directMessage = directMessage;
        this.dataOutputStream = dataOutputStream;
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

    private HBox createButtons(String username) {
        Button addFriendButton = new Button("Add Friend");
        addFriendButton.setOnAction(event -> handleButtonClick(username, "Add Friend"));

        Button messageButton = new Button("Message");
        messageButton.setOnAction(event -> handleButtonClick(username, "Message"));

        HBox hbox = new HBox(10);
        hbox.getChildren().addAll(addFriendButton, messageButton);

        return hbox;
    }

    private void handleButtonClick(String username, String buttonLabel) {
        if ("Add Friend".equals(buttonLabel)) {
            System.out.println("Adding " + username + " as a friend.");
            if (!friendsList.getItems().contains(username)) {
                friendsList.getItems().add(username);
            }
        } else if ("Message".equals(buttonLabel)) {
            System.out.println("Sending a message to " + username);
            if (!directMessage.getItems().contains(username)) {
                directMessage.getItems().add(username);
            }
            try {
                dataOutputStream.writeUTF(username + ": " + "Your message here");
                dataOutputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}