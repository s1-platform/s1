package script;

import org.s1.objects.MapMethod;
import org.s1.script.function.ScriptFunctionSet;

/**
 * s1v2
 * User: GPykhov
 * Date: 11.02.14
 * Time: 19:05
 */
public class TestScriptFunctions extends ScriptFunctionSet {

    @MapMethod
    public int sum(Integer a, Integer b){
        return a+b;
    }

}
