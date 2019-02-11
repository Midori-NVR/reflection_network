package be.kdg.distrib.skeletonFactory;

import be.kdg.distrib.communication.MessageManager;
import be.kdg.distrib.communication.MethodCallMessage;
import be.kdg.distrib.communication.NetworkAddress;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

public class MyInvocationHandler implements InvocationHandler {
    private Object impl;
    private NetworkAddress address;
    private MessageManager messageManager;

    public MyInvocationHandler(Object impl) {
        this.impl = impl;
        messageManager = new MessageManager();
        address = messageManager.getMyAddress();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println("invoke op skeleton");
        System.out.println("\tmethodName = " + method.getName());

        if (args != null) {
            for (Object arg : args) {
                System.out.println("\targ = " + arg);
            }
        }

        if ("run".equals(method.getName())) {
            new Thread(this::run).start();
        } else if ("getAddress".equals(method.getName())) {
            return address;
        } else if ("handleRequest".equals(method.getName())) {
            MethodCallMessage message = (MethodCallMessage) args[0];
            handleRequest(message);
        }

        return null;
    }

    private void sendEmptyReply(MethodCallMessage request) {
        MethodCallMessage reply = new MethodCallMessage(messageManager.getMyAddress(), "result");
        reply.setParameter("result", "Ok");
        messageManager.send(reply, request.getOriginator());
        System.out.println("OK");
    }

    private void run(){
        while (true) {
            MethodCallMessage request = messageManager.wReceive();
            try {
                handleRequest(request);
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleRequest(MethodCallMessage message) throws NoSuchMethodException {
        //TODO check for null result vv
        Method implMethod = Arrays.stream(impl.getClass().getDeclaredMethods()).filter(method -> method.getParameterCount() == message.getParameters().size()).filter(method -> method.getName() == message.getMethodName()).findFirst().get();
        try {
            implMethod.invoke(impl, message.getParameters().values().toArray());
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        sendEmptyReply(message);
    }
}
