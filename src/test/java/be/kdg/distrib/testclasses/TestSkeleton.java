package be.kdg.distrib.testclasses;

import be.kdg.distrib.communication.MessageManager;
import be.kdg.distrib.communication.MethodCallMessage;
import be.kdg.distrib.communication.NetworkAddress;

public class TestSkeleton implements Runnable {
    private MessageManager messageManager;
    private MethodCallMessage message;
    private String returnValue;

    public TestSkeleton() {
        this.returnValue = "Ok";
        this.messageManager = new MessageManager();
    }

    public void run() {
        MethodCallMessage message = messageManager.wReceive();
        this.message = message;
        message.getParameters().forEach((o,i) -> System.out.println(o + " | " + i));
        MethodCallMessage reply = new MethodCallMessage(getMyAddress(), "result");
        if (!"object".equals(returnValue)) {
            reply.setParameter("result", returnValue);
        } else {
            reply.setParameter("result.name", "bloop");
            reply.setParameter("result.age", "123");
            reply.setParameter("result.gender", "r");
            reply.setParameter("result.deleted", "true");
        }
        messageManager.send(reply, message.getOriginator());
    }

    public void start() {
        Thread thread = new Thread(this);
        thread.start();
    }

    public NetworkAddress getMyAddress() {
        return messageManager.getMyAddress();
    }

    public MethodCallMessage getMessage() {
        return message;
    }

    public void sendStringReturnValue() {
        returnValue = "forty-two";
    }

    public void sendIntReturnValue() {
        returnValue = "42";
    }

    public void sendCharReturnValue() {
        returnValue = "K";
    }

    public void sendBooleanReturnValue() {
        returnValue = "true";
    }

    public void sendObjectReturnValue() {
        returnValue = "object";
    }
}
