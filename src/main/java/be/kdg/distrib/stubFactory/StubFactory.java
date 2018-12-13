package be.kdg.distrib.stubFactory;

import be.kdg.distrib.communication.NetworkAddress;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

public class StubFactory {
    public static Object createStub(Class interfaceClass, String s, int port) {
        InvocationHandler handler = new StubInvocationHandler(new NetworkAddress(s,port));
        Object result = Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class[]{interfaceClass}, handler);
        return result;
    }
}
