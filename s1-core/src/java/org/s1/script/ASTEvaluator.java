/*
 * Copyright 2014 Grigory Pykhov
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.s1.script;

import org.mozilla.javascript.Node;
import org.mozilla.javascript.Token;
import org.mozilla.javascript.ast.*;
import org.s1.objects.Objects;
import org.s1.script.errors.FunctionReturnException;
import org.s1.script.errors.LoopBreakException;
import org.s1.script.errors.LoopContinueException;
import org.s1.script.errors.ScriptException;
import org.s1.script.function.ScriptFunction;
import org.s1.script.function.ScriptFunctionSet;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Abstract syntax tree evaluator
 */
public class ASTEvaluator {

    private String source;

    public ASTEvaluator(String source) {
        this.source = source;
    }

    /**
     * Eval container
     *
     * @param node
     * @param ctx
     * @return
     */
    public Object eval(Node node, Context ctx) {
        Object evalResult = null;
        if(node instanceof AstRoot){
            Object ret = null;
            Iterator<Node> it = node.iterator();
            while(it.hasNext()){
                Node n = it.next();
                ret = eval(n, ctx);
            }
            evalResult = ret;
        }else if(node instanceof Block){
            //{...}
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
            }catch (ScriptException e){
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
            throw new ScriptException(get(((ThrowStatement) node).getExpression(),ctx));
        }else if(node instanceof ReturnStatement){
            //return ...;
            throw new FunctionReturnException(get(((ReturnStatement) node).getReturnValue(),ctx));
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
                eval(n,ctx1);
            }
        }else{
            evalResult = get((AstNode)node,ctx);
        }
        return evalResult;
    }

    /**
     *
     * @param node
     * @param ctx
     * @return
     */
    protected boolean condition(AstNode node, Context ctx){
        boolean result = false;
        Object o = get(node,ctx);
        if(o instanceof Boolean)
            result = ((Boolean) o).booleanValue();
        else
            result = !Objects.isNullOrEmpty(o);
        return result;
    }

    /**
     *
     * @param node
     * @param ctx
     * @return
     */
    protected Object get(AstNode node, Context ctx){
        Object getResult = null;
        if(node instanceof Name){
            getResult = ctx.get(((Name) node).getIdentifier());
        }else if(node instanceof PropertyGet){
            Object obj = get(((PropertyGet) node).getTarget(),ctx);
            if(obj instanceof ScriptFunctionSet){
                getResult = ((ScriptFunctionSet) obj).getFunction(((PropertyGet) node).getProperty().getIdentifier());
            }else if(obj instanceof Map){
                getResult = ((Map) obj).get(((PropertyGet) node).getProperty().getIdentifier());
            }else{
                throwScriptError("Object is not instance of Map",node);
            }
        }else if(node instanceof ElementGet){
            Object obj = get(((ElementGet) node).getTarget(),ctx);

            Object o = get(((ElementGet) node).getElement(), ctx);
            if(o instanceof Number){
                if(!(obj instanceof List)){
                    throwScriptError("Object is not instance of List",node);
                }
                if(((List) obj).size()>((Number) o).intValue())
                    getResult = ((List) obj).get(((Number) o).intValue());
            }else{
                if(obj instanceof ScriptFunctionSet){
                    getResult = ((ScriptFunctionSet) obj).getFunction(""+o);
                }else if(obj instanceof Map){
                    getResult = ((Map) obj).get(o);
                }else{
                    throwScriptError("Object is not instance of Map",node);
                }
                if(!(obj instanceof Map)){
                    throwScriptError("Object is not instance of Map",node);
                }
            }
        }else if(node instanceof KeywordLiteral){
            if(((KeywordLiteral) node).isBooleanLiteral()){
                getResult = node.getType() == Token.TRUE;
            }else if(node.getType()==Token.NULL)
                getResult = null;
        }else if(node instanceof StringLiteral){
            String s = ((StringLiteral) node).getValue();
            getResult = s;
        }else if(node instanceof NumberLiteral){
            Double d = ((NumberLiteral) node).getNumber();
            getResult = d;
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
                    m.put(k,get(op.getRight(), ctx));
                }

            }
            getResult = m;
        }else if(node instanceof ArrayLiteral){
            List<Object> l = Objects.newArrayList();
            for(AstNode n:((ArrayLiteral) node).getElements()){
                l.add(get(n, ctx));
            }
            getResult = l;
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
                public Object call() throws ScriptException {
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

            getResult = sf;
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
                getResult = sf.call();
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
                    getResult = null;
                else if(o instanceof Map)
                    getResult = "Map";
                else if(o instanceof List)
                    getResult = "List";
                else if(o instanceof ScriptFunction)
                    getResult = "Function";
                else
                    getResult = o.getClass().getSimpleName();
            }else if(operator==Token.DELPROP){
                set(operand,ctx, UNDEFINED);
            }else if(operator==Token.NOT){
                if(o instanceof Boolean){
                    getResult = !((Boolean) o).booleanValue();
                }else{
                    getResult = Objects.isNullOrEmpty(o);
                }
            }else if(operator==Token.INC){
                if(o instanceof Number){
                    double d = ((Number)o).doubleValue();
                    if(prefix){
                        set(operand,ctx,d++);
                        getResult = d;
                    }else{
                        set(operand,ctx,d+1);
                        getResult = d;
                    }
                }
            }else if(operator==Token.DEC){
                if(o instanceof Number){
                    double d = ((Number)o).doubleValue();
                    if(prefix){
                        set(operand,ctx,d--);
                        getResult = d;
                    }else{
                        set(operand,ctx,d-1);
                        getResult = d;
                    }
                }
            }else if(operator==Token.NEG){
                if(o instanceof Number){
                    double d = ((Number)o).doubleValue();
                    if(prefix){
                        getResult = -d;
                    }else{
                        getResult = d;
                    }
                }
            }
        }else if(node instanceof ConditionalExpression){
            //ternary operator
            if(condition(((ConditionalExpression) node).getTestExpression(), ctx)){
                getResult = get(((ConditionalExpression) node).getTrueExpression(),ctx);
            }else{
                getResult = get(((ConditionalExpression) node).getFalseExpression(),ctx);
            }
        }else if(node instanceof ExpressionStatement){
            //expression
            AstNode exp = ((ExpressionStatement) node).getExpression();
            getResult = get(exp,ctx);
        }else if(node instanceof ParenthesizedExpression){
            AstNode exp = ((ParenthesizedExpression) node).getExpression();
            getResult = get(exp,ctx);
        }else if(node instanceof InfixExpression){
            //a>b
            int operator = ((InfixExpression) node).getOperator();
            int operatorPos = ((InfixExpression) node).getOperatorPosition();
            Object l = get(((InfixExpression) node).getLeft(),ctx);
            Object r = get(((InfixExpression) node).getRight(),ctx);
            if(operator==Token.ASSIGN){
                set(((InfixExpression) node).getLeft(),ctx,r);
                getResult = r;
            }else if(operator==Token.OR){
                if(!(l instanceof Boolean))
                    l = !Objects.isNullOrEmpty(l);
                if(!(r instanceof Boolean))
                    r = !Objects.isNullOrEmpty(r);
                getResult = (Boolean)l||(Boolean)r;
            }else if(operator==Token.AND){
                if(!(l instanceof Boolean))
                    l = !Objects.isNullOrEmpty(l);
                if(!(r instanceof Boolean))
                    r = !Objects.isNullOrEmpty(r);
                getResult = (Boolean)l&&(Boolean)r;
            }else if(operator==Token.LT){
                if(l instanceof Number && r instanceof Number){
                    getResult = ((Number)l).doubleValue() < ((Number)r).doubleValue();
                }else if(l instanceof String && r instanceof String){
                    getResult = ((String) l).compareTo((String)r)<0;
                }
            }else if(operator==Token.LE){
                if(l instanceof Number && r instanceof Number){
                    getResult = ((Number)l).doubleValue() <= ((Number)r).doubleValue();
                }else if(l instanceof String && r instanceof String){
                    getResult = ((String) l).compareTo((String)r)<=0;
                }
            }else if(operator==Token.GT){
                if(l instanceof Number && r instanceof Number){
                    getResult = ((Number)l).doubleValue() > ((Number)r).doubleValue();
                }else if(l instanceof String && r instanceof String){
                    getResult = ((String) l).compareTo((String)r)>0;
                }
            }else if(operator==Token.GE){
                if(l instanceof Number && r instanceof Number){
                    getResult = ((Number)l).doubleValue() >= ((Number)r).doubleValue();
                }else if(l instanceof String && r instanceof String){
                    getResult = ((String) l).compareTo((String)r)>=0;
                }
            }else if(operator==Token.ADD){
                if(l instanceof Number && r instanceof Number)
                    getResult = ((Number)l).doubleValue() + ((Number)r).doubleValue();
                else
                    getResult = Objects.cast(l,String.class)+Objects.cast(r,String.class);
            }else if(operator==Token.SUB){
                double l_d = Objects.cast(l,Double.class);
                double r_d = Objects.cast(r,Double.class);
                getResult = l_d-r_d;
                //if(l instanceof Number && r instanceof Number)
                //    getResult = ((Number)l).doubleValue() - ((Number)r).doubleValue();
            }else if(operator==Token.MOD){
                double l_d = Objects.cast(l,Double.class);
                double r_d = Objects.cast(r,Double.class);
                getResult = l_d%r_d;
                //if(l instanceof Number && r instanceof Number)
                //    getResult = ((Number)l).doubleValue() % ((Number)r).doubleValue();
            }else if(operator==Token.MUL){
                double l_d = Objects.cast(l,Double.class);
                double r_d = Objects.cast(r,Double.class);
                getResult = l_d*r_d;
                //if(l instanceof Number && r instanceof Number)
                    //getResult = ((Number)l).doubleValue() * ((Number)r).doubleValue();
            }else if(operator==Token.DIV){
                double l_d = Objects.cast(l,Double.class);
                double r_d = Objects.cast(r,Double.class);
                getResult = l_d/r_d;
                //if(l instanceof Number && r instanceof Number)
                //    getResult = ((Number)l).doubleValue() / ((Number)r).doubleValue();
            }else if(operator==Token.EQ){
                getResult = Objects.equals(l,r);
            }else if(operator==Token.NE){
                getResult = !Objects.equals(l,r);
            }else if(operator==Token.ASSIGN_ADD){
                Object o = null;
                if(l instanceof Number && r instanceof Number)
                    o = ((Number)l).doubleValue() + ((Number)r).doubleValue();
                else
                    o = Objects.cast(l,String.class)+Objects.cast(r,String.class);
                set(((InfixExpression) node).getLeft(),ctx,o);
                getResult = o;
            }else if(operator==Token.ASSIGN_SUB){
                Object o = null;
                if(l instanceof Number && r instanceof Number)
                    o = ((Number)l).doubleValue() - ((Number)r).doubleValue();
                set(((InfixExpression) node).getLeft(),ctx,o);
                getResult = o;
            }else if(operator==Token.ASSIGN_MUL){
                Object o = null;
                if(l instanceof Number && r instanceof Number)
                    o = ((Number)l).doubleValue() * ((Number)r).doubleValue();
                set(((InfixExpression) node).getLeft(),ctx,o);
                getResult = o;
            }else if(operator==Token.ASSIGN_DIV){
                Object o = null;
                if(l instanceof Number && r instanceof Number)
                    o = ((Number)l).doubleValue() / ((Number)r).doubleValue();
                set(((InfixExpression) node).getLeft(),ctx,o);
                getResult = o;
            }else if(operator==Token.ASSIGN_MOD){
                Object o = null;
                if(l instanceof Number && r instanceof Number)
                    o = ((Number)l).doubleValue() % ((Number)r).doubleValue();
                set(((InfixExpression) node).getLeft(),ctx,o);
                getResult = o;
            }
        }
        ctx.getMemoryHeap().take(getResult);
        return getResult;
    }

    /**
     *
     */
    public static final Object UNDEFINED = new Object();

    /**
     *
     * @param node
     * @param ctx
     * @param val
     */
    protected void set(AstNode node, Context ctx, Object val){
        MemoryHeap mh = ctx.getMemoryHeap();
        //mh.take(val);
        if(node instanceof Name){
            String k = ((Name) node).getIdentifier();
            if(val==UNDEFINED){
                ctx.remove(k);
            }else{
                ctx.set(k, val);
                mh.take(k);
            }
        }else if(node instanceof PropertyGet){
            Object obj = get(((PropertyGet) node).getTarget(),ctx);
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

    /**
     *
     * @param message
     * @param node
     */
    protected void throwScriptError(String message, AstNode node){
        String m = getErrorMessage(source, node.getLineno() - 1, node.getPosition());
        throw new ScriptException(message+": "+m);
    }

    protected static String getErrorMessage(String source, int lineno, int position){
        String arr [] = source.split("\n");
        String line = lineno<arr.length?arr[lineno]:"";
        int s = position-20;
        int e = position+20;
        s = s<0?0:s;
        e = e<line.length()?e:line.length()-1;
        return "line "+(lineno+1)+", column "+position+": ... "+line.substring(s,e)+" ...";
    }

}
