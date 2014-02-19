import org.s1.lucene.FullTextSearcher;
import org.s1.lucene.SearcherFactory;
import org.s1.misc.Closure;
import org.s1.misc.ClosureException;
import org.s1.test.ClusterTest;
import org.s1.test.LoadTestUtils;

/**
 * s1v2
 * User: GPykhov
 * Date: 19.02.14
 * Time: 11:52
 */
public class FactoryTest extends ClusterTest {

    public void testFactory(){
        int p = 100;
        title("Factory, parallel: "+p);
        assertEquals(p, LoadTestUtils.run("test",p,p,new Closure<Integer, Object>() {
            @Override
            public Object call(Integer input) throws ClosureException {
                FullTextSearcher s = SearcherFactory.getSearcher("test1");
                assertEquals(4,s.getFields().size());

                boolean b = false;
                try{
                    SearcherFactory.getSearcher("not_found");
                }catch (Throwable e){
                    b = true;
                }
                assertTrue(b);
                return null;
            }
        }));

    }

}
