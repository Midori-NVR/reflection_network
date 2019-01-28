package be.kdg.distrib.stubFactory;

import be.kdg.distrib.communication.MessageManager;
import be.kdg.distrib.communication.MethodCallMessage;
import be.kdg.distrib.communication.NetworkAddress;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class StubInvocationHandler implements InvocationHandler {
    private NetworkAddress serverAddress;
    private MessageManager messageManager;

    public StubInvocationHandler(NetworkAddress networkAddress) {
        messageManager = new MessageManager();
        this.serverAddress = networkAddress;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        System.out.println("invoke op skeleton");
        System.out.println("\tmethodName = " + method.getName());

        System.out.println("\treturn = " + method.getReturnType());

        if (args != null) {
            for (Object arg : args) {
                System.out.println("\targ = " + arg);
            }
        }

        MethodCallMessage callMessage = new MethodCallMessage(messageManager.getMyAddress(), method.getName());

        if (args != null) {
            int count = 0;
            for (Object arg : args) {
                if (isNotObject(arg)) {
                    callMessage.setParameter("arg" + count++, arg.toString());
                } else {
                    for (Method getter : arg.getClass().getDeclaredMethods()) {
                        if (getter.getName().startsWith("get")) {
                            callMessage.setParameter("arg" + count + "." + getter.getName().substring(3).toLowerCase(), getter.invoke(arg).toString());
                        } else if (getter.getName().startsWith("is")) {
                            callMessage.setParameter("arg" + count + "." + getter.getName().substring(2).toLowerCase(), getter.invoke(arg).toString());
                        }
                    }
                    count++;
                }
            }
        }
        messageManager.send(callMessage, serverAddress);
        Class returnType = method.getReturnType();
        if (returnType == void.class)
            checkEmptyReply();
        else if (returnType.isPrimitive() || returnType == String.class)
            return checkReply();
        else
            return checkObjectReply();
        return null;
    }

    private void checkEmptyReply() {
        String value = "";
        while (!"Ok".equals(value)) {
            MethodCallMessage reply = messageManager.wReceive();
            if (!"result".equals(reply.getMethodName())) {
                continue;
            }
            value = reply.getParameter("result");
        }
        System.out.println("OK");
    }

    private Object checkReply() {
        MethodCallMessage reply = messageManager.wReceive();
        while (!"result".equals(reply.getMethodName())) {
            reply = messageManager.wReceive();
        }
        return reply.getParameter("result");
    }

    private Object checkObjectReply() {
        MethodCallMessage reply = messageManager.wReceive();
        while (!"result".equals(reply.getMethodName())) {//TODO
            reply = messageManager.wReceive();
        }
        return reply.getParameter("result");
    }

    private boolean isNotObject(Object object) {
        if (object.getClass().isPrimitive() || object.getClass().equals(String.class)) {
            return true;
        } else {
            return false;
        }
    }
}
