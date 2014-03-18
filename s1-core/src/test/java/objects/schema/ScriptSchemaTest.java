package objects.schema;

import org.s1.S1SystemError;
import org.s1.misc.Closure;
import org.s1.objects.Objects;
import org.s1.objects.schema.*;
import org.s1.objects.schema.errors.ObjectSchemaFormatException;
import org.s1.objects.schema.errors.ValidationException;
import org.s1.test.BasicTest;
import org.s1.test.LoadTestUtils;

import java.util.Map;

/**
 * s1v2
 * User: GPykhov
 * Date: 13.01.14
 * Time: 10:40
 */
public class ScriptSchemaTest extends BasicTest{

    public void testValidate(){
        title("custom validate");
        int p=10;
        assertEquals(p, LoadTestUtils.run("test", p, p, new Closure<Integer, Object>() {
            @Override
            public Object call(Integer input) {
                Map<String,Object> m = Objects.newHashMap(
                        "attributes",Objects.newArrayList(
                                Objects.newHashMap("name","str","label","string","appearance","normal","type","String",
                                        "validate","if('qwer'!=data) throw 'err1';")
                        )
                );
                ObjectSchema s = new ObjectSchema();
                try {
                    s.fromMap(m);
                } catch (ObjectSchemaFormatException e) {
                    throw S1SystemError.wrap(e);
                }

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
                    assertTrue(e.getMessage().contains("err1"));
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

                Map<String,Object> m = Objects.newHashMap(
                        "attributes",Objects.newArrayList(
                        Objects.newHashMap("name","str","label","string","appearance","normal","type","String"),
                        Objects.newHashMap("name","str1","label","string1","appearance","normal","type","String",
                                "script","if('qwer'==s1.get(parent,'data.str','')) attr.appearance='required'; ")
                )
                );
                ObjectSchema s = new ObjectSchema();
                try {
                    s.fromMap(m);
                } catch (ObjectSchemaFormatException e) {
                    throw S1SystemError.wrap(e);
                }

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
                boolean b = false;
                try {
                    data1 = s.validate(data);
                    throw new RuntimeException("error");
                } catch (ValidationException e) {
                    if (input.equals(0))
                        trace(e.getMessage());
                    b = true;
                }
                assertTrue(b);

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

                Map<String,Object> m = Objects.newHashMap(
                        "attributes",Objects.newArrayList(
                        Objects.newHashMap("name","str1","label","string1","appearance","normal","type","String",
                                "script","attr.type='Map'; attr.name='m'; attr.label='map'; attr.appearance = 'required'; attr.attributes = [{name:'str',label:'str',type:'String',appearance:'required'}]; ")
                )
                );
                ObjectSchema s = new ObjectSchema();
                try {
                    s.fromMap(m);
                } catch (ObjectSchemaFormatException e) {
                    throw S1SystemError.wrap(e);
                }

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

                Map<String,Object> m = Objects.newHashMap(
                        "attributes",Objects.newArrayList(
                        Objects.newHashMap("name","l","label","list","appearance","normal","type","List",
                                "element",Objects.newHashMap("type","Map","attributes",Objects.newArrayList(
                                Objects.newHashMap("name","a","label","aaa","appearance","normal","type","String","script",
                                        "if(parent.name=='0') attr.appearance='required';"),
                                Objects.newHashMap("name","b","label","bbb","appearance","normal","type","String","script",
                                        "var sz = s1.length(parent.parent.data)-1; if(parent.name==sz) attr.appearance='required';")
                        ))
                ))
                );
                ObjectSchema s = new ObjectSchema();
                try {
                    s.fromMap(m);
                } catch (ObjectSchemaFormatException e) {
                    throw S1SystemError.wrap(e);
                }

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


}
