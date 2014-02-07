package org.s1.script;

import org.mozilla.javascript.Node;
import org.mozilla.javascript.Token;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.ast.*;
import org.s1.objects.Objects;

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


    public static Object eval(Node node, Context ctx) {
        if(node instanceof AstRoot){
            try {
                Iterator<Node> it = node.iterator();
                while(it.hasNext()){
                    Node n = it.next();
                    eval(n, ctx);
                }
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
                eval(n,ctx1);
            }
        }else if(node instanceof DoLoop){
            //do while(...) ...;
            Context ctx1 = ctx.createChild();
            do{
                try{
                    eval(((DoLoop) node).getBody(), ctx);
                }catch (LoopContinueException e){

                }catch (LoopBreakException e){
                    break;
                }
            }while (condition(((DoLoop) node).getCondition(),ctx1));
        }else if(node instanceof WhileLoop){
            //while(...){...}
            Context ctx1 = ctx.createChild();
            while(condition(((WhileLoop) node).getCondition(),ctx1)){
                try{
                    eval(((WhileLoop) node).getBody(), ctx1);
                }catch (LoopContinueException e){

                }catch (LoopBreakException e){
                    break;
                }
            }
        }else if(node instanceof ForInLoop){
            //for(x in y){...}
            Context ctx1 = ctx.createChild();
            Object o = get(((ForInLoop) node).getIteratedObject(),ctx);
            if(o instanceof Map){
                for(String s:((Map<String,Object>) o).keySet()){
                    AstNode it = ((ForInLoop) node).getIterator();
                    if(it instanceof VariableDeclaration){
                        String name = ((Name)((VariableDeclaration) it).getVariables().get(0).getTarget()).getIdentifier();
                        ctx1.getVariables().put(name, s);
                    }
                    try{
                        eval(((ForInLoop) node).getBody(),ctx1);
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
                        eval(((ForInLoop) node).getBody(),ctx1);
                    }catch (LoopContinueException e){

                    }catch (LoopBreakException e){
                        break;
                    }
                }
            }
        }else if(node instanceof ForLoop){
            //for(...;...;...){...}
            Context ctx1 = ctx.createChild();
            for(eval(((ForLoop) node).getInitializer(),ctx1);
                condition(((ForLoop) node).getCondition(),ctx1);
                eval(((ForLoop) node).getIncrement(),ctx1)){
                try{
                    eval(((ForLoop) node).getBody(), ctx1);
                }catch (LoopContinueException e){

                }catch (LoopBreakException e){
                    break;
                }
            }
        }else if(node instanceof NewExpression){
            //new ...;

        }else if(node instanceof IfStatement){
            //if(...){...}
            boolean c = condition(((IfStatement) node).getCondition(),ctx);
            if(c){
                eval(((IfStatement) node).getThenPart(), ctx.createChild());
            }else{
                if(((IfStatement) node).getElsePart()!=null){
                    eval(((IfStatement) node).getElsePart(),ctx.createChild());
                }
            }
        }else if(node instanceof SwitchStatement){
            //switch(...)
            Object o = get(((SwitchStatement) node).getExpression(), ctx);
            for(SwitchCase sc:((SwitchStatement) node).getCases()){
                boolean b = false;
                if(sc.isDefault()){
                    b = true;
                }else{
                    Object o2 = get(sc.getExpression(),ctx);
                    if(Objects.equals(o,o2)){
                        b = true;
                    }
                }
                if(b){
                    try{
                        for(AstNode s: sc.getStatements()){
                            eval(s, ctx);
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
                ctx.getVariables().put(name,get(vi.getInitializer(), ctx));
            }
        }else if(node instanceof TryStatement){
            //try{...}catch(..){...}finally{...}
            try{
                Context ctx1 = ctx.createChild();
                eval(((TryStatement) node).getTryBlock(),ctx1);
            }catch (JavaScriptException e){
                for(CatchClause cc:((TryStatement) node).getCatchClauses()){
                    Context ctx1 = ctx.createChild();
                    ctx1.getVariables().put(cc.getVarName().getIdentifier(),e.getData());
                    if(cc.getCatchCondition()==null || condition(cc.getCatchCondition(), ctx1)){
                        eval(cc.getBody(),ctx1);
                    }
                }
            }finally {
                eval(((TryStatement) node).getFinallyBlock(),ctx);
            }
        }else if(node instanceof ThrowStatement){
            //throw ...;
            throw new JavaScriptException(get(((ThrowStatement) node).getExpression(),ctx));
        }else if(node instanceof ReturnStatement){
            //return ...;
            throw new FunctionReturnException(get(((ReturnStatement) node).getReturnValue(),ctx));
        }else if(node instanceof BreakStatement){
            //break;
            throw new LoopBreakException();
        }else if(node instanceof ContinueStatement){
            //continue;
            throw new LoopContinueException();
        }else if(node instanceof Scope){
            //{...}
            Context ctx1 = ctx.createChild();
            for(AstNode n:((Scope) node).getStatements()){
                eval(n,ctx1);
            }
        }else{
            get((AstNode)node,ctx);
        }
        return null;
    }

    protected static boolean condition(AstNode node, Context ctx){
        Object o = get(node,ctx);
        if(o instanceof Boolean)
            return ((Boolean) o).booleanValue();
        else
            return !Objects.isNullOrEmpty(o);
    }

    protected static Object get(AstNode node, Context ctx){
        //System.out.println(node.getClass().getName());
        if(node instanceof Name){
            return ctx.get(((Name) node).getIdentifier());
        }else if(node instanceof PropertyGet){
            Object obj = get(((PropertyGet) node).getTarget(),ctx);
            if(!(obj instanceof Map)){
                throwScriptError("Object is not instance of Map",node);
            }
            return ((Map) obj).get(((PropertyGet) node).getProperty().getIdentifier());
        }else if(node instanceof ElementGet){
            Object obj = get(((ElementGet) node).getTarget(),ctx);

            Object o = get(((ElementGet) node).getElement(), ctx);
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
            return ((StringLiteral) node).getValue();
        }else if(node instanceof NumberLiteral){
            return ((NumberLiteral) node).getNumber();
        }else if(node instanceof ObjectLiteral){
            Map<String,Object> m = Objects.newHashMap();
            for(ObjectProperty op:((ObjectLiteral) node).getElements()){
                if(op.getLeft() instanceof Name){
                    m.put(op.getLeft().getString(),get(op.getRight(), ctx));
                }else if(op.getLeft() instanceof StringLiteral){
                    m.put(((StringLiteral) op.getLeft()).getValue(),get(op.getRight(), ctx));
                }
            }
            return m;
        }else if(node instanceof ArrayLiteral){
            List<Object> l = Objects.newArrayList();
            for(AstNode n:((ArrayLiteral) node).getElements()){
                l.add(get(n, ctx));
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
                        eval(fnode.getBody(), getContext());
                    }catch (FunctionReturnException e){
                        return e.getData();
                    }
                    return null;
                }
            };
            if(!Objects.isNullOrEmpty(fn)){
                ctx.getVariables().put(fn,sf);
            }
            //System.out.println("-------"+fn);
            //ctx.getVariables().put(fn,node);
            //AstNode n = ((FunctionNode) node).getBody();
            //((FunctionNode) node).getParams();
            return sf;
        }else if(node instanceof FunctionCall){
            //a()
            List<Object> params = Objects.newArrayList();
            List<AstNode> pnodes = ((FunctionCall) node).getArguments();
            for(AstNode pn:pnodes){
                params.add(get(pn, ctx));
            }
            Object f = get(((FunctionCall) node).getTarget(),ctx);
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
            Object o = get(operand,ctx);
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
                set(operand,ctx, UNDEFINED);
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
                        set(operand,ctx,d++);
                        return d;
                    }else{
                        set(operand,ctx,d+1);
                        return d;
                    }
                }
            }else if(operator==Token.DEC){
                if(o instanceof Number){
                    double d = ((Number)o).doubleValue();
                    if(prefix){
                        set(operand,ctx,d--);
                        return d;
                    }else{
                        set(operand,ctx,d-1);
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
            if(condition(((ConditionalExpression) node).getTestExpression(), ctx)){
                return get(((ConditionalExpression) node).getTrueExpression(),ctx);
            }else{
                return get(((ConditionalExpression) node).getFalseExpression(),ctx);
            }
        }else if(node instanceof ExpressionStatement){
            //expression
            AstNode exp = ((ExpressionStatement) node).getExpression();
            return get(exp,ctx);
        }else if(node instanceof ParenthesizedExpression){
            AstNode exp = ((ParenthesizedExpression) node).getExpression();
            return get(exp,ctx);
        }else if(node instanceof InfixExpression){
            //a>b
            int operator = ((InfixExpression) node).getOperator();
            int operatorPos = ((InfixExpression) node).getOperatorPosition();
            Object l = get(((InfixExpression) node).getLeft(),ctx);
            Object r = get(((InfixExpression) node).getRight(),ctx);
            if(operator==Token.ASSIGN){
                set(((InfixExpression) node).getLeft(),ctx,get(((InfixExpression) node).getRight(),ctx));
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
                set(((InfixExpression) node).getLeft(),ctx,o);
                return o;
            }else if(operator==Token.ASSIGN_SUB){
                Object o = null;
                if(l instanceof Number && r instanceof Number)
                    o = ((Number)l).doubleValue() - ((Number)r).doubleValue();
                set(((InfixExpression) node).getLeft(),ctx,o);
                return o;
            }else if(operator==Token.ASSIGN_MUL){
                Object o = null;
                if(l instanceof Number && r instanceof Number)
                    o = ((Number)l).doubleValue() * ((Number)r).doubleValue();
                set(((InfixExpression) node).getLeft(),ctx,o);
                return o;
            }else if(operator==Token.ASSIGN_DIV){
                Object o = null;
                if(l instanceof Number && r instanceof Number)
                    o = ((Number)l).doubleValue() / ((Number)r).doubleValue();
                set(((InfixExpression) node).getLeft(),ctx,o);
                return o;
            }else if(operator==Token.ASSIGN_MOD){
                Object o = null;
                if(l instanceof Number && r instanceof Number)
                    o = ((Number)l).doubleValue() % ((Number)r).doubleValue();
                set(((InfixExpression) node).getLeft(),ctx,o);
                return o;
            }
        }
        return null;
    }

    public static final Object UNDEFINED = new Object();

    protected static void set(AstNode node, Context ctx, Object val){
        if(node instanceof Name){
            if(val==UNDEFINED){
                ctx.remove(((Name) node).getIdentifier());
            }else{
                ctx.set(((Name) node).getIdentifier(), val);
            }
        }else if(node instanceof PropertyGet){
            Object obj = get(((PropertyGet) node).getTarget(),ctx);
            if(!(obj instanceof Map)){
                throwScriptError("Object is not instance of Map",node);
            }
            if(val==UNDEFINED){
                ((Map) obj).remove(((PropertyGet) node).getProperty().getIdentifier());
            }else{
                ((Map) obj).put(((PropertyGet) node).getProperty().getIdentifier(), val);
            }
        }else if(node instanceof ElementGet){
            Object obj = get(((ElementGet) node).getTarget(),ctx);

            Object o = get(((ElementGet) node).getElement(), ctx);
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
                }
            }
        }
    }

    protected static void throwScriptError(String message, AstNode node){
        throw new JavaScriptException(message+": line "+node.getLineno()+", column "+node.getPosition());
    }

}
