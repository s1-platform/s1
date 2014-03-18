package cache;

import org.s1.cache.Cache;
import org.s1.misc.Closure;
import org.s1.test.BasicTest;
import org.s1.test.LoadTestUtils;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * s1v2
 * User: GPykhov
 * Date: 17.01.14
 * Time: 15:11
 */
public class CacheTest extends BasicTest {

    public void testGet(){
        int p = 10;
        int size = 100;
        final Cache c = new Cache(size);
        final AtomicInteger i = new AtomicInteger(0);
        title("Put if not found, parallel "+p);
        assertEquals(p*1000, LoadTestUtils.run("test", p * 1000, p, new Closure<Integer, Object>() {
            @Override
            public Object call(Integer index) {

                final String label0 = "test-0";
                final String label1 = "aaa-" + (index % 5);
                final String label2 = "bbb-" + (index % 1000);

                String a2 = c.get(label2, new Closure<String, String>() {
                    @Override
                    public String call(String input) {
                        i.incrementAndGet();
                        return label2;
                    }
                });
                String a1 = c.get(label1, new Closure<String, String>() {
                    @Override
                    public String call(String input) {
                        i.incrementAndGet();
                        return label1;
                    }
                });
                String a0 = c.get(label0, new Closure<String, String>() {
                    @Override
                    public String call(String input) {
                        i.incrementAndGet();
                        return label0;
                    }
                });
                assertEquals(label0, a0);
                assertEquals(label1, a1);
                assertEquals(label2, a2);

                //String p = Options.getStorage().getParameter("config");

                return null;
            }
        }));
        trace(i.get());
        //assertTrue(i.get()>1);
        //assertTrue(i.get()<=p);
        trace(c.getCache().size());
        trace(c.getCache());
        trace(c.getGetsStat());
        c.getCache().put("a",123);
        assertTrue(!c.getCache().containsKey("a"));
        assertTrue(c.getCache().size()<=size);

        c.get("test-0",new Closure<String, String>() {
            @Override
            public String call(String input) {
                throw new RuntimeException("error");
            }
        });
        c.get("aaa-0",new Closure<String, String>() {
            @Override
            public String call(String input) {
                throw new RuntimeException("error");
            }
        });
        c.get("aaa-1",new Closure<String, String>() {
            @Override
            public String call(String input) {
                throw new RuntimeException("error");
            }
        });
        c.get("aaa-2",new Closure<String, String>() {
            @Override
            public String call(String input) {
                throw new RuntimeException("error");
            }
        });
        c.get("aaa-3",new Closure<String, String>() {
            @Override
            public String call(String input) {
                throw new RuntimeException("error");
            }
        });
        c.get("aaa-4",new Closure<String, String>() {
            @Override
            public String call(String input) {
                throw new RuntimeException("error");
            }
        });
    }

    public void testNested(){
        int p = 1000;
        int size = 100;
        final Cache c = new Cache(size);
        title("Nested, parallel "+p);
        assertEquals(p, LoadTestUtils.run("test", p, p, new Closure<Integer, Object>() {
            @Override
            public Object call(Integer index) {

                final String l1 = "test" + index;
                String s = c.get("test0", new Closure<String, String>() {
                    @Override
                    public String call(String input) {

                        c.get("test0", new Closure<String, String>() {
                            @Override
                            public String call(String input) {
                                sleep(10);
                                return "asdf";
                            }
                        });

                        c.get("test1", new Closure<String, String>() {
                            @Override
                            public String call(String input) {
                                c.get("test0", new Closure<String, String>() {
                                    @Override
                                    public String call(String input) {
                                        c.get("test1", new Closure<String, String>() {
                                            @Override
                                            public String call(String input) {
                                                sleep(10);
                                                return "qwer";
                                            }
                                        });
                                        sleep(10);
                                        return "asdf";
                                    }
                                });
                                sleep(10);
                                return "qwer";
                            }
                        });

                        return "asdf";
                    }
                });
                assertEquals("asdf", s);
                return null;
            }
        }));
    }

    public void testWeight(){
        title("Weight");
        final Cache c = new Cache(2);
        c.get("test1",new Closure<String, String>() {
            @Override
            public String call(String input) {
                return "test1";
            }
        });
        c.get("test1",new Closure<String, String>() {
            @Override
            public String call(String input) {
                return "test1";
            }
        });
        sleep(100);
        c.get("test2",new Closure<String, String>() {
            @Override
            public String call(String input) {
                return "test2";
            }
        });
        trace(c.getGetsStat());
        assertTrue(c.getCache().size()==2);
        assertTrue(c.getCache().containsKey("test1"));
        assertTrue(c.getCache().containsKey("test2"));
        sleep(100);

        //cause resize
        c.get("test3",new Closure<String, String>() {
            @Override
            public String call(String input) {
                return "test3";
            }
        });
        trace(c.getGetsStat());
        assertTrue(c.getCache().size()==2);
        assertTrue(c.getCache().containsKey("test1"));
        assertTrue(c.getCache().containsKey("test3"));
    }

    public void testTimeout(){
        title("Weight");
        TimeUnit tu = TimeUnit.SECONDS;
        long timeout = 1;
        final Cache c = new Cache(1,timeout, tu);
        trace("TTL: "+tu.toMillis(timeout)+"ms");

        String s = null;
        s = c.get("test1",new Closure<String, String>() {
            @Override
            public String call(String input) {
                return "test1";
            }
        });
        assertEquals("test1",s);
        s = c.get("test1",new Closure<String, String>() {
            @Override
            public String call(String input) {
                return "test2";
            }
        });
        assertEquals("test1",s);
        sleep(100);
        s = c.get("test1",new Closure<String, String>() {
            @Override
            public String call(String input) {
                return "test3";
            }
        });
        assertEquals("test1",s);
        sleep(1000);
        s = c.get("test1",new Closure<String, String>() {
            @Override
            public String call(String input) {
                return "test4";
            }
        });
        assertEquals("test4",s);

    }

    public void testInvalidate(){
        int p = 1000;
        int size = 100;
        final Cache c = new Cache(size);
        final AtomicInteger i = new AtomicInteger(0);
        title("Invalidate, parallel "+p);
        assertEquals(p, LoadTestUtils.run("test", p, p, new Closure<Integer, Object>() {
            @Override
            public Object call(Integer index) {

                if (index % 3 == 0) {
                    c.get("test", new Closure<String, String>() {
                        @Override
                        public String call(String input) {
                            return "test";
                        }
                    });
                } else if (index % 3 == 1) {
                    c.invalidate("test");
                } else if (index % 3 == 2) {
                    c.invalidateAll();
                }

                return null;
            }
        }));
        c.get("test",new Closure<String, String>() {
            @Override
            public String call(String input) {
                return "test";
            }
        });
        c.invalidate("test");
        String s1 = c.get("test",new Closure<String, String>() {
            @Override
            public String call(String input) {
                i.incrementAndGet();
                return "test123";
            }
        });
        assertEquals("test123",s1);
        assertEquals(1,i.get());

        c.invalidateAll();
        s1 = c.get("test",new Closure<String, String>() {
            @Override
            public String call(String input) {
                i.incrementAndGet();
                return "test1234";
            }
        });
        assertEquals("test1234",s1);
        assertEquals(2,i.get());

    }
}
