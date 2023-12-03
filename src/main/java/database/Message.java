/*
 * Copyright (C) Vincent Ybanez 2023-Present
 * All Rights Reserved 2023
 */
package database;

public class Message {
    private int id;
    private String text;
    private int senderId;
    private String senderUsername;

    public Message(int id, String text) {
        this.id = id;
        this.text = text;
    }

    public Message(int id, String text, int senderId) {
        this(id, text);
        this.senderId = senderId;
    }

    public Message(int id, String text, int senderId, String senderUsername) {
        this(id, text, senderId);
        this.senderUsername = senderUsername;
    }

    public int getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public int getMessageId() {
        return id;
    }

    public int getSenderId() {
        return senderId;
    }

    public String getSenderUsername() {
        return senderUsername;
    }
}