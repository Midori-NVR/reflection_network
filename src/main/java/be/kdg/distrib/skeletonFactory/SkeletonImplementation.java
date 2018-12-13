package be.kdg.distrib.skeletonFactory;

import be.kdg.distrib.communication.MethodCallMessage;
import be.kdg.distrib.communication.NetworkAddress;
@Deprecated
public class SkeletonImplementation implements Skeleton {
    @Override
    public void run() {

    }

    @Override
    public NetworkAddress getAddress() {
        return null;
    }

    @Override
    public void handleRequest(MethodCallMessage message) {

    }
}
