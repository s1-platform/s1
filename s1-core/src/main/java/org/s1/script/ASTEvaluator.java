package org.s1.script;

import org.mozilla.javascript.Node;
import org.mozilla.javascript.Token;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.ast.*;
import org.s1.objects.Objects;

import java.lang.instrument.Instrumentation;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * s1v2
 * User: GPykhov
 * Date: 01.02.14
 * Time: 10:37
 */
public class ASTEvaluator {


    public static Object eval(Node node, Context ctx, MemoryHeap mh) {
        if(node instanceof AstRoot){
            try {
                Object ret = null;
                Iterator<Node> it = node.iterator();
                while(it.hasNext()){
                    Node n = it.next();
                    ret = eval(n, ctx, mh);
                    System.out.println(n.getClass().getName());
                }
                return ret;
            } catch (FunctionReturnException e){
                return e.getData();
            }
        }else if(node instanceof Block){
            //{...}
            //System.out.println("123");
            Context ctx1 = ctx.createChild();
            Iterator<Node> it = node.iterator();
            while(it.hasNext()){
                Node n = it.next();
                eval(n,ctx1, mh);
            }
        }else if(node instanceof DoLoop){
            //do while(...) ...;
            Context ctx1 = ctx.createChild();
            do{
                try{
                    eval(((DoLoop) node).getBody(), ctx, mh);
                }catch (LoopContinueException e){

                }catch (LoopBreakException e){
                    break;
                }
            }while (condition(((DoLoop) node).getCondition(),ctx1, mh));
        }else if(node instanceof WhileLoop){
            //while(...){...}
            Context ctx1 = ctx.createChild();
            while(condition(((WhileLoop) node).getCondition(),ctx1, mh)){
                try{
                    eval(((WhileLoop) node).getBody(), ctx1, mh);
                }catch (LoopContinueException e){

                }catch (LoopBreakException e){
                    break;
                }
            }
        }else if(node instanceof ForInLoop){
            //for(x in y){...}
            Context ctx1 = ctx.createChild();
            Object o = get(((ForInLoop) node).getIteratedObject(),ctx, mh);
            if(o instanceof Map){
                for(String s:((Map<String,Object>) o).keySet()){
                    AstNode it = ((ForInLoop) node).getIterator();
                    if(it instanceof VariableDeclaration){
                        String name = ((Name)((VariableDeclaration) it).getVariables().get(0).getTarget()).getIdentifier();
                        ctx1.getVariables().put(name, s);
                    }
                    try{
                        eval(((ForInLoop) node).getBody(),ctx1, mh);
                    }catch (LoopContinueException e){

                    }catch (LoopBreakException e){
                        break;
                    }
                }
            }else if(o instanceof List){
                for(double i=0.0D;i<((List<Object>) o).size();i++){
                    AstNode it = ((ForInLoop) node).getIterator();
                    if(it instanceof VariableDeclaration){
                        String name = ((Name)((VariableDeclaration) it).getVariables().get(0).getTarget()).getIdentifier();
                        ctx1.getVariables().put(name,i);
                    }
                    try{
                        eval(((ForInLoop) node).getBody(),ctx1, mh);
                    }catch (LoopContinueException e){

                    }catch (LoopBreakException e){
                        break;
                    }
                }
            }
        }else if(node instanceof ForLoop){
            //for(...;...;...){...}
            Context ctx1 = ctx.createChild();
            for(eval(((ForLoop) node).getInitializer(),ctx1, mh);
                condition(((ForLoop) node).getCondition(),ctx1, mh);
                eval(((ForLoop) node).getIncrement(),ctx1, mh)){
                try{
                    eval(((ForLoop) node).getBody(), ctx1, mh);
                }catch (LoopContinueException e){

                }catch (LoopBreakException e){
                    break;
                }
            }
        }else if(node instanceof NewExpression){
            //new ...;

        }else if(node instanceof IfStatement){
            //if(...){...}
            boolean c = condition(((IfStatement) node).getCondition(),ctx, mh);
            if(c){
                eval(((IfStatement) node).getThenPart(), ctx.createChild(), mh);
            }else{
                if(((IfStatement) node).getElsePart()!=null){
                    eval(((IfStatement) node).getElsePart(),ctx.createChild(), mh);
                }
            }
        }else if(node instanceof SwitchStatement){
            //switch(...)
            Object o = get(((SwitchStatement) node).getExpression(), ctx, mh);
            for(SwitchCase sc:((SwitchStatement) node).getCases()){
                boolean b = false;
                if(sc.isDefault()){
                    b = true;
                }else{
                    Object o2 = get(sc.getExpression(),ctx, mh);
                    if(Objects.equals(o,o2)){
                        b = true;
                    }
                }
                if(b){
                    try{
                        for(AstNode s: sc.getStatements()){
                            eval(s, ctx, mh);
                        }
                    }catch (LoopContinueException e){
                    }catch (LoopBreakException e){
                        break;
                    }
                }
            }
        }else if(node instanceof WithStatement){
            //with(...){...}

        }else if(node instanceof VariableDeclaration){
            //var ...
            for(VariableInitializer vi:((VariableDeclaration) node).getVariables()){
                String name = ((Name)vi.getTarget()).getIdentifier();
                ctx.getVariables().put(name,get(vi.getInitializer(), ctx, mh));
            }
        }else if(node instanceof TryStatement){
            //try{...}catch(..){...}finally{...}
            try{
                Context ctx1 = ctx.createChild();
                eval(((TryStatement) node).getTryBlock(),ctx1, mh);
            }catch (JavaScriptException e){
                for(CatchClause cc:((TryStatement) node).getCatchClauses()){
                    Context ctx1 = ctx.createChild();
                    ctx1.getVariables().put(cc.getVarName().getIdentifier(),e.getData());
                    if(cc.getCatchCondition()==null || condition(cc.getCatchCondition(), ctx1, mh)){
                        eval(cc.getBody(),ctx1, mh);
                    }
                }
            }finally {
                eval(((TryStatement) node).getFinallyBlock(),ctx, mh);
            }
        }else if(node instanceof ThrowStatement){
            //throw ...;
            throw new JavaScriptException(get(((ThrowStatement) node).getExpression(),ctx, mh));
        }else if(node instanceof ReturnStatement){
            //return ...;
            throw new FunctionReturnException(get(((ReturnStatement) node).getReturnValue(),ctx, mh));
        }else if(node instanceof BreakStatement){
            //break;
            throw new LoopBreakException();
        }else if(node instanceof ContinueStatement){
            //continue;
            throw new LoopContinueException();
        }else if(node instanceof Scope && !(node instanceof FunctionNode)){
            //{...}
            Context ctx1 = ctx.createChild();
            for(AstNode n:((Scope) node).getStatements()){
                eval(n,ctx1, mh);
            }
        }else{
            return get((AstNode)node,ctx, mh);
        }
        return null;
    }

    protected static boolean condition(AstNode node, Context ctx, MemoryHeap mh){
        Object o = get(node,ctx, mh);
        if(o instanceof Boolean)
            return ((Boolean) o).booleanValue();
        else
            return !Objects.isNullOrEmpty(o);
    }

    protected static Object get(AstNode node, Context ctx, final MemoryHeap mh){
        //System.out.println(node.getClass().getName());
        if(node instanceof Name){
            return ctx.get(((Name) node).getIdentifier());
        }else if(node instanceof PropertyGet){
            Object obj = get(((PropertyGet) node).getTarget(),ctx, mh);
            if(!(obj instanceof Map)){
                throwScriptError("Object is not instance of Map",node);
            }
            return ((Map) obj).get(((PropertyGet) node).getProperty().getIdentifier());
        }else if(node instanceof ElementGet){
            Object obj = get(((ElementGet) node).getTarget(),ctx, mh);

            Object o = get(((ElementGet) node).getElement(), ctx, mh);
            if(o instanceof Number){
                if(!(obj instanceof List)){
                    throwScriptError("Object is not instance of List",node);
                }
                if(((List) obj).size()<=((Number) o).intValue())
                    return null;
                return ((List) obj).get(((Number) o).intValue());
            }else{
                if(!(obj instanceof Map)){
                    throwScriptError("Object is not instance of Map",node);
                }
                return ((Map) obj).get(o);
            }
        }else if(node instanceof KeywordLiteral){
            if(((KeywordLiteral) node).isBooleanLiteral()){
                return node.getType() == Token.TRUE;
            }else if(node.getType()==Token.NULL)
                return null;
        }else if(node instanceof StringLiteral){
            String s = ((StringLiteral) node).getValue();
            return s;
        }else if(node instanceof NumberLiteral){
            Double d = ((NumberLiteral) node).getNumber();
            return d;
        }else if(node instanceof ObjectLiteral){
            Map<String,Object> m = Objects.newHashMap();
            for(ObjectProperty op:((ObjectLiteral) node).getElements()){
                String k = null;
                if(op.getLeft() instanceof Name){
                    k = op.getLeft().getString();
                }else if(op.getLeft() instanceof StringLiteral){
                    k = ((StringLiteral) op.getLeft()).getValue();
                }
                if(k!=null){
                    m.put(k,get(op.getRight(), ctx, mh));
                }

            }
            return m;
        }else if(node instanceof ArrayLiteral){
            List<Object> l = Objects.newArrayList();
            for(AstNode n:((ArrayLiteral) node).getElements()){
                l.add(get(n, ctx, mh));
            }
            return l;
        }else if(node instanceof FunctionNode){
            //function(...){...}
            String fn = ((FunctionNode) node).getName();
            final FunctionNode fnode = (FunctionNode)node;
            List<String> fparams = Objects.newArrayList();
            for(AstNode n:fnode.getParams()){
                fparams.add(((Name)n).getIdentifier());
            }
            ScriptFunction sf = new ScriptFunction(ctx.createChild(),fparams){
                @Override
                public Object call() throws JavaScriptException {
                    try{
                        eval(fnode.getBody(), getContext(), mh);
                    }catch (FunctionReturnException e){
                        return e.getData();
                    }
                    return null;
                }
            };
            if(!Objects.isNullOrEmpty(fn)){
                ctx.getVariables().put(fn,sf);
            }
            System.out.println("-------"+fn);
            //ctx.getVariables().put(fn,node);
            //AstNode n = ((FunctionNode) node).getBody();
            //((FunctionNode) node).getParams();
            return sf;
        }else if(node instanceof FunctionCall){
            //a()
            List<Object> params = Objects.newArrayList();
            List<AstNode> pnodes = ((FunctionCall) node).getArguments();
            for(AstNode pn:pnodes){
                params.add(get(pn, ctx, mh));
            }
            Object f = get(((FunctionCall) node).getTarget(),ctx, mh);
            if(f instanceof ScriptFunction){
                ScriptFunction sf = (ScriptFunction)f;
                for(int i=0;i<sf.getParams().size();i++){
                    Object pval = i<params.size()?params.get(i):null;
                    sf.getContext().getVariables().put(sf.getParams().get(i),pval);
                }
                sf.getContext().getVariables().put("arguments",params);
                return sf.call();
            }else{
                throwScriptError("Object is not a ScriptFunction",node);
            }
        }else if(node instanceof UnaryExpression){
            //a++, delete a; typeof a
            boolean prefix = ((UnaryExpression) node).isPrefix();
            int operator = ((UnaryExpression) node).getOperator();
            AstNode operand = ((UnaryExpression) node).getOperand();
            Object o = get(operand,ctx, mh);
            if(operator==Token.TYPEOF){
                if(o==null)
                    return null;
                else if(o instanceof Map)
                    return "Map";
                else if(o instanceof List)
                    return "List";
                else if(o instanceof ScriptFunction)
                    return "Function";
                else
                    return o.getClass().getSimpleName();
            }else if(operator==Token.DELPROP){
                set(operand,ctx, UNDEFINED, mh);
                return null;
            }else if(operator==Token.NOT){
                if(o instanceof Boolean){
                    return !((Boolean) o).booleanValue();
                }
                return Objects.isNullOrEmpty(o);
            }else if(operator==Token.INC){
                if(o instanceof Number){
                    double d = ((Number)o).doubleValue();
                    if(prefix){
                        set(operand,ctx,d++, mh);
                        return d;
                    }else{
                        set(operand,ctx,d+1, mh);
                        return d;
                    }
                }
            }else if(operator==Token.DEC){
                if(o instanceof Number){
                    double d = ((Number)o).doubleValue();
                    if(prefix){
                        set(operand,ctx,d--, mh);
                        return d;
                    }else{
                        set(operand,ctx,d-1, mh);
                        return d;
                    }
                }
            }else if(operator==Token.NEG){
                if(o instanceof Number){
                    double d = ((Number)o).doubleValue();
                    if(prefix){
                        return -d;
                    }else{
                        return d;
                    }
                }
            }
        }else if(node instanceof ConditionalExpression){
            //ternary operator
            if(condition(((ConditionalExpression) node).getTestExpression(), ctx, mh)){
                return get(((ConditionalExpression) node).getTrueExpression(),ctx, mh);
            }else{
                return get(((ConditionalExpression) node).getFalseExpression(),ctx, mh);
            }
        }else if(node instanceof ExpressionStatement){
            //expression
            AstNode exp = ((ExpressionStatement) node).getExpression();
            return get(exp,ctx, mh);
        }else if(node instanceof ParenthesizedExpression){
            AstNode exp = ((ParenthesizedExpression) node).getExpression();
            return get(exp,ctx, mh);
        }else if(node instanceof InfixExpression){
            //a>b
            int operator = ((InfixExpression) node).getOperator();
            int operatorPos = ((InfixExpression) node).getOperatorPosition();
            Object l = get(((InfixExpression) node).getLeft(),ctx, mh);
            Object r = get(((InfixExpression) node).getRight(),ctx, mh);
            if(operator==Token.ASSIGN){
                set(((InfixExpression) node).getLeft(),ctx,get(((InfixExpression) node).getRight(),ctx, mh), mh);
                return r;
            }else if(operator==Token.OR){
                if(!(l instanceof Boolean))
                    l = !Objects.isNullOrEmpty(l);
                if(!(r instanceof Boolean))
                    r = !Objects.isNullOrEmpty(r);
                return (Boolean)l||(Boolean)r;
            }else if(operator==Token.AND){
                if(!(l instanceof Boolean))
                    l = !Objects.isNullOrEmpty(l);
                if(!(r instanceof Boolean))
                    r = !Objects.isNullOrEmpty(r);
                return (Boolean)l&&(Boolean)r;
            }else if(operator==Token.LT){
                if(l instanceof Number && r instanceof Number)
                    return ((Number)l).doubleValue() < ((Number)r).doubleValue();
            }else if(operator==Token.LE){
                if(l instanceof Number && r instanceof Number)
                    return ((Number)l).doubleValue() <= ((Number)r).doubleValue();
            }else if(operator==Token.GT){
                if(l instanceof Number && r instanceof Number)
                    return ((Number)l).doubleValue() > ((Number)r).doubleValue();
            }else if(operator==Token.GE){
                if(l instanceof Number && r instanceof Number)
                    return ((Number)l).doubleValue() >= ((Number)r).doubleValue();
            }else if(operator==Token.ADD){
                if(l instanceof Number && r instanceof Number)
                    return ((Number)l).doubleValue() + ((Number)r).doubleValue();
                else
                    return Objects.cast(l,String.class)+Objects.cast(r,String.class);
            }else if(operator==Token.SUB){
                if(l instanceof Number && r instanceof Number)
                    return ((Number)l).doubleValue() - ((Number)r).doubleValue();
            }else if(operator==Token.MOD){
                if(l instanceof Number && r instanceof Number)
                    return ((Number)l).doubleValue() % ((Number)r).doubleValue();
            }else if(operator==Token.MUL){
                System.out.println("---"+((InfixExpression) node).getLeft().getClass().getName());
                if(l instanceof Number && r instanceof Number)
                    return ((Number)l).doubleValue() * ((Number)r).doubleValue();
            }else if(operator==Token.DIV){
                if(l instanceof Number && r instanceof Number)
                    return ((Number)l).doubleValue() / ((Number)r).doubleValue();
            }else if(operator==Token.EQ){
                return Objects.equals(l,r);
            }else if(operator==Token.NE){
                return !Objects.equals(l,r);
            }else if(operator==Token.ASSIGN_ADD){
                Object o = null;
                if(l instanceof Number && r instanceof Number)
                    o = ((Number)l).doubleValue() + ((Number)r).doubleValue();
                else
                    o = Objects.cast(l,String.class)+Objects.cast(r,String.class);
                set(((InfixExpression) node).getLeft(),ctx,o, mh);
                return o;
            }else if(operator==Token.ASSIGN_SUB){
                Object o = null;
                if(l instanceof Number && r instanceof Number)
                    o = ((Number)l).doubleValue() - ((Number)r).doubleValue();
                set(((InfixExpression) node).getLeft(),ctx,o, mh);
                return o;
            }else if(operator==Token.ASSIGN_MUL){
                Object o = null;
                if(l instanceof Number && r instanceof Number)
                    o = ((Number)l).doubleValue() * ((Number)r).doubleValue();
                set(((InfixExpression) node).getLeft(),ctx,o, mh);
                return o;
            }else if(operator==Token.ASSIGN_DIV){
                Object o = null;
                if(l instanceof Number && r instanceof Number)
                    o = ((Number)l).doubleValue() / ((Number)r).doubleValue();
                set(((InfixExpression) node).getLeft(),ctx,o, mh);
                return o;
            }else if(operator==Token.ASSIGN_MOD){
                Object o = null;
                if(l instanceof Number && r instanceof Number)
                    o = ((Number)l).doubleValue() % ((Number)r).doubleValue();
                set(((InfixExpression) node).getLeft(),ctx,o, mh);
                return o;
            }
        }
        return null;
    }

    public static final Object UNDEFINED = new Object();

    protected static void set(AstNode node, Context ctx, Object val, MemoryHeap mh){
        mh.take(val);
        if(node instanceof Name){
            String k = ((Name) node).getIdentifier();
            if(val==UNDEFINED){
                ctx.remove(k);
            }else{
                ctx.set(k, val);
                mh.take(k);
            }
        }else if(node instanceof PropertyGet){
            Object obj = get(((PropertyGet) node).getTarget(),ctx, mh);
            if(!(obj instanceof Map)){
                throwScriptError("Object is not instance of Map",node);
            }
            String k = ((PropertyGet) node).getProperty().getIdentifier();
            if(val==UNDEFINED){
                ((Map) obj).remove(k);
            }else{
                ((Map) obj).put(k, val);
                mh.take(k);
            }
        }else if(node instanceof ElementGet){
            Object obj = get(((ElementGet) node).getTarget(),ctx, mh);

            Object o = get(((ElementGet) node).getElement(), ctx, mh);
            if(o instanceof Number){
                if(!(obj instanceof List)){
                    throwScriptError("Object is not instance of List",node);
                }
                if(val==UNDEFINED)
                    val = null;
                List l = ((List) obj);
                int i = ((Number) o).intValue();
                if(l.size()<=i){
                    for(int j=l.size();j<=i;j++){
                        l.add(null);
                        mh.take(""+j);
                    }
                }
                l.set(i,val);
            }else{
                if(!(obj instanceof Map)){
                    throwScriptError("Object is not instance of Map",node);
                }
                if(val==UNDEFINED){
                    ((Map) obj).remove(o);
                }else{
                    ((Map) obj).put(o,val);
                    mh.take(("" + o));
                }
            }
        }
    }

    protected static void throwScriptError(String message, AstNode node){
        throw new JavaScriptException(message+": line "+node.getLineno()+", column "+node.getPosition());
    }

}
