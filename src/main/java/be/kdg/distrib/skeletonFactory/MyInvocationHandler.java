package be.kdg.distrib.skeletonFactory;

import be.kdg.distrib.communication.MessageManager;
import be.kdg.distrib.communication.NetworkAddress;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class MyInvocationHandler implements InvocationHandler {
    private Class impl;
    private NetworkAddress address;
    private MessageManager messageManager;

    public MyInvocationHandler(Class impl) {
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

        switch (method.getName()) {
            case "run":
                break;
            case "getAddress":
                return address;
            case "handleRequest":
                break;
        }

        return null;
    }
}
