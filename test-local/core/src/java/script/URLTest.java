package script;

import org.s1.script.function.URLFunctionSet;
import org.s1.testing.BasicTest;
import org.s1.testing.LoadTestUtils;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * @author Grigory Pykhov
 */
public class URLTest extends BasicTest{

    @DataProvider(name = "add")
    public Object[][] add(){
        return new Object[][]{
                {"http://example.com/a/b?a=1","http://example.com/a/b","a","1"},
                {"http://example.com/a/b?a=1&b=2","http://example.com/a/b?a=1","b","2"}
        };
    }

    @DataProvider(name = "remove")
    public Object[][] remove(){
        return new Object[][]{
                {"http://example.com/a/b","http://example.com/a/b?a=1","a"},
                {"http://example.com/a/b?a=1","http://example.com/a/b?a=1&b=2","b"}
        };
    }

    @Test(dataProvider = "add")
    public void testAdd(final String result, final String url, final String param, final String value){
        int p=10;
        final URLFunctionSet uf = new URLFunctionSet();
        assertEquals(p,LoadTestUtils.run("test",p,p,new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int i) throws Exception {
                assertEquals(result,uf.addParam(url,param,value));
            }
        }));
    }

    @Test(dataProvider = "remove")
    public void testRemove(final String result, final String url, final String param){
        int p=10;
        final URLFunctionSet uf = new URLFunctionSet();
        assertEquals(p,LoadTestUtils.run("test",p,p,new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int i) throws Exception {
                assertEquals(result,uf.removeParam(url,param));
            }
        }));
    }
}
