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
        Map<String, String> parameters = reply.getParameters();
        int variableCount = parameters.size();
        Object test = null;
        try {
            test = object.getDeclaredConstructor().newInstance();
        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            e.printStackTrace();
        }
        for (String param : parameters.keySet()) {
            try {
                Field temp = object.getDeclaredField(param.substring(7));
                temp.setAccessible(true);
                temp.set(test, convertParameter(parameters.get(param), temp.getType()));
                temp.setAccessible(false);
            } catch (IllegalAccessException | NoSuchFieldException e) {
                e.printStackTrace();
            }
        }
        return test;
    }

    private Object convertParameter(String parameter, Class type) {
        if (type.isPrimitive()) {
            if (type == int.class)
                return Integer.valueOf(parameter);
            else if (type == char.class)
                return parameter.charAt(0);
            else if (type == boolean.class)
                return Boolean.valueOf(parameter);
        } else {
            if (type == String.class)
                return parameter;
        }
        return null;
    }

    private boolean isNotObject(Object object) {
        Class type = object.getClass();
        return type.isPrimitive() || type == String.class || type == Integer.class || type == Double.class || type == Boolean.class || type == Character.class;
    }
}
