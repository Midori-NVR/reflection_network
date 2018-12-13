package be.kdg.distrib.testclasses;

public class TestImplementation implements TestInterface2 {
    private String s;
    private int i;
    private char c;
    private boolean b;
    private double d;

    @Override
    public void testMethod1() {
        s = "void";
    }

    @Override
    public void testMethod2(String s) {
        this.s = s;
    }

    @Override
    public void testMethod3(int i, String s, double d, boolean b, char c) {
        this.i = i;
        this.s = s;
        this.d = d;
        this.b = b;
        this.c = c;
    }

    @Override
    public void testMethod4(TestObject test) {
        c = test.getGender();
        s = test.getName();
        b = test.isDeleted();
        i = test.getAge();
    }

    @Override
    public String testMethod5() {
        return "Yes";
    }

    @Override
    public int testMethod6() {
        return 100;
    }

    @Override
    public char testMethod7() {
        return 'r';
    }

    @Override
    public boolean testMethod8() {
        return true;
    }

    @Override
    public void testMethod9(int i) {
        this.i = i;
    }

    @Override
    public TestObject testMethod11() {
        return new TestObject("hoehoe", 97, 'p', false);
    }

    @Override
    public TestObject fullBlownTestMethod(String s, TestObject test, int a, boolean b) {
        return new TestObject(s, a, test.getGender(), true);
    }

    public String getS() {
        return s;
    }

    public void setS(String s) {
        this.s = s;
    }

    public int getI() {
        return i;
    }

    public void setI(int i) {
        this.i = i;
    }

    public char getC() {
        return c;
    }

    public void setC(char c) {
        this.c = c;
    }

    public boolean isB() {
        return b;
    }

    public void setB(boolean b) {
        this.b = b;
    }

    public double getD() {
        return d;
    }

    public void setD(double d) {
        this.d = d;
    }
}
