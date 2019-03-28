package be.kdg.distrib;

import be.kdg.distrib.skeletonFactory.Skeleton;
import be.kdg.distrib.skeletonFactory.SkeletonFactory;
import be.kdg.distrib.communication.MessageManager;
import be.kdg.distrib.communication.MethodCallMessage;
import be.kdg.distrib.communication.NetworkAddress;
import be.kdg.distrib.testclasses.TestImplementation;
import org.junit.Before;
import org.junit.Test;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestSkeletonFactory {
    private TestImplementation testImplementation;
    private Skeleton skeleton;
    private MessageManager messageManager;
    private NetworkAddress myAddress;

    @Before
    public void setup() {
        testImplementation = new TestImplementation();
        skeleton = (Skeleton) SkeletonFactory.createSkeleton(testImplementation);
        messageManager = new MessageManager();
        myAddress = messageManager.getMyAddress();
    }

    @Test
    public void testCreateSkeletonWithValidAddress() {
        assertNotNull(skeleton);
        NetworkAddress address = skeleton.getAddress();
        assertNotNull(address);
        assertTrue(address.getIpAddress().matches("^(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\." +
                "(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"));
        assertTrue(address.getPortNumber()>1023);
    }

    @Test
    public void testVoidMethod() {
        MethodCallMessage message = new MethodCallMessage(myAddress, "testMethod1");
        skeleton.handleRequest(message);
        assertEquals("void", testImplementation.getS());
    }

    @Test(expected = RuntimeException.class)
    public void testWrongMethodName() {
        MethodCallMessage message = new MethodCallMessage(myAddress, "nonExistingMethodName");
        skeleton.handleRequest(message);
    }

    @Test(timeout = 1000)
    public void testEmptyReply() {
        MethodCallMessage message = new MethodCallMessage(myAddress, "testMethod1");
        skeleton.handleRequest(message);
        MethodCallMessage reply = messageManager.wReceive();
        assertNotNull(reply.getParameter("result"));
        assertEquals("Ok", reply.getParameter("result"));
    }

    private interface MyLambda {
        boolean operator();
    }

    private void waitUntil(int timeoutmsec, MyLambda l) {
        long endTime = System.currentTimeMillis()+timeoutmsec;
        while(System.currentTimeMillis()<endTime) {
            boolean b = l.operator();
            if (b) return;
        }
        throw new RuntimeException("timeout!");
    }

    @Test(timeout = 1000)
    public void testRunMethodSpawnThread() {
        int numberOfThreads = Thread.getAllStackTraces().keySet().size();
        skeleton.run();
        int newNumber = Thread.getAllStackTraces().keySet().size();
        assertEquals("run method should create new thread", numberOfThreads+1, newNumber);
    }

    //TODO fails on run all
    @Test(timeout = 1000)
    public void testRunMethodOneRequest() {
        skeleton.run();
        MethodCallMessage message = new MethodCallMessage(myAddress, "testMethod1");
        messageManager.send(message, skeleton.getAddress());
        waitUntil(1000, () -> "void".equals(testImplementation.getS()));
        MethodCallMessage reply = messageManager.wReceive();
        assertNotNull(reply.getParameter("result"));
        assertEquals("Ok", reply.getParameter("result"));
    }

    @Test(timeout = 1000)
    public void testRunMethodMultipleRequests() {
        skeleton.run();
        MethodCallMessage message = new MethodCallMessage(myAddress, "testMethod1");
        messageManager.send(message, skeleton.getAddress());
        MethodCallMessage reply = messageManager.wReceive();
        assertEquals("Ok", reply.getParameter("result"));
        messageManager.send(message, skeleton.getAddress());
        reply = messageManager.wReceive();
        assertEquals("Ok", reply.getParameter("result"));
    }

    @Test(expected = RuntimeException.class)
    public void testMessageWithWrongNumberOfParams() {
        MethodCallMessage message = new MethodCallMessage(myAddress, "testMethod1");
        message.setParameter("arg0", "bla");
        skeleton.handleRequest(message);
    }

    @Test(timeout = 1000)
    public void testMethodWithParam() {
        MethodCallMessage message = new MethodCallMessage(myAddress, "testMethod2");
        message.setParameter("arg0", "test");
        skeleton.handleRequest(message);
        assertEquals("test", testImplementation.getS());
        MethodCallMessage reply = messageManager.wReceive();
        assertEquals("Ok", reply.getParameter("result"));
    }

    //TODO
    @Test(expected = RuntimeException.class)
    public void testMessageWithWrongParamName() {
        MethodCallMessage message = new MethodCallMessage(myAddress, "testMethod2");
        message.setParameter("naam", "test");
        skeleton.handleRequest(message);
    }

    @Test(timeout = 1000)
    public void testMethodWithIntParam() {
        MethodCallMessage message = new MethodCallMessage(myAddress, "testMethod9");
        message.setParameter("arg0", "42");
        skeleton.handleRequest(message);
        assertEquals(42, testImplementation.getI());
        MethodCallMessage reply = messageManager.wReceive();
        assertEquals("Ok", reply.getParameter("result"));
    }

    @Test(expected = RuntimeException.class)
    public void testMessageWithWrongParamType() {
        MethodCallMessage message = new MethodCallMessage(myAddress, "testMethod9");
        message.setParameter("arg0", "no integer");
        skeleton.handleRequest(message);
    }

    @Test(timeout = 1000)
    public void testMethodWithManyParams() {
        MethodCallMessage message = new MethodCallMessage(myAddress, "testMethod3");
        message.setParameter("arg0", "42");
        message.setParameter("arg1", "testString");
        message.setParameter("arg2", "42.5");
        message.setParameter("arg3", "true");
        message.setParameter("arg4", "a");
        skeleton.handleRequest(message);
        assertEquals(42, testImplementation.getI());
        assertEquals("testString", testImplementation.getS());
        assertTrue(42.5==testImplementation.getD());
        assertTrue(testImplementation.isB());
        assertEquals('a', testImplementation.getC());
        MethodCallMessage reply = messageManager.wReceive();
        assertEquals("Ok", reply.getParameter("result"));
    }

    @Test(timeout = 1000)
    public void testObjectAsParameter() {
        MethodCallMessage message = new MethodCallMessage(myAddress, "testMethod4");
        message.setParameter("arg0.age", "42");
        message.setParameter("arg0.name", "teststring");
        message.setParameter("arg0.deleted", "true");
        message.setParameter("arg0.gender", "a");
        skeleton.handleRequest(message);
        assertEquals(42, testImplementation.getI());
        assertEquals("teststring", testImplementation.getS());
        assertTrue(testImplementation.isB());
        assertEquals('a', testImplementation.getC());
        MethodCallMessage reply = messageManager.wReceive();
        assertEquals("Ok", reply.getParameter("result"));
    }

    @Test(timeout = 1000)
    public void testReturnValue() {
        MethodCallMessage message = new MethodCallMessage(myAddress, "testMethod5");
        skeleton.handleRequest(message);
        MethodCallMessage reply = messageManager.wReceive();
        assertEquals("Yes", reply.getParameter("result"));
    }

    @Test(timeout = 1000)
    public void testIntReturnValue() {
        MethodCallMessage message = new MethodCallMessage(myAddress, "testMethod6");
        skeleton.handleRequest(message);
        MethodCallMessage reply = messageManager.wReceive();
        assertEquals("100", reply.getParameter("result"));
    }

    @Test(timeout = 1000)
    public void testCharReturnValue() {
        MethodCallMessage message = new MethodCallMessage(myAddress, "testMethod7");
        skeleton.handleRequest(message);
        MethodCallMessage reply = messageManager.wReceive();
        assertEquals("r", reply.getParameter("result"));
    }

    @Test(timeout = 1000)
    public void testBoolReturnValue() {
        MethodCallMessage message = new MethodCallMessage(myAddress, "testMethod8");
        skeleton.handleRequest(message);
        MethodCallMessage reply = messageManager.wReceive();
        assertEquals("true", reply.getParameter("result"));
    }

    @Test(timeout = 1000)
    public void testObjectReturnValue() {
        MethodCallMessage message = new MethodCallMessage(myAddress, "testMethod11");
        skeleton.handleRequest(message);
        MethodCallMessage reply = messageManager.wReceive();
        assertEquals("hoehoe", reply.getParameter("result.name"));
        assertEquals("p", reply.getParameter("result.gender"));
        assertEquals("97", reply.getParameter("result.age"));
        assertEquals("false", reply.getParameter("result.deleted"));
    }
}
