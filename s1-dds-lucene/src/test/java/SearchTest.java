import org.s1.lucene.FullTextSearcher;
import org.s1.lucene.SearcherFactory;
import org.s1.misc.Closure;
import org.s1.misc.ClosureException;
import org.s1.objects.Objects;
import org.s1.table.format.FieldQueryNode;
import org.s1.table.format.GroupQueryNode;
import org.s1.table.format.Query;
import org.s1.test.ClusterTest;
import org.s1.test.LoadTestUtils;

import java.util.Date;
import java.util.Map;

/**
 * s1v2
 * User: GPykhov
 * Date: 19.02.14
 * Time: 13:22
 */
public class SearchTest extends ClusterTest {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        if(1==1)
            return;

        FullTextSearcher s = SearcherFactory.getSearcher("test1");
        for(int i=0;i<100;i++){
            s.removeDocument(""+i);
        }
        trace("remove done");
        for(int i=0;i<100;i++){
            s.addDocument(""+i, Objects.newHashMap(String.class,Object.class,
                    "title","title document_"+i,
                    "size",i*10,
                    "created",new Date()
            ));
        }
        trace("add done");
        for(int i=0;i<100;i++){
            s.setDocument("" + i, Objects.newHashMap(String.class, Object.class,
                    "title", "title document_" + i,
                    "body", "hello world test" + (i % 5),
                    "size", i * 10,
                    "created", new Date()
            ));
        }
        trace("set done");
    }


    public void testSearch(){
        int p = 1;
        title("Write, parallel: "+p);
        assertEquals(p, LoadTestUtils.run("test", p, p, new Closure<Integer, Object>() {
            @Override
            public Object call(Integer input) throws ClosureException {
                FullTextSearcher s = SearcherFactory.getSearcher("test1");
                Map<String,Object> m = null;
                //search
                m = s.search("hello",null,false,false,false,0,10);
                assertTrue(Objects.equals(100,Objects.get(m,"count")));

                //filter
                m = s.search("hello",new Query(new FieldQueryNode("id", FieldQueryNode.FieldOperation.EQUALS,"5")),
                        false,false,false,0,10);
                assertTrue(Objects.equals(1,Objects.get(m,"count")));

                //filter2
                m = s.search("hello",new Query(new FieldQueryNode("size", FieldQueryNode.FieldOperation.GT,980)),
                        false,false,false,0,10);
                assertTrue(Objects.equals(1,Objects.get(m,"count")));

                //filter3
                m = s.search("hello",new Query(new FieldQueryNode("size", FieldQueryNode.FieldOperation.GTE,980)),
                        false,false,false,0,10);
                assertTrue(Objects.equals(2,Objects.get(m,"count")));

                //filter4
                m = s.search("hello",new Query(new FieldQueryNode("size", FieldQueryNode.FieldOperation.LT,20)),
                        false,false,false,0,10);
                assertTrue(Objects.equals(2,Objects.get(m,"count")));

                //filter5
                m = s.search("hello",new Query(new FieldQueryNode("size", FieldQueryNode.FieldOperation.LTE,20)),
                        false,false,false,0,10);
                assertTrue(Objects.equals(3,Objects.get(m,"count")));

                //filter6
                m = s.search("hello",new Query(new FieldQueryNode("created", FieldQueryNode.FieldOperation.LTE,new Date())),
                        false,false,false,0,10);
                assertTrue(Objects.equals(100,Objects.get(m,"count")));

                //filter7
                m = s.search("hello",new Query(
                        new GroupQueryNode(GroupQueryNode.GroupOperation.AND,
                                new FieldQueryNode("created", FieldQueryNode.FieldOperation.LTE,new Date()),
                                new FieldQueryNode("size", FieldQueryNode.FieldOperation.LTE,20),
                                new FieldQueryNode("size", FieldQueryNode.FieldOperation.GTE,10)
                                )),
                        false,false,false,0,10);
                assertTrue(Objects.equals(2,Objects.get(m,"count")));

                //filter8
                m = s.search("hello",new Query(
                        new GroupQueryNode(GroupQueryNode.GroupOperation.OR,
                                new FieldQueryNode("size", FieldQueryNode.FieldOperation.LTE,20),
                                new FieldQueryNode("size", FieldQueryNode.FieldOperation.GTE,980)
                        )),
                        false,false,false,0,10);
                assertTrue(Objects.equals(5,Objects.get(m,"count")));

                //fuzzy
                m = s.search("hllo",null,false,false,true,0,10);
                assertTrue(Objects.equals(100,Objects.get(m,"count")));
                assertEquals(true,Objects.get(m,"notFound"));

                //suggest
                m = s.search("hello worl",null,false,true,true,0,10);
                assertTrue(Objects.equals(100,Objects.get(m,"count")));

                //highlight
                m = s.search("hello test0",null,true,false,false,0,10);
                assertTrue(Objects.equals(100,Objects.get(m,"count")));
                assertEquals("<B>hello</B> world <B>test0</B>",Objects.get(m,"list[0].highlight.body"));


                return null;
            }
        }));

    }

}
