package utils;

import javafx.scene.control.Button;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;

public class UserListCell extends ListCell<String> {

    private ListView<String> friendsList;
    private ListView<String> directMessage;

    public UserListCell(ListView<String> friendsList, ListView<String> directMessage) {
        this.friendsList = friendsList;
        this.directMessage = directMessage;
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
        // Handle the button click for the specified username and button label
        if ("Add Friend".equals(buttonLabel)) {
            // Perform actions for Add Friend button
            System.out.println("Adding " + username + " as a friend.");
            friendsList.getItems().add(username);
        } else if ("Message".equals(buttonLabel)) {
            // Perform actions for Message button
            System.out.println("Sending a message to " + username);
            directMessage.getItems().add(username);
        }
    }
}