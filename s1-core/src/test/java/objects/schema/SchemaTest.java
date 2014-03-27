package objects.schema;

import org.s1.misc.Closure;
import org.s1.objects.Objects;
import org.s1.objects.schema.*;
import org.s1.objects.schema.errors.CustomValidationException;
import org.s1.objects.schema.errors.DeniedAttributeException;
import org.s1.objects.schema.errors.ValidationException;
import org.s1.test.BasicTest;
import org.s1.test.LoadTestUtils;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.Map;

/**
 * s1v2
 * User: GPykhov
 * Date: 13.01.14
 * Time: 10:40
 */
public class SchemaTest extends BasicTest{

    public void testSimpleType1(){
        title("simple types");
        int p=10;
        assertEquals(p, LoadTestUtils.run("test", p, p, new Closure<Integer, Object>() {
            @Override
            public Object call(Integer input) {
                ObjectSchema s = new ObjectSchema(
                        new SimpleTypeAttribute("str", "string", String.class),
                        new SimpleTypeAttribute("int", "integer", Integer.class),
                        new SimpleTypeAttribute("bool", "boolean", Boolean.class),
                        new SimpleTypeAttribute("fl", "float", Float.class),
                        new SimpleTypeAttribute("dbl", "double", Double.class),
                        new SimpleTypeAttribute("l", "long", Long.class),
                        new SimpleTypeAttribute("bi", "bigint", BigInteger.class),
                        new SimpleTypeAttribute("bd", "decimal", BigDecimal.class),
                        new SimpleTypeAttribute("dt", "date", Date.class),
                        new SimpleTypeAttribute("o", "object", Object.class),
                        new SimpleTypeAttribute("st", "object2", SchemaTest.class)
                );

                //System.out.println(s.toMap());
                Map<String, Object> data = Objects.newHashMap(
                        "str", "qwer",
                        "int", 1,
                        "bool", true,
                        "fl", 2.4F,
                        "dbl", 2.1D,
                        "l", 2L,
                        "bi", new BigInteger("2"),
                        "bd", new BigDecimal(1.2D),
                        "dt", new Date(),
                        "o", "o1",
                        "st", "st1"
                );

                Map<String, Object> data1 = null;
                try {
                    data1 = s.validate(data);
                } catch (ValidationException e) {
                    throw new RuntimeException(e);
                }

                assertEquals(Objects.get(data, "str"), Objects.get(data1, "str"));
                assertEquals(Objects.get(data, "int"), Objects.get(data1, "int"));
                assertEquals(Objects.get(data, "bool"), Objects.get(data1, "bool"));
                assertEquals(Objects.get(data, "fl"), Objects.get(data1, "fl"));
                assertEquals(Objects.get(data, "dbl"), Objects.get(data1, "dbl"));
                assertEquals(Objects.get(data, "l"), Objects.get(data1, "l"));
                assertEquals(Objects.get(data, "bi"), Objects.get(data1, "bi"));
                assertEquals(Objects.get(data, "bd"), Objects.get(data1, "bd"));
                assertEquals(Objects.get(data, "dt"), Objects.get(data1, "dt"));
                assertEquals(Objects.get(data, "o"), Objects.get(data1, "o"));
                assertEquals(Objects.get(data, "st"), Objects.get(data1, "st"));

                assertEquals(String.class, Objects.get(data1, "str").getClass());
                assertEquals(Integer.class, Objects.get(data1, "int").getClass());
                assertEquals(Boolean.class, Objects.get(data1, "bool").getClass());
                assertEquals(Float.class, Objects.get(data1, "fl").getClass());
                assertEquals(Double.class, Objects.get(data1, "dbl").getClass());
                assertEquals(Long.class, Objects.get(data1, "l").getClass());
                assertEquals(BigInteger.class, Objects.get(data1, "bi").getClass());
                assertEquals(BigDecimal.class, Objects.get(data1, "bd").getClass());
                assertEquals(Date.class, Objects.get(data1, "dt").getClass());
                assertEquals(String.class, Objects.get(data1, "o").getClass());
                assertEquals(String.class, Objects.get(data1, "st").getClass());

                return null;
            }
        }));
    }

    public void testMapLax(){
        title("map");
        int p=1;
        assertEquals(p, LoadTestUtils.run("test", p, p, new Closure<Integer, Object>() {
            @Override
            public Object call(Integer input) {

                ObjectSchema s = new ObjectSchema(ObjectSchema.Mode.LAX,
                        new MapAttribute("m", "map",
                                new SimpleTypeAttribute("str", "string", String.class)
                        )
                );

                Map<String, Object> data = Objects.newHashMap(
                        "m", Objects.newHashMap(
                        "str", "qwer",
                        "str1", "asdf"
                )
                );

                Map<String, Object> data1 = null;
                try {
                    data1 = s.validate(data);
                } catch (ValidationException e) {
                    throw new RuntimeException(e);
                }

                assertTrue(Objects.equals(data, data1));
                assertEquals("qwer", Objects.get(data1, "m.str"));
                assertEquals("asdf", Objects.get(data1, "m.str1"));

                return null;
            }
        }));
    }

    public void testMapStrict(){
        title("map");
        int p=10;
        assertEquals(p, LoadTestUtils.run("test", p, p, new Closure<Integer, Object>() {
            @Override
            public Object call(Integer input) {

                ObjectSchema s = new ObjectSchema(ObjectSchema.Mode.STRICT,
                        new MapAttribute("m", "map",
                                new SimpleTypeAttribute("str", "string", String.class)
                        )
                );

                Map<String, Object> data = Objects.newHashMap(
                        "m", Objects.newHashMap(
                                "str", "qwer",
                                "str1", "asdf"
                        )
                );

                boolean b = false;
                Map<String, Object> data1 = null;
                try {
                    data1 = s.validate(data);
                } catch (DeniedAttributeException e) {
                    b = true;
                } catch (ValidationException e) {
                    throw new RuntimeException(e);
                }

                assertTrue(b);
                return null;
            }
        }));
    }

    public void testMapList(){
        title("maps and lists");
        int p=10;
        assertEquals(p, LoadTestUtils.run("test", p, p, new Closure<Integer, Object>() {
            @Override
            public Object call(Integer input) {

                ObjectSchema s = new ObjectSchema(ObjectSchema.Mode.LAX,
                        new MapAttribute("m", "map",
                                new SimpleTypeAttribute("str", "string", String.class),
                                new MapAttribute("m", "map",
                                        new SimpleTypeAttribute("str", "string", String.class),
                                        new SimpleTypeAttribute("i", "integer", Integer.class),
                                        new ListAttribute("lst1", "list1", new SimpleTypeAttribute(null, null, String.class)),
                                        new ListAttribute("lst2", "list2", new MapAttribute(null, null,
                                                new SimpleTypeAttribute("str", "string", String.class),
                                                new SimpleTypeAttribute("i", "integer", Integer.class)
                                        ))
                                )
                        )
                );

                Map<String, Object> data = Objects.newHashMap(
                        "m", Objects.newHashMap(
                        "str", "qwer",
                        "str1", "asdf",
                        "m", Objects.newHashMap(
                        "str", "qwer",
                        "i", "123",
                        "lst1", Objects.newArrayList(1, 2, 3),
                        "lst2", Objects.newArrayList(
                        Objects.newHashMap(
                                "str", "qwer",
                                "i", 123
                        )
                )
                )
                )
                );

                Map<String, Object> data2 = Objects.newHashMap(
                        "m", Objects.newHashMap(
                        "str", "qwer",
                        "str1", "asdf",
                        "m", Objects.newHashMap(
                        "str", "qwer",
                        "i", 123,
                        "lst1", Objects.newArrayList("1", "2", "3"),
                        "lst2", Objects.newArrayList(
                        Objects.newHashMap(
                                "str", "qwer",
                                "i", 123
                        )
                )
                )
                )
                );

                Map<String, Object> data1 = null;
                try {
                    data1 = s.validate(data);
                } catch (ValidationException e) {
                    throw new RuntimeException(e);
                }

                assertTrue(Objects.equals(data2, data1));
                assertEquals("qwer", Objects.get(data1, "m.str"));
                assertEquals("asdf", Objects.get(data1, "m.str1"));
                assertEquals("qwer", Objects.get(data1, "m.m.str"));
                assertEquals(123, Objects.get(data1, "m.m.i"));
                assertEquals("1", Objects.get(data1, "m.m.lst1[0]"));
                assertEquals("2", Objects.get(data1, "m.m.lst1[1]"));
                assertEquals("3", Objects.get(data1, "m.m.lst1[2]"));
                assertEquals("qwer", Objects.get(data1, "m.m.lst2[0].str"));
                assertEquals(123, Objects.get(data1, "m.m.lst2[0].i"));

                return null;
            }
        }));
    }

    public void testList1(){
        title("list of strings");
        int p=10;
        assertEquals(p, LoadTestUtils.run("test", p, p, new Closure<Integer, Object>() {
            @Override
            public Object call(Integer input) {

                ObjectSchema s = new ObjectSchema(
                        new ListAttribute("l", "list",
                                new SimpleTypeAttribute("str", "string", String.class)
                        )
                );

                Map<String, Object> data = Objects.newHashMap(
                        "l", Objects.newArrayList("1", "2", "3")
                );

                Map<String, Object> data1 = null;
                try {
                    data1 = s.validate(data);
                } catch (ValidationException e) {
                    throw new RuntimeException(e);
                }

                assertTrue(Objects.equals(data, data1));
                assertEquals("1", Objects.get(data1, "l[0]"));
                assertEquals("2", Objects.get(data1, "l[1]"));
                assertEquals("3", Objects.get(data1, "l[2]"));

                return null;
            }
        }));
    }

    public void testList2(){
        title("list with maps");
        int p=10;
        assertEquals(p, LoadTestUtils.run("test", p, p, new Closure<Integer, Object>() {
            @Override
            public Object call(Integer input) {

                ObjectSchema s = new ObjectSchema(
                        new ListAttribute("l", "list",
                                new MapAttribute(null, null,
                                        new SimpleTypeAttribute("str", "string", String.class)
                                )
                        )
                );

                Map<String, Object> data = Objects.newHashMap(
                        "l", Objects.newArrayList(
                        Objects.newHashMap("str", "1"),
                        Objects.newHashMap("str", "2"),
                        Objects.newHashMap("str", "3")
                )
                );

                Map<String, Object> data1 = null;
                try {
                    data1 = s.validate(data);
                } catch (ValidationException e) {
                    throw new RuntimeException(e);
                }

                assertTrue(Objects.equals(data, data1));
                assertEquals("1", Objects.get(data1, "l[0].str"));
                assertEquals("2", Objects.get(data1, "l[1].str"));
                assertEquals("3", Objects.get(data1, "l[2].str"));

                ObjectSchema.ValidateResultBean vr = s.validateQuite(data);
                assertTrue(Objects.equals(data, vr.getValidatedData()));
                assertEquals(3, ((ListAttribute) vr.getResolvedSchema().getAttributes().get(0)).getList().size());
                assertTrue(
                        Objects.equals(Objects.newHashMap("str", "1"),
                                ((ListAttribute) vr.getResolvedSchema().getAttributes().get(0)).getList().get(0).getData())
                );

                return null;
            }
        }));
    }

    public void testDefault1(){
        title("default list");
        int p=10;
        assertEquals(p, LoadTestUtils.run("test", p, p, new Closure<Integer, Object>() {
            @Override
            public Object call(Integer input) {

                ObjectSchema s = new ObjectSchema(
                        new ListAttribute("l", "list",
                                new SimpleTypeAttribute("str", "string", String.class)
                        ).setRequired(true).setDefault(Objects.newArrayList("1", "2", "3"))
                );

                Map<String, Object> data = Objects.newHashMap();

                Map<String, Object> data1 = null;
                try {
                    data1 = s.validate(data);
                } catch (ValidationException e) {
                    throw new RuntimeException(e);
                }

                assertEquals("1", Objects.get(data1, "l[0]"));
                assertEquals("2", Objects.get(data1, "l[1]"));
                assertEquals("3", Objects.get(data1, "l[2]"));

                return null;
            }
        }));
    }

    public void testDefault2(){
        title("default string");
        int p=10;
        assertEquals(p, LoadTestUtils.run("test", p, p, new Closure<Integer, Object>() {
            @Override
            public Object call(Integer input) {

                ObjectSchema s = new ObjectSchema(
                        new SimpleTypeAttribute("str", "string", String.class).setRequired(true).setDefault("123")
                );

                Map<String, Object> data = Objects.newHashMap();

                Map<String, Object> data1 = null;
                try {
                    data1 = s.validate(data);
                } catch (ValidationException e) {
                    throw new RuntimeException(e);
                }

                assertEquals("123", Objects.get(data1, "str"));

                return null;
            }
        }));
    }

    public void testVariants1(){
        title("variants");
        int p=10;
        assertEquals(p, LoadTestUtils.run("test", p, p, new Closure<Integer, Object>() {
            @Override
            public Object call(Integer input) {

                ObjectSchema s = new ObjectSchema(
                        new SimpleTypeAttribute("str", "string", String.class).setVariants("1", "2")
                );

                Map<String, Object> data = Objects.newHashMap("str", "1");

                Map<String, Object> data1 = null;
                try {
                    data1 = s.validate(data);
                } catch (ValidationException e) {
                    throw new RuntimeException(e);
                }

                assertEquals("1", Objects.get(data1, "str"));

                //error
                data = Objects.newHashMap("str", "111");

                try {
                    s.validate(data);
                    throw new RuntimeException("error");
                } catch (ValidationException e) {

                }

                return null;
            }
        }));
    }

    public void testReference1(){
        title("reference");
        int p=10;
        assertEquals(p, LoadTestUtils.run("test", p, p, new Closure<Integer, Object>() {
            @Override
            public Object call(Integer input) {

                ObjectSchema s = new ObjectSchema(
                        new ReferenceAttribute("t", "type", "type1"),
                        new ObjectSchemaType("type1",
                                new SimpleTypeAttribute("str", "string", String.class).setRequired(true),
                                new SimpleTypeAttribute("i", "int", Integer.class)
                        )
                );

                Map<String, Object> data = Objects.newHashMap("t", Objects.newHashMap(
                        "str", "qwer",
                        "i", 123
                ));

                Map<String, Object> data1 = null;
                try {
                    data1 = s.validate(data);
                } catch (ValidationException e) {
                    throw new RuntimeException(e);
                }
                assertTrue(Objects.equals(data, data1));
                assertEquals("qwer", Objects.get(data1, "t.str"));
                assertEquals(123, Objects.get(data1, "t.i"));

                //null ok
                data = Objects.newHashMap();
                try {
                    s.validate(data);
                } catch (ValidationException e) {
                    throw new RuntimeException(e);
                }

                //error
                data = Objects.newHashMap("t", "111");
                try {
                    s.validate(data);
                    throw new RuntimeException("error");
                } catch (ValidationException e) {

                }

                //error2
                data = Objects.newHashMap("t", Objects.newHashMap());
                try {
                    s.validate(data);
                    throw new RuntimeException("error");
                } catch (ValidationException e) {

                }

                return null;
            }
        }));
    }

    public void testReference2(){
        title("cycle reference");
        int p=10;
        assertEquals(p, LoadTestUtils.run("test", p, p, new Closure<Integer, Object>() {
            @Override
            public Object call(Integer input) {

                ObjectSchema s = new ObjectSchema(
                        new ReferenceAttribute("t", "type", "type1"),
                        new ObjectSchemaType("type1",
                                new SimpleTypeAttribute("str", "string", String.class).setRequired(true),
                                new SimpleTypeAttribute("i", "int", Integer.class),
                                new ReferenceAttribute("t", "type", "type1")
                        )
                );

                Map<String, Object> data = Objects.newHashMap("t", Objects.newHashMap(
                        "str", "qwer",
                        "i", 123,
                        "t", Objects.newHashMap("str", "qwer", "i", 123, "t", Objects.newHashMap("str", "qwer", "i", 123))
                ));

                Map<String, Object> data1 = null;
                try {
                    data1 = s.validate(data);
                } catch (ValidationException e) {
                    throw new RuntimeException(e);
                }
                assertTrue(Objects.equals(data, data1));
                assertEquals("qwer", Objects.get(data1, "t.str"));
                assertEquals(123, Objects.get(data1, "t.i"));
                assertEquals("qwer", Objects.get(data1, "t.t.str"));
                assertEquals(123, Objects.get(data1, "t.t.i"));
                assertEquals("qwer", Objects.get(data1, "t.t.t.str"));
                assertEquals(123, Objects.get(data1, "t.t.t.i"));

                //error2
                data = Objects.newHashMap("t", Objects.newHashMap(
                        "str", "qwer",
                        "i", 123,
                        "t", Objects.newHashMap("str", "qwer", "i", 123, "t", Objects.newHashMap())
                ));
                try {
                    s.validate(data);
                    throw new RuntimeException("error");
                } catch (ValidationException e) {

                }

                return null;
            }
        }));
    }

    public void testListMinMax(){
        title("list min max");
        int p=10;
        assertEquals(p, LoadTestUtils.run("test", p, p, new Closure<Integer, Object>() {
            @Override
            public Object call(Integer input) {

                ObjectSchema s = new ObjectSchema(
                        new ListAttribute("l", "list",
                                new SimpleTypeAttribute("str", "string", String.class), 1, 2
                        )
                );

                Map<String, Object> data = Objects.newHashMap(
                        "l", Objects.newArrayList("1", "2")
                );

                Map<String, Object> data1 = null;
                try {
                    data1 = s.validate(data);
                } catch (ValidationException e) {
                    throw new RuntimeException(e);
                }

                assertTrue(Objects.equals(data, data1));
                assertEquals("1", Objects.get(data1, "l[0]"));
                assertEquals("2", Objects.get(data1, "l[1]"));

                //error min
                data = Objects.newHashMap("l", Objects.newArrayList());
                try {
                    data1 = s.validate(data);
                    throw new RuntimeException("error");
                } catch (ValidationException e) {

                }

                //error max
                data = Objects.newHashMap("l", Objects.newArrayList(1, 2, 3));
                try {
                    data1 = s.validate(data);
                    throw new RuntimeException("error");
                } catch (ValidationException e) {

                }

                return null;
            }
        }));
    }

    public static class TestComplexType extends ComplexType{
        @Override
        public Map<String, Object> expand(Map<String, Object> m, boolean expand) throws Exception {
            m.put("y",m.get("x")+"_"+ config.get("a"));
            if(expand)
                m.put("z",m.get("x")+"_1234");
            return m;
        }

        @Override
        public Map<String, Object> validate(Map<String, Object> m) throws ValidationException {
            if(!m.containsKey("x"))
                throw new CustomValidationException("x not found");
            return m;
        }
    }

    public void testComplexType(){
        title("complex type");
        int p=10;
        assertEquals(p, LoadTestUtils.run("test", p, p, new Closure<Integer, Object>() {
            @Override
            public Object call(Integer input) {

                Map<String, Object> cfg = Objects.newHashMap("a", "123");
                ObjectSchema s = new ObjectSchema(
                        new ComplexTypeAttribute("ct", "ct1", TestComplexType.class, cfg)
                );

                Map<String, Object> data = Objects.newHashMap(
                        "ct", Objects.newHashMap(
                        "x", "qwer"
                )
                );

                Map<String, Object> data1 = null;
                try {
                    data1 = s.validate(data);
                } catch (ValidationException e) {
                    throw new RuntimeException(e);
                }

                assertEquals("qwer", Objects.get(data1, "ct.x"));
                assertNull(Objects.get(data1, "ct.y"));
                assertNull(Objects.get(data1, "ct.z"));

                //expand
                try {
                    data1 = s.validate(data, true, false, null);
                } catch (ValidationException e) {
                    throw new RuntimeException(e);
                }

                assertEquals("qwer", Objects.get(data1, "ct.x"));
                assertEquals("qwer_123", Objects.get(data1, "ct.y"));
                assertNull(Objects.get(data1, "ct.z"));

                //expand deep
                try {
                    data1 = s.validate(data, true, true, null);
                } catch (ValidationException e) {
                    throw new RuntimeException(e);
                }

                assertEquals("qwer", Objects.get(data1, "ct.x"));
                assertEquals("qwer_123", Objects.get(data1, "ct.y"));
                assertEquals("qwer_1234", Objects.get(data1, "ct.z"));


                //error
                data = Objects.newHashMap(
                        "ct", Objects.newHashMap()
                );
                try {
                    data1 = s.validate(data, true, true, null);
                    throw new RuntimeException("error");
                } catch (ValidationException e) {

                }

                return null;
            }
        }));
    }

    public void testValidate(){
        title("custom validate");
        int p=10;
        assertEquals(p, LoadTestUtils.run("test", p, p, new Closure<Integer, Object>() {
            @Override
            public Object call(Integer input) {

                ObjectSchema s = new ObjectSchema(
                        new SimpleTypeAttribute("str", "string", String.class).setValidate(new Closure<ObjectSchemaAttribute, String>() {
                            @Override
                            public String call(ObjectSchemaAttribute input) {
                                if (!"qwer".equals(input.getData()))
                                    return "err1";
                                return null;
                            }
                        })
                );

                Map<String, Object> data = Objects.newHashMap(
                        "str", "qwer"
                );

                Map<String, Object> data1 = null;
                try {
                    data1 = s.validate(data);
                } catch (ValidationException e) {
                    throw new RuntimeException(e);
                }

                //error
                data = Objects.newHashMap(
                        "str", "qwer1"
                );
                try {
                    data1 = s.validate(data);
                    throw new RuntimeException("error");
                } catch (ValidationException e) {
                    if (input.equals(0))
                        trace(e.getMessage());
                }

                return null;
            }
        }));
    }

    public void testDynamic1(){
        title("dynamic form field");
        int p=10;
        assertEquals(p, LoadTestUtils.run("test", p, p, new Closure<Integer, Object>() {
            @Override
            public Object call(Integer input) {

                ObjectSchema s = new ObjectSchema(
                        new SimpleTypeAttribute("str", "string", String.class),
                        new SimpleTypeAttribute("str1", "string", String.class).setScript(new Closure<ObjectSchemaAttribute, ObjectSchemaAttribute>() {
                            @Override
                            public ObjectSchemaAttribute call(ObjectSchemaAttribute input) {
                                if ("qwer".equals(Objects.get((Map<String, Object>) input.getParent().getData(), "str")))
                                    input.setRequired(true);
                                return null;
                            }
                        })
                );

                Map<String, Object> data = Objects.newHashMap(
                        "str", "qwer1"
                );

                Map<String, Object> data1 = null;
                try {
                    data1 = s.validate(data);
                } catch (ValidationException e) {
                    throw new RuntimeException(e);
                }

                //error
                data = Objects.newHashMap(
                        "str", "qwer"
                );
                try {
                    data1 = s.validate(data);
                    throw new RuntimeException("error");
                } catch (ValidationException e) {
                    if (input.equals(0))
                        trace(e.getMessage());
                }

                return null;
            }
        }));
    }

    public void testDynamic2(){
        title("change entire attribute");
        int p=10;
        assertEquals(p, LoadTestUtils.run("test", p, p, new Closure<Integer, Object>() {
            @Override
            public Object call(Integer input) {

                ObjectSchema s = new ObjectSchema(
                        new SimpleTypeAttribute("str1", "str1", String.class).setScript(new Closure<ObjectSchemaAttribute, ObjectSchemaAttribute>() {
                            @Override
                            public ObjectSchemaAttribute call(ObjectSchemaAttribute input) {
                                return new MapAttribute("m", "map",
                                        new SimpleTypeAttribute("str", "str", String.class).setRequired(true)).setRequired(true);
                            }
                        })
                );

                Map<String, Object> data = Objects.newHashMap(
                        "str1", Objects.newHashMap("str", "qwer")
                );

                Map<String, Object> data1 = null;
                try {
                    data1 = s.validate(data);
                } catch (ValidationException e) {
                    throw new RuntimeException(e);
                }

                //error
                data = Objects.newHashMap(
                        "str1", "qwer"
                );
                try {
                    data1 = s.validate(data);
                    throw new RuntimeException("error");
                } catch (ValidationException e) {
                    if (input.equals(0))
                        trace(e.getMessage());
                }

                return null;
            }
        }));
    }

    public void testDynamic3(){
        title("dynamic list");
        int p=10;
        assertEquals(p, LoadTestUtils.run("test", p, p, new Closure<Integer, Object>() {
            @Override
            public Object call(Integer input) {

                ObjectSchema s = new ObjectSchema(
                        new ListAttribute("l", "list", new MapAttribute(null, null,
                                new SimpleTypeAttribute("a", "a", String.class).setScript(new Closure<ObjectSchemaAttribute, ObjectSchemaAttribute>() {
                                    @Override
                                    public ObjectSchemaAttribute call(ObjectSchemaAttribute input) {
                                        //required if first
                                        String name = input.getParent().getName();
                                        if ("0".equals(name))
                                            input.setRequired(true);
                                        return null;
                                    }
                                }),
                                new SimpleTypeAttribute("b", "b", String.class).setScript(new Closure<ObjectSchemaAttribute, ObjectSchemaAttribute>() {
                                    @Override
                                    public ObjectSchemaAttribute call(ObjectSchemaAttribute input) {
                                        //required if last
                                        String name = input.getParent().getName();
                                        ListAttribute la = (ListAttribute) input.getParent().getParent();
                                        if (("" + (la.getList().size() - 1)).equals(name))
                                            input.setRequired(true);
                                        return null;
                                    }
                                })))
                );

                Map<String, Object> data = Objects.newHashMap(
                        "l", Objects.newArrayList(Objects.newHashMap("a", "qwer"), Objects.newHashMap("b", "qwer"))
                );

                Map<String, Object> data1 = null;
                try {
                    data1 = s.validate(data);
                } catch (ValidationException e) {
                    throw new RuntimeException(e);
                }

                //error
                data = Objects.newHashMap(
                        "l", Objects.newArrayList(Objects.newHashMap("b", "qwer"), Objects.newHashMap("b", "qwer"))
                );
                try {
                    data1 = s.validate(data);
                    throw new RuntimeException("error");
                } catch (ValidationException e) {
                    if (input.equals(0))
                        trace(e.getMessage());
                }

                //error
                data = Objects.newHashMap(
                        "l", Objects.newArrayList(Objects.newHashMap("a", "qwer"), Objects.newHashMap("a", "qwer"))
                );
                try {
                    data1 = s.validate(data);
                    throw new RuntimeException("error");
                } catch (ValidationException e) {
                    if (input.equals(0))
                        trace(e.getMessage());
                }

                return null;
            }
        }));
    }

    public void testErrorFormat(){
        title("error format");
        int p=10;
        assertEquals(p, LoadTestUtils.run("test", p, p, new Closure<Integer, Object>() {
            @Override
            public Object call(Integer input) {

                ObjectSchema s = new ObjectSchema(
                        new SimpleTypeAttribute("str", "str", String.class).setRequired(true),
                        new MapAttribute("m", "map",
                                new SimpleTypeAttribute("str", "str", String.class).setRequired(true)).setRequired(true),
                        new ListAttribute("l", "list", new MapAttribute("m", "map",
                                new SimpleTypeAttribute("str", "str", String.class).setRequired(true))).setRequired(true)
                );

                Map<String, Object> data = Objects.newHashMap();

                //error string
                try {
                    s.validate(data);
                    throw new RuntimeException("error");
                } catch (ValidationException e) {
                    if (input.equals(0))
                        trace(e.getMessage());
                    assertTrue(e.getMessage().contains("str"));
                }

                //error map
                data = Objects.newHashMap("str", "a", "m", Objects.newHashMap());
                try {
                    s.validate(data);
                    throw new RuntimeException("error");
                } catch (ValidationException e) {
                    if (input.equals(0))
                        trace(e.getMessage());
                    assertTrue(e.getMessage().contains("map / str"));
                }

                //error list
                data = Objects.newHashMap("str", "a", "m", Objects.newHashMap("str", "a"), "l", Objects.newArrayList(Objects.newHashMap("str", "a"), Objects.newHashMap()));
                try {
                    s.validate(data);
                    throw new RuntimeException("error");
                } catch (ValidationException e) {
                    if (input.equals(0))
                        trace(e.getMessage());
                    assertTrue(e.getMessage().contains("list / 1 / str"));
                }

                return null;
            }
        }));
    }

}
