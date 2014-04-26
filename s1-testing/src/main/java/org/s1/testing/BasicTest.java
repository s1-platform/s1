package org.s1.testing;

import org.testng.Assert;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.BeforeTest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
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
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage(),e);
        }
    }

    @BeforeTest
    public void beforeTest(){
        String s = System.getProperty("s1.ConfigHome");
        if(s==null || s.isEmpty())
            s = "classpath:/"+properties.getProperty("s1.ConfigHome","");
        System.setProperty("s1.ConfigHome",s);
        trace("Config.Home="+s);
    }

    protected String getProjectHome(){
        if(new File("./build.gradle").exists())
            return new File("").getAbsolutePath();
        return null;
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

    protected void title(Object t){
        System.out.print("****");
        for(int i=0;i<t.toString().length();i++){
            System.out.print("*");
        }
        System.out.print("****\n");
        System.out.println("*** "+t+" ***");
        System.out.print("****");
        for(int i=0;i<t.toString().length();i++){
            System.out.print("*");
        }
        System.out.print("****\n");
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
