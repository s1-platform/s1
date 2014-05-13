package objects;

import org.s1.objects.MapMethod;
import org.s1.objects.MapMethodWrapper;
import org.s1.objects.MapSerializableObject;
import org.s1.objects.Objects;
import org.s1.testing.BasicTest;
import org.s1.testing.LoadTestUtils;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

/**
 * @author Grigory Pykhov
 */
public class MapMethodWrapperTest extends BasicTest {

    @MapMethod
    public void n(){
        trace("VOID");
    }

    @MapMethod(names = {"a","b"})
    public String a(String a, int b){
        return a+":"+b;
    }

    @MapMethod
    public String b(String a,M1 b){
        return a+":"+b.getA()+":"+b.getB();
    }

    @MapMethod
    public String c(String a,List<M1> b){
        for(M1 m:b) {
            trace(a + "::" + m.getA() + ":" + m.getB());
        }
        return a+":"+b.size();
    }

    @MapMethod
    public String d(String a,List<List<M1>> b){
        for(List<M1> l:b) {
            for(M1 m:l) {
                trace(a + "::" + m.getA() + ":" + m.getB());
            }
        }
        return a+":"+b.size();
    }

    @MapMethod(names = {"a","b"})
    public String map1(String a,Map<M1,List<M1>> b){
        for(M1 l:b.keySet()) {
            for(M1 m:b.get(l)) {
                trace(a + "::" +
                        l.getA() + ":" + l.getB()+"->"+
                        m.getA() + ":" + m.getB());
            }
        }
        return a+":"+b.size();
    }

    public static class M1 implements MapSerializableObject {
        private int a;
        private String b;

        public int getA() {
            return a;
        }

        public void setA(int a) {
            this.a = a;
        }

        public String getB() {
            return b;
        }

        public void setB(String b) {
            this.b = b;
        }

        @Override
        public Map<String, Object> toMap() {
            return Objects.newSOHashMap("a",a,"b",b);
        }

        @Override
        public void fromMap(Map<String, Object> m) {
            a = Objects.get(m,"a");
            b = Objects.get(m,"b");
        }
    }

    @Test
    public void testInvoke(){
        int p = 10;

        assertEquals(p, LoadTestUtils.run("test", p, p, new LoadTestUtils.LoadTestProcedure() {
            @Override
            public void call(int index) throws Exception {
                MapMethodWrapperTest m = new MapMethodWrapperTest();
                MapMethodWrapper.findAndInvoke(m,"n");

                assertEquals("qwer:1",MapMethodWrapper.findAndInvoke(m,"a","qwer","1"));
                assertEquals("qwer:2:asd",MapMethodWrapper.findAndInvoke(m,"b","qwer",Objects.newSOHashMap("a",2,"b","asd")));
                assertEquals("qwer:2",MapMethodWrapper.findAndInvoke(m,"c","qwer",Objects.newArrayList(
                        Objects.newSOHashMap("a",1,"b","asd1"),
                        Objects.newSOHashMap("a",2,"b","asd2"))));
                assertEquals("qwer:2",MapMethodWrapper.findAndInvoke(m,"d","qwer",Objects.newArrayList(
                        Objects.newArrayList(
                        Objects.newSOHashMap("a",1,"b","asd1")),
                        Objects.newArrayList(
                        Objects.newSOHashMap("a", 2, "b", "asd2")))));
                assertEquals("qwer:2",MapMethodWrapper.findAndInvoke(m,"map1","qwer", Objects.newHashMap(
                        Objects.newSOHashMap("a", 1, "b", "zxc1"),
                        Objects.newArrayList(
                                Objects.newSOHashMap("a", 1, "b", "asd1")),
                        Objects.newSOHashMap("a", 1, "b", "zxc2"),
                        Objects.newArrayList(
                                Objects.newSOHashMap("a", 2, "b", "asd2"))
                )));

                assertEquals("qwer:2",MapMethodWrapper.findAndInvoke(m,"map1", Objects.newSOHashMap(
                        "a","qwer",
                        "b",Objects.newHashMap(
                                Objects.newSOHashMap("a", 1, "b", "zxc1"),
                                Objects.newArrayList(
                                        Objects.newSOHashMap("a", 1, "b", "asd1")),
                                Objects.newSOHashMap("a", 1, "b", "zxc2"),
                                Objects.newArrayList(
                                        Objects.newSOHashMap("a", 2, "b", "asd2"))
                        )
                )));
            }
        }));
    }

}