package org.s1.test;

import junit.framework.TestCase;
import org.s1.S1SystemError;
import org.s1.misc.IOUtils;
import org.s1.misc.protocols.Init;
import org.s1.objects.Objects;
import org.s1.options.Options;
import org.s1.options.OptionsStorage;

import java.io.*;
import java.net.URISyntaxException;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

/**
 * Base class for test cases
 */
public abstract class BasicTest extends TestCase {

    private static Properties properties;

    protected Properties getProperties(){
        if(properties==null){
            properties = new Properties();
            try {
                properties.load(resource("/s1test.properties"));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return properties;
    }

    protected String getOptionsPath(){
        String s = (String)getProperties().get("options");
        if(!Objects.isNullOrEmpty(s)){
            s = s.replace(".",File.separator);
            s = getTestClassesHome()+s;
        }
        return s;
    }

    @Override
    protected void setUp() throws Exception {
        Init.init();
        super.setUp();
        if(getOptionsPath()!=null){
            System.setProperty("s1."+ OptionsStorage.CONFIG_HOME, getOptionsPath());
            trace("s1."+ OptionsStorage.CONFIG_HOME+"="+getOptionsPath());
        }
    }

    private PrintStream os = System.out;

    private String repeat(String s, int length){
        String r = "";
        for(int i=0;i<length;i++){
            r+=s;
        }
        return r;
    }

    /**
     * Test title
     *
     * @param title
     */
    protected void title(String title) {
        os.println(repeat("*", title.length() + 8) + "\n"
                + "**  " + title + "  **\n" +
                repeat("*", title.length() + 8));
        os.flush();
    }

    /**
     * Test result message
     *
     * @param result
     */
    protected void result(String result) {
        os.println(repeat("-", Math.min(result.length(), 20)) + "\n" + result);
        os.flush();
    }

    /**
     * Test tracing messages
     *
     * @param message
     */
    protected void trace(Object message) {
        os.println("\t> " + message);
        os.flush();
    }

    protected InputStream resource(String p){
        return this.getClass().getResourceAsStream(p);
    }

    protected String resourceAsString(String p){
        return IOUtils.toString(resource(p),"UTF-8");
    }

    protected String getProjectHome(){
        return getClassesHome()+".."+File.separator+".."+File.separator;
    }

    protected String getClassesHome(){
        try {
            return new File(Options.class.getResource("/").toURI()).getAbsolutePath()+File.separator;
        } catch (URISyntaxException e) {
            return null;
        }
    }

    protected String getTestClassesHome(){
        try {
            return new File(this.getClass().getResource("/").toURI()).getAbsolutePath()+File.separator;
        } catch (URISyntaxException e) {
            return null;
        }
    }

    protected void sleep(long millis){
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw S1SystemError.wrap(e);
        }
    }

    protected void sleep(long time, TimeUnit tu){
        try {
            tu.sleep(time);
        } catch (InterruptedException e) {
            throw S1SystemError.wrap(e);
        }
    }

}
