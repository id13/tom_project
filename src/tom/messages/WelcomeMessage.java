package tom.messages;

public class WelcomeMessage extends Message {

    public WelcomeMessage(int logicalClock) {
      super(logicalClock, Message.WELCOME, "");
    }
    
    public WelcomeMessage(Message message) {
      super(message.getLogicalClock(), Message.WELCOME, message.getContent());
    }

    @Override
    public String toString() {
      return "Welcome message: LC: "+ getLogicalClock();
    }
}
