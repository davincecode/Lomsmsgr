/*
 * Copyright (C) Vincent Ybanez 2023-Present
 * All Rights Reserved 2023
 */

package utils;

import database.OnLimeDB;
import javafx.beans.property.StringProperty;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.io.DataOutputStream;

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
            Label usernameLabel = new Label(username);
            HBox hbox = createButtons(username);
            hbox.getChildren().add(0, usernameLabel);
            setGraphic(hbox);
        }
    }

    /*
    * This method is used to create a horizontal box (HBox)
    * that contains three buttons: "Add Friend", "Add DM", and "Delete".
    */
    private HBox createButtons(String username) {
        ComboBox<String> actionsComboBox = new ComboBox<>();
        actionsComboBox.getItems().addAll("Add Friend", "Add DM", "Delete User");
        actionsComboBox.setPromptText("Select");

        actionsComboBox.setOnAction(event -> {
            String selectedAction = actionsComboBox.getSelectionModel().getSelectedItem();
            handleButtonClick(username, selectedAction);
        });

        // User status label
        ImageView statusImage = new ImageView();
        Image image = new Image(getClass().getResource("/img/online-dot.png").toExternalForm());
        statusImage.setImage(image);
        statusImage.setFitWidth(10);
        statusImage.setFitHeight(10);


        HBox hbox = new HBox(10);
        hbox.setAlignment(Pos.CENTER);
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        hbox.getChildren().addAll(statusImage, spacer, actionsComboBox);

        return hbox;
    }

    // Handle Add Friends, Add DM, and Delete button clicks
    private void handleButtonClick(String username, String selectedAction) {
        int userId = onLimeDB.getUserId(clientNameProperty.get());
        int friendId = onLimeDB.getUserId(username);

        switch (selectedAction) {
            case "Add Friend":
                System.out.println("Adding " + username + " as a friend.");
                handleAddAction(username, friendsList, userId, friendId, "friends");
                break;
            case "Add DM":
                System.out.println("Adding " + username + " to direct messages.");
                handleAddAction(username, directMessage, userId, friendId, "DMs");
                break;
            case "Delete User":
                // Remove the user from the ListView
                getListView().getItems().remove(username);

                // Delete the user from the database
                onLimeDB.deleteUser(username);

                // Create a new Alert
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("User Deleted");
                alert.setHeaderText(null);
                alert.setContentText(username + " has been deleted.");

                // Show the Alert and wait for the user to close it
                alert.showAndWait();
                break;
        }
    }

    private void handleAddAction(String username, ListView<String> listView, int userId, int friendId, String listType) {
        if (!listView.getItems().contains(username)) {
            listView.getItems().add(username);

            if ("friends".equals(listType)) {
                onLimeDB.addFriend(userId, friendId);
            } else if ("DMs".equals(listType)) {
                onLimeDB.addDM(userId, friendId);
            }

            // Alert to inform that the user has been added
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Information Dialog");
            alert.setHeaderText(null);
            alert.setContentText(username + " has been added to your " + listType + " list.");
            alert.showAndWait();
        } else {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Information Dialog");
            alert.setHeaderText(null);
            alert.setContentText(username + " is already in your " + listType + " list.");
            alert.showAndWait();
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