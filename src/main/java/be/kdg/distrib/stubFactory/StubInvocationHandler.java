package be.kdg.distrib.stubFactory;

import be.kdg.distrib.communication.MessageManager;
import be.kdg.distrib.communication.MethodCallMessage;
import be.kdg.distrib.communication.NetworkAddress;

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.Map;

public class StubInvocationHandler implements InvocationHandler {
    private NetworkAddress serverAddress;
    private MessageManager messageManager;

    public StubInvocationHandler(NetworkAddress networkAddress) {
        messageManager = new MessageManager();
        this.serverAddress = networkAddress;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        //PRINTS
        System.out.println("invoke op skeleton");
        System.out.println("\tmethodName = " + method.getName());
        System.out.println("\treturnType = " + method.getReturnType());

        if (args != null) {
            for (Object arg : args) {
                System.out.println("\targ = " + arg);
            }
        }

        MethodCallMessage callMessage = new MethodCallMessage(messageManager.getMyAddress(), method.getName());

        //doing parameters
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

        //doing returns
        Class returnType = method.getReturnType();
        if (returnType == void.class)
            checkEmptyReply();
        else if (returnType == String.class)
            return checkReply();
        else if (returnType.isPrimitive()) {
            if (returnType == int.class)
                return Integer.valueOf(checkReply());
            else if (returnType == char.class)
                return checkReply().charAt(0);
            else if (returnType == boolean.class)
                return Boolean.valueOf(checkReply());
            else
                throw new Exception(returnType + ": Primitive type not implemented");
        } else
            return checkObjectReply(returnType);
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

    private String checkReply() {
        MethodCallMessage reply = messageManager.wReceive();
        while (!"result".equals(reply.getMethodName())) {
            reply = messageManager.wReceive();
        }
        return reply.getParameter("result");
    }

    private Object checkObjectReply(Class object) {
        MethodCallMessage reply = messageManager.wReceive();
        while (!"result".equals(reply.getMethodName())) {
            reply = messageManager.wReceive();
        }
        Map<String,String> parameters = reply.getParameters();
        int variableCount = parameters.size();
        Constructor con = Arrays.stream(object.getConstructors())
                .filter(constructor -> constructor.getParameterCount() == variableCount)
                .filter(constructor -> Arrays.stream(constructor.getParameters()).allMatch(parameter -> parameters.containsKey("result." + parameter.getName())))
                .findFirst().get();
        try {
            //TODO convert to right types and right order with names
            con.newInstance(parameters.values());
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    private boolean isNotObject(Object object) {
        if (object.getClass().isPrimitive() || object.getClass().equals(String.class)) {
            return true;
        } else {
            return false;
        }
    }
}
