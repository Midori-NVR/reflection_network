package be.kdg.distrib.skeletonFactory;

import be.kdg.distrib.communication.MessageManager;
import be.kdg.distrib.communication.MethodCallMessage;
import be.kdg.distrib.communication.NetworkAddress;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.OptionalInt;

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


    private void run() {
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
        int parameterCount = 0;
        if(message.getParameters().keySet().stream().allMatch(s -> s.matches("arg\\d*.*"))) {
            OptionalInt parameterCountOpt =
                    message.getParameters().keySet().stream().mapToInt(s -> Integer.parseInt(s.indexOf('.') == -1 ? s.substring(3): s.substring(3, s.indexOf('.')))).max();

            if (parameterCountOpt.isPresent()) parameterCount = parameterCountOpt.getAsInt() + 1;
        }
        int finalParameterCount = parameterCount;
        Method implMethod = Arrays.stream(impl.getClass().getDeclaredMethods()).filter(method -> method.getParameterCount() == finalParameterCount).filter(method -> method.getName().equals(message.getMethodName())).findFirst().get();
        try {
            Class[] argumentTypes = implMethod.getParameterTypes();
            Object[] argumentsConverted = new Object[implMethod.getParameterCount()];

            for (int i = 0; i < argumentTypes.length; i++) {
                Class type = argumentTypes[i];
                if (isNotObjectClass(type)) {
                    String param = message.getParameter("arg" + i);
                    if (param == null) throw new RuntimeException();
                    argumentsConverted[i] = convertParameter(param, type);
                } else {
                    Object test = null;
                    try {
                        test = type.getDeclaredConstructor().newInstance();
                    } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
                        e.printStackTrace();
                    }
                    Map<String, String> objectParameters = message.getParametersStartingWith("arg" + i + ".");
                    for (String param : objectParameters.keySet()) {
                        try {
                            Field temp = type.getDeclaredField(param.substring(5));
                            temp.setAccessible(true);
                            temp.set(test, convertParameter(objectParameters.get(param), temp.getType()));
                            temp.setAccessible(false);
                        } catch (IllegalAccessException | NoSuchFieldException e) {
                            e.printStackTrace();
                        }
                    }
                    argumentsConverted[i] = test;
                }
            }

            Object returnedObject = implMethod.invoke(impl, argumentsConverted);
            Class returnType = implMethod.getReturnType();
            if (returnType == void.class)
                sendEmptyReply(message);
            else if (isNotObjectClass(returnType))
                sendStringReply(message, returnedObject);
            else
                sendObjectReply(message, returnedObject);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }


    }

    private void sendObjectReply(MethodCallMessage request, Object returnedObject) throws InvocationTargetException, IllegalAccessException {
        MethodCallMessage reply = new MethodCallMessage(messageManager.getMyAddress(), "result");
        for (Method getter : returnedObject.getClass().getDeclaredMethods()) {
            if (getter.getName().startsWith("get")) {
                reply.setParameter("result." + getter.getName().substring(3).toLowerCase(), getter.invoke(returnedObject).toString());
            } else if (getter.getName().startsWith("is")) {
                reply.setParameter("result." + getter.getName().substring(2).toLowerCase(), getter.invoke(returnedObject).toString());
            }
        }
        messageManager.send(reply, request.getOriginator());
        System.out.println("OK");
    }

    private void sendStringReply(MethodCallMessage request, Object returnedObject) {
        MethodCallMessage reply = new MethodCallMessage(messageManager.getMyAddress(), "result");
        reply.setParameter("result", returnedObject.toString());
        messageManager.send(reply, request.getOriginator());
        System.out.println("OK");
    }

    private void sendEmptyReply(MethodCallMessage request) {
        MethodCallMessage reply = new MethodCallMessage(messageManager.getMyAddress(), "result");
        reply.setParameter("result", "Ok");
        messageManager.send(reply, request.getOriginator());
        System.out.println("OK");
    }

    private Object convertParameter(String parameter, Class type) {
        if (type.isPrimitive()) {
            if (type == int.class)
                return Integer.valueOf(parameter);
            else if (type == char.class)
                return parameter.charAt(0);
            else if (type == boolean.class)
                return Boolean.valueOf(parameter);
            else if (type == double.class)
                return Double.valueOf(parameter);
        } else {
            if (type == String.class)
                return parameter;
        }
        return null;
    }

    private boolean isNotObjectClass(Class type) {
        return type.isPrimitive() || type == String.class || type == Integer.class || type == Double.class || type == Boolean.class || type == Character.class;
    }
}
