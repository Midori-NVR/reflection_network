package be.kdg.distrib.skeletonFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

public class SkeletonFactory {
    public static Object createSkeleton(Object impl) {
        InvocationHandler handler = new MyInvocationHandler(impl.getClass());
        Object result = Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class[]{Skeleton.class}, handler);
        return result;
    }
}
