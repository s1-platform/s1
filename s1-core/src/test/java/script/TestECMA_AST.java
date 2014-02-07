package script;

import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Node;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.ast.*;
import org.s1.objects.Objects;
import org.s1.script.ASTEvaluator;
import org.s1.script.Context;
import org.s1.script.JavaScriptException;
import org.s1.script.ScriptFunction;
import org.s1.test.BasicTest;

import java.util.Iterator;

/**
 * s1v2
 * User: GPykhov
 * Date: 31.01.14
 * Time: 18:19
 */
public class TestECMA_AST extends BasicTest{

    public void test2(){
        CompilerEnvirons ce = new CompilerEnvirons();
        ce.setRecordingComments(false);
        ce.setStrictMode(true);
        ce.setXmlAvailable(false);

        AstRoot ar = new Parser(ce).parse("var a = true||false","qwer",1);
        trace(ar.debugPrint());
        Context c=new Context();

        ASTEvaluator.eval(ar,c);
        trace(c.getVariables());
    }
}
