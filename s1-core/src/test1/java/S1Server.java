import org.s1.test.TestAppServer;

/**
 * s1v2
 * User: GPykhov
 * Date: 24.01.14
 * Time: 14:59
 */
public class S1Server extends TestAppServer {

    @Override
    protected int getPort() {
        return 9990;
    }

    @Override
    protected String getAppPath() {
        return getClassesHome()+ "../../src/test/resources/webapp";
    }

    @Override
    protected String getContext() {
        return "/s1";
    }

    @Override
    protected String getOptions() {
        return getClassesHome()+"options_standalone";
    }

    public static void main(String[] args) {
        new S1Server().run();
    }
}
