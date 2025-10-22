package leap.droidcord;

public class Message {
    // You will need to add fields that correspond to the data
    // you get from your API for a message (e.g., author, timestamp, content).
    private String content;
    private String author;
    // Add other fields as needed, for example:
    // private long id;
    // private long timestamp;

    // A constructor is helpful for creating new Message objects.
    public Message(String author, String content) {
        this.author = author;
        this.content = content;
    }

    // This method is required by your ChatActivity.
    public String getContent() {
        return content;
    }

    // This method is also required by your ChatActivity.
    public void setContent(String content) {
        this.content = content;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    // Override toString() to control how the object is displayed,
    // which is useful for adapters.
    @Override
    public String toString() {
        return author + ": " + content;
    }
}
