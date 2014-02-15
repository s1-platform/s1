package log;

import com.mongodb.DB;
import com.mongodb.DBCollection;
import org.s1.S1SystemError;
import org.s1.cluster.datasource.MoreThanOneFoundException;
import org.s1.cluster.datasource.NotFoundException;
import org.s1.log.LogStorage;
import org.s1.misc.Closure;
import org.s1.misc.ClosureException;
import org.s1.mongodb.MongoDBConnectionHelper;
import org.s1.mongodb.MongoDBFormat;
import org.s1.mongodb.MongoDBQueryHelper;
import org.s1.mongodb.log.MongoDBLogStorage;
import org.s1.objects.Objects;
import org.s1.test.BasicTest;
import org.s1.test.LoadTestUtils;
import org.s1.test.ServerTest;
import org.s1.test.TestHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * s1v2
 * User: GPykhov
 * Date: 14.02.14
 * Time: 13:21
 */
public class LogStorageTest extends ServerTest {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        DB db = MongoDBConnectionHelper.getConnection(MongoDBLogStorage.DB_INSTANCE);
        DBCollection coll = db.getCollection(MongoDBLogStorage.getCollectionName());
        coll.drop();
        trace("log4j collection cleared");
    }

    public void testLog(){
        int p = 1;
        title("Log, parallel: " + p);
        assertEquals(p, LoadTestUtils.run("test", p, p, new Closure<Integer, Object>() {
            @Override
            public Object call(Integer input) throws ClosureException {
                TestHttpClient client = client();

                //authenticate
                client.postJSON(getContext() + "/dispatcher/Auth.login", Objects.newHashMap(
                        String.class, Object.class,
                        "name", "root",
                        "password", "root"
                ), null);

                //get
                Map<String, Object> m = client.postJSON(getContext() + "/dispatcher/Log.list", Objects.newHashMap(
                        String.class, Object.class
                        //,"search",Objects.newHashMap("name","MongoDBConnectionHelper")
                ), null);

                assertTrue(Objects.get(Long.class,m, "count")>0);
                assertEquals(10,Objects.get(List.class, m, "list").size());
                assertNotNull(Objects.get(m,"list[0].name"));
                if(input==0)
                    trace(Objects.get(m,"list[0]"));

                return null;
            }
        }));
    }

}
