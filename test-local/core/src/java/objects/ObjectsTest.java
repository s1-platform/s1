package objects;

import org.s1.misc.Closure;
import org.s1.objects.BadDataException;
import org.s1.objects.ObjectDiff;
import org.s1.objects.ObjectIterator;
import org.s1.objects.Objects;
import org.s1.testing.BasicTest;
import org.s1.testing.LoadTestUtils;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * s1v2
 * User: GPykhov
 * Date: 09.01.14
 * Time: 21:05
 */
public class ObjectsTest extends BasicTest {

    @Test 
	public void testNewHashMap(){
        int p = 10;

        assertEquals(p, LoadTestUtils.run("test", p, p, new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int index) throws Exception  {
                Map<String, Object> m1 = Objects.newHashMap("a", "aaa", "b", 123, "c", Objects.newHashMap("a", "bbb"));
                assertEquals("aaa", m1.get("a"));
                assertEquals(123, m1.get("b"));
                assertEquals("bbb", ((Map<String, Object>) m1.get("c")).get("a"));

                m1 = Objects.newHashMap(String.class, Object.class, "a", "aaa", "b", 123, "c", Objects.newHashMap("a", "bbb"));
                assertEquals("aaa", m1.get("a"));
                assertEquals(123, m1.get("b"));
                assertEquals("bbb", ((Map<String, Object>) m1.get("c")).get("a"));

                m1 = Objects.newSOHashMap("a", "aaa", "b", 123, "c", Objects.newHashMap("a", "bbb"));
                assertEquals("aaa", m1.get("a"));
                assertEquals(123, m1.get("b"));
                assertEquals("bbb", ((Map<String, Object>) m1.get("c")).get("a"));

                
            }
        }));
    }

    @Test 
	public void testNewArrayList(){
        int p = 10;

        assertEquals(p, LoadTestUtils.run("test", p, p, new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int index) throws Exception  {

                List<Object> m1 = Objects.newArrayList("a", "aaa", "b", 123, "c", Objects.newHashMap("a", "bbb"));
                assertEquals("aaa", m1.get(1));
                assertEquals(123, m1.get(3));
                assertEquals(6, m1.size());
                assertEquals("bbb", ((Map<String, String>) m1.get(5)).get("a"));

                
            }
        }));
    }

    @Test 
	public void testFind(){
        int p = 10;

        assertEquals(p, LoadTestUtils.run("test", p, p, new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int index) throws Exception  {
                List<Object> l1 = Objects.newArrayList("a", "aaa", "b", 123, "c", Objects.newHashMap("a", "bbb"));
                String s = (String) Objects.find(l1, new Closure<Object, Boolean>() {
                    @Override
                    public Boolean call(Object input) {
                        return "aaa".equals(input);
                    }
                });
                assertEquals("aaa", s);
                assertNull(Objects.find(l1, new Closure<Object, Boolean>() {
                    @Override
                    public Boolean call(Object input) {
                        return false;
                    }
                }));

                Map<String, Object> m1 = Objects.newHashMap("a", "aaa", "b", 123, "c", Objects.newHashMap("a", "bbb"));
                String s1 = (String) Objects.find(m1, new Closure<Map.Entry<String, Object>, Boolean>() {
                    @Override
                    public Boolean call(Map.Entry<String, Object> input) {
                        return "aaa".equals(input.getValue());
                    }
                }).getKey();
                assertEquals("a", s1);
                assertNull(Objects.find(m1, new Closure<Map.Entry<String, Object>, Boolean>() {
                    @Override
                    public Boolean call(Map.Entry<String, Object> input) {
                        return false;
                    }
                }));

                
            }
        }));
    }

    @Test 
	public void testFindAll(){
        int p = 10;

        assertEquals(p, LoadTestUtils.run("test", p, p, new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int index) throws Exception  {

                List<Object> l1 = Objects.newArrayList("a", "aaa", "b", 123, "c", Objects.newHashMap("a", "bbb"));
                List<Object> s = Objects.findAll(l1, new Closure<Object, Boolean>() {
                    @Override
                    public Boolean call(Object input) {
                        return "aaa".equals(input) || "a".equals(input);
                    }
                });
                assertEquals(2, s.size());
                s = Objects.findAll(l1, new Closure<Object, Boolean>() {
                    @Override
                    public Boolean call(Object input) {
                        return false;
                    }
                });
                assertEquals(0, s.size());

                Map<String, Object> m1 = Objects.newHashMap("a", "aaa", "b", 123, "c", Objects.newHashMap("a", "bbb"));
                List<Map.Entry<String, Object>> s1 = Objects.findAll(m1, new Closure<Map.Entry<String, Object>, Boolean>() {
                    @Override
                    public Boolean call(Map.Entry<String, Object> input) {
                        return "aaa".equals(input.getValue()) || "b".equals(input.getKey());
                    }
                });
                assertEquals(2, s1.size());
                s1 = Objects.findAll(m1, new Closure<Map.Entry<String, Object>, Boolean>() {
                    @Override
                    public Boolean call(Map.Entry<String, Object> input) {
                        return false;
                    }
                });
                assertEquals(0, s1.size());
                
            }
        }));
    }

    @Test 
	public void testCast() {
        int p = 10;
        assertEquals(p, LoadTestUtils.run("test", p, p, new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int index) throws Exception  {
                Date dt = new Date();
                assertEquals(123.2D, Objects.cast("123.2", Double.class));
                assertEquals(123.2D, Objects.cast("123.2", "Double"));
                assertEquals(123.2D, Objects.cast("123,2", "Double"));
                assertEquals((Integer) 123, Objects.cast("123", Integer.class));
                assertEquals(123, Objects.cast("123", "Integer"));
                assertEquals(123.2F, Objects.cast("123.2", Float.class));
                assertEquals(123.2F, Objects.cast("123.2", "Float"));
                assertEquals((Long) 123L, Objects.cast("123", Long.class));
                assertEquals(123L, Objects.cast("123", "Long"));
                assertEquals((Boolean) true, Objects.cast("true", Boolean.class));
                assertEquals(true, Objects.cast(true, "Boolean"));
                assertEquals("123.2", Objects.cast(123.2F, String.class));
                assertEquals("123.2", Objects.cast(123.2D, "String"));
                assertEquals(new BigDecimal("123.2"), Objects.cast("123.2", BigDecimal.class));
                assertEquals(new BigDecimal("123.2"), Objects.cast(123.2D, "BigDecimal"));
                assertEquals(new BigInteger("123"), Objects.cast("123", BigInteger.class));
                assertEquals(new BigInteger("123"), Objects.cast(123, "BigInteger"));
                assertEquals(dt.getTime(), Objects.cast(dt, Date.class).getTime());
                assertEquals(null, Objects.cast(null, "Date"));
                assertEquals(dt.getTime(), Objects.cast(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(dt), Date.class).getTime());
                
            }
        }));
    }

    @Test 
	public void testCopy() {
        int p = 10;
        final Map<String, Object> m = Objects.newHashMap();
        m.put("a", 123);
        m.put("b", "345");
        final Map<String, Object> m1 = Objects.newHashMap();
        m.put("c", m1);
        m1.put("d", Objects.newArrayList(1, 2, 3));

        trace("orginal map: " + m);

        assertEquals(p, LoadTestUtils.run("test", p, p, new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int index) throws Exception  {
                Map<String, Object> _m = Objects.copy(m);
                if (index == 0)
                    trace("copied map: " + m);
                assertTrue(!((Map) _m.get("c")).containsKey("x"));
                assertTrue(((Map) _m.get("c")).containsKey("d"));
                ((Map) _m.get("c")).put("x", "x");
                
            }
        }));
    }

    @Test 
	public void testIsNullOrEmpty(){
        int p = 10;

        assertEquals(p, LoadTestUtils.run("test", p, p, new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int index) throws Exception  {
                assertTrue(Objects.isNullOrEmpty(null));
                assertTrue(Objects.isNullOrEmpty(""));
                assertTrue(Objects.isNullOrEmpty(Objects.newHashMap()));
                assertTrue(Objects.isNullOrEmpty(Objects.newArrayList()));
                assertTrue(!Objects.isNullOrEmpty("asd"));
                assertTrue(!Objects.isNullOrEmpty(Objects.newHashMap("a", "a")));
                assertTrue(!Objects.isNullOrEmpty(Objects.newArrayList("a", "a")));
                
            }
        }));
    }

    @Test 
	public void testEquals(){
        int p = 1;

        assertEquals(p, LoadTestUtils.run("test", p, p, new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int index) throws Exception  {

                assertTrue(Objects.equals("", ""));
                assertTrue(Objects.equals("1", "1"));
                assertTrue(Objects.equals(1, 1));
                assertTrue(Objects.equals(1L, 1));
                assertTrue(Objects.equals(1L, 1.0F));
                assertTrue(Objects.equals("1", 1));
                assertTrue(!Objects.equals("asd", 1));
                assertTrue(Objects.equals(Objects.newHashMap(), Objects.newHashMap()));
                assertTrue(Objects.equals(Objects.newArrayList(), Objects.newArrayList()));
                assertTrue(Objects.equals(Objects.newHashMap("a", "1", "b", "2"), Objects.newHashMap("b", "2", "a", "1")));
                assertTrue(Objects.equals(Objects.newHashMap("a", "1", "b", "2", "c", Objects.newHashMap("a", "1", "b", "2")),
                        Objects.newHashMap("c", Objects.newHashMap("a", "1", "b", "2"), "b", "2", "a", "1")));
                assertTrue(!Objects.equals(Objects.newHashMap("a", "1", "b", "2", "c", Objects.newHashMap("a", "1", "b", "2")),
                        Objects.newHashMap("c", Objects.newHashMap("a", "1", "b", "21"), "b", "2", "a", "1")));
                assertTrue(Objects.equals(Objects.newArrayList("1", "2", 3), Objects.newArrayList("1", "2", 3)));
                assertTrue(Objects.equals(Objects.newArrayList(Objects.newHashMap("a", "1", "b", "21", "c", Objects.newHashMap("a", "1", "b", "21")), "2"), Objects.newArrayList(Objects.newHashMap("a", "1", "b", "21", "c", Objects.newHashMap("a", "1", "b", "21")), "2")));
                assertTrue(!Objects.equals(Objects.newArrayList(Objects.newHashMap("a", "1", "b", "2"), "2"), Objects.newArrayList(Objects.newHashMap("a", "1", "b", "21"), "2")));

                
            }
        }));
    }


    @Test 
	public void testSet(){
        int p = 10;

        assertEquals(p, LoadTestUtils.run("test", p, p, new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int index) throws Exception  {

                Map<String, Object> m = Objects.newHashMap("a",
                        Objects.newArrayList(Objects.newHashMap("a", "1", "b", "2"), "2"));

                Objects.set(m, "a[0].a", "xxx");
                Objects.set(m, "b[2].a", "yyy");
                assertEquals("xxx", Objects.get(m, "a[0].a"));
                assertEquals("yyy", Objects.get(m, "b[2].a"));

                
            }
        }));
    }

    @Test 
	public void testGet(){
        int p = 10;

        assertEquals(p, LoadTestUtils.run("test", p, p, new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int index) throws Exception  {

                Map<String, Object> m = Objects.newHashMap("a", Objects.newArrayList(Objects.newHashMap("a", "1", "b", "2"), "2"));

                assertEquals("1", Objects.get(m, "a[0].a"));
                assertEquals("111", Objects.get(m, "a[0].a1", "111"));

                
            }
        }));
    }

    @Test 
	public void testMerge(){
        int p = 10;

        assertEquals(p, LoadTestUtils.run("test", p, p, new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int index) throws Exception  {

                Map<String, Object> m = Objects.newHashMap("m", Objects.newHashMap("a", 2, "b", 1, "c", 3), "a", Objects.newArrayList(Objects.newHashMap("a", "1", "b", "2", "c", "4"), "2"));
                Map<String, Object> m1 = Objects.newHashMap("m", Objects.newHashMap("a", 1, "b", 2, "d", 5), "a", Objects.newArrayList(Objects.newHashMap("a", "1", "b", "3"), "2"));

                Map<String, Object> _m = Objects.copy(m);
                Map<String, Object> _m1 = Objects.copy(m1);

                Map<String, Object> m2 = Objects.merge(_m, _m1);

                assertTrue(Objects.equals(m, _m));
                assertTrue(Objects.equals(m1, _m1));

                assertEquals(1, Objects.get(m2, "m.a"));
                assertEquals(2, Objects.get(m2, "m.b"));
                assertEquals(3, Objects.get(m2, "m.c"));
                assertEquals(5, Objects.get(m2, "m.d"));

                assertEquals(null, Objects.get(m2, "a[0].c"));
                assertEquals("3", Objects.get(m2, "a[0].b"));

                
            }
        }));
    }

    @Test 
	public void testDiff(){
        int p = 10;

        assertEquals(p, LoadTestUtils.run("test", p, p, new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int index) throws Exception  {

                Map<String, Object> m = Objects.newHashMap(
                        "m", Objects.newHashMap("a", 2, "b", 1, "c", 3),
                        "a", Objects.newArrayList(Objects.newHashMap("a", "1", "b", "2", "c", "4"), "2"));
                Map<String, Object> m1 = Objects.newHashMap(
                        "m", Objects.newHashMap("a", 1, "b", 2, "d", 5),
                        "a", Objects.newArrayList(Objects.newHashMap("a", "1", "b", "3"), "2", "3"));

                Map<String, Object> _m = Objects.copy(m);
                Map<String, Object> _m1 = Objects.copy(m1);

                List<ObjectDiff.DiffBean> l = Objects.diff(_m, _m1);

                assertTrue(Objects.equals(m, _m));
                assertTrue(Objects.equals(m1, _m1));

                assertEquals(7, l.size());

                ObjectDiff.DiffBean da = Objects.find(l, new Closure<ObjectDiff.DiffBean, Boolean>() {
                    @Override
                    public Boolean call(ObjectDiff.DiffBean input) {
                        return input.getPath().equals("m.a");
                    }
                });
                ObjectDiff.DiffBean dc = Objects.find(l, new Closure<ObjectDiff.DiffBean, Boolean>() {
                    @Override
                    public Boolean call(ObjectDiff.DiffBean input) {
                        return input.getPath().equals("m.c");
                    }
                });
                ObjectDiff.DiffBean d3 = Objects.find(l, new Closure<ObjectDiff.DiffBean, Boolean>() {
                    @Override
                    public Boolean call(ObjectDiff.DiffBean input) {
                        return input.getPath().equals("a[2]");
                    }
                });

                assertEquals("m.a", da.getPath());
                assertEquals(2, da.getOldValue());
                assertEquals(1, da.getNewValue());

                assertEquals("m.c", dc.getPath());
                assertEquals(3, dc.getOldValue());
                assertEquals(null, dc.getNewValue());

                assertEquals("a[2]", d3.getPath());
                assertEquals(null, d3.getOldValue());
                assertEquals("3", d3.getNewValue());

                
            }
        }));
    }

    @Test 
	public void testIterate(){
        int p = 10;

        assertEquals(p, LoadTestUtils.run("test", p, p, new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int index) throws Exception  {

                Map<String, Object> m1 = Objects.newHashMap(
                        "m", Objects.newHashMap("a", 1, "b", 2, "d", 5),
                        "a", Objects.newArrayList(Objects.newHashMap("a", "1", "b", "aaa"), "2", "44"));

                Map<String, Object> _m1 = Objects.copy(m1);

                //just iterate
                Objects.iterate(m1, new Closure<ObjectIterator.IterateBean, Object>() {
                    @Override
                    public Object call(ObjectIterator.IterateBean input) {
                        if (input.getPath().equals("a[0].b"))
                            return Objects.newHashMap("x", "y");
                        return input.getValue();
                    }
                });
                assertTrue(Objects.equals(m1, _m1));

                //change m1
                m1 = Objects.iterate(m1, new Closure<ObjectIterator.IterateBean, Object>() {
                    @Override
                    public Object call(ObjectIterator.IterateBean input) {
                        if (input.getPath().equals("a[0].b"))
                            return Objects.newHashMap("x", "y");
                        return input.getValue();
                    }
                });
                assertTrue(!Objects.equals(m1, _m1));

                Objects.set(_m1, "a[0].b", Objects.newHashMap("x", "y"));

                assertTrue(Objects.equals(m1, _m1));

                
            }
        }));
    }

    @Test 
	public void testWire(){
        int p = 10;

        assertEquals(p, LoadTestUtils.run("test", p, p, new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int index) throws Exception  {

                Date dt = new Date();
                Map<String, Object> m1 = Objects.newHashMap(
                        "m", Objects.newHashMap("a", 1, "b", 2, "d", 5, "dt", dt),
                        "a", Objects.newArrayList(Objects.newHashMap("a", "1", "b", "aaa"), "2", "44"));

                Map<String, Object> _m1 = Objects.copy(m1);

                m1 = Objects.toWire(m1);

                assertEquals("/Date(" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(dt) + ")/", Objects.get(m1, "m.dt"));
                assertTrue(!Objects.equals(_m1, m1));

                m1 = Objects.fromWire(m1);

                assertTrue(Objects.equals(_m1, m1));

                
            }
        }));
    }

    @Test 
	public void testParseFormatDate(){
        int p = 10;

        assertEquals(p, LoadTestUtils.run("test", p, p, new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int index) throws Exception  {

                Date dt = new Date();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                String str = sdf.format(dt);
                Date dt1 = Objects.parseDate(str, "yyyy-MM-dd HH:mm:ss.SSS");
                String str1 = Objects.formatDate(dt, "yyyy-MM-dd HH:mm:ss.SSS");
                assertTrue(Objects.equals(dt, dt1));
                assertTrue(Objects.equals(str, str1));
                
            }
        }));
    }

    @Test 
	public void testAssert(){
        int p = 10;

        assertEquals(p, LoadTestUtils.run("test", p, p, new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int index) throws Exception  {
                boolean b = false;

                b = false;
                try{
                    Objects.assertNotEmpty("qwer",null);
                }catch (BadDataException e){
                    assertEquals("qwer",e.getMessage());
                    b = true;
                }
                assertTrue(b);

                b = false;
                try{
                    Objects.assertTrue("qwer",false);
                }catch (BadDataException e){
                    assertEquals("qwer",e.getMessage());
                    b = true;
                }
                assertTrue(b);

                
            }
        }));
    }

    @Test
    public void testAsList() {
        int p = 10;

        assertEquals(p, LoadTestUtils.run("test", p, p, new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int index) throws Exception  {
                assertTrue(Objects.asList(null).size()==0);
                assertTrue(Objects.asList(null,true).size()==1);
                assertTrue(Objects.asList(1).size()==1);
                assertTrue(Objects.asList(1).get(0)==1);
                assertTrue(Objects.asList(Objects.newArrayList(1)).size()==1);
                assertTrue(Objects.asList(Objects.newArrayList(1,2)).size()==2);
                assertTrue(Objects.equals(Objects.asList(Objects.newArrayList(1)).get(0),1));

            }
        }));
    }
}
