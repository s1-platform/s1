package org.s1.testing;

import org.testng.Assert;
import org.testng.annotations.*;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

/**
 * @author Grigory Pykhov
 */
public abstract class BasicTest {

    protected static final Properties properties = new Properties();
    static {
        try {
            properties.load( BasicTest.class.getResourceAsStream("/s1test.properties"));
        } catch (Throwable e) {
            System.out.println("Cannot load /s1test.properties: "+e.getMessage());
        }
    }

    @BeforeMethod
    public void beforeMethod(Method method){
        methodTitle("BEGIN: " + method.getDeclaringClass().getName() + "#" + method.getName());
    }

    @AfterMethod
    public void afterMethod(Method method){
        methodTitle("END: " + method.getDeclaringClass().getName() + "#" + method.getName());
    }

    @BeforeClass
    public void setEnv(){
        String s = System.getProperty("s1.ConfigHome");
        if(s==null || s.isEmpty())
            s = "classpath:/config";
        System.setProperty("s1.ConfigHome",s);
        trace("s1.ConfigHome="+s);
    }

    protected void wrap(Throwable e){
        throw new RuntimeException(e.getMessage(),e);
    }

    protected void sleep(long millis){
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            wrap(e);
        }
    }

    protected void sleep(long time, TimeUnit tu){
        try {
            tu.sleep(time);
        } catch (InterruptedException e) {
            wrap(e);
        }
    }

    protected InputStream resource(String p){
        return this.getClass().getResourceAsStream(p);
    }

    protected String resourceAsString(String p){
        InputStream is = resource(p);
        if(is==null)
            return null;
        Scanner s = new Scanner(is, "UTF-8").useDelimiter("\\A");
        return s.hasNext() ? s.next() : null;
    }

    protected void trace(Object t){
        System.out.println("\t> " + (t == null ? "" : "(" + t.getClass() + ")") + " " + t);
    }

    private void methodTitle(Object t){
        String s = "****";
        for(int i=0;i<t.toString().length();i++){
            s+=("*");
        }
        s+=("****\n");
        s+=("*** "+t+" ***\n");
        s+=("****");
        for(int i=0;i<t.toString().length();i++){
            s+=("*");
        }
        s+=("****\n");
        System.out.print(s);
    }

    protected void assertEquals(Object expected, Object found){
        Assert.assertEquals(found,expected);
    }

    protected void assertTrue(boolean ex){
        Assert.assertTrue(ex);
    }

    protected void assertFalse(boolean ex){
        Assert.assertFalse(ex);
    }

    protected void assertNull(Object o){
        Assert.assertNull(o);
    }

    protected void assertNotNull(Object o){
        Assert.assertNotNull(o);
    }
}
