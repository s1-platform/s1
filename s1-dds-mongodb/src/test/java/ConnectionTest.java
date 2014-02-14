import com.mongodb.DB;
import org.s1.misc.Closure;
import org.s1.misc.ClosureException;
import org.s1.mongodb.MongoDBConnectionHelper;
import org.s1.options.Options;
import org.s1.test.BasicTest;
import org.s1.test.LoadTestUtils;

/**
 * s1v2
 * User: GPykhov
 * Date: 14.02.14
 * Time: 12:42
 */
public class ConnectionTest extends BasicTest {

    public void testDefault(){
        int p=100;
        title("Default, parallel: "+p);
        final String n = Options.getStorage().get("MongoDB","default.name");
        assertEquals(p, LoadTestUtils.run("test",p,p,new Closure<Integer, Object>() {
            @Override
            public Object call(Integer input) throws ClosureException {
                DB db = MongoDBConnectionHelper.getConnection(null);
                assertEquals(n,db.getName());
                db.getCollectionNames();
                if(input==0)
                    trace(db.getCollectionNames());
                return null;
            }
        }));
    }

}
