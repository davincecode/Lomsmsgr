package database;

public class Message {
    private int id;
    private String text;
    private int senderId;

    public Message(int id, String text) {
        this.id = id;
        this.text = text;
    }

    public Message(int id, String text, int senderId) {
        this(id, text);
        this.senderId = senderId;
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
}