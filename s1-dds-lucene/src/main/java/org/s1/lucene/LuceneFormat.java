package org.s1.lucene;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.util.BytesRef;
import org.s1.S1SystemError;
import org.s1.misc.Closure;
import org.s1.misc.ClosureException;
import org.s1.objects.ObjectIterator;
import org.s1.objects.Objects;
import org.s1.table.format.*;
import org.s1.table.format.Query;
import org.s1.table.format.Sort;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Lucene Format
 */
public class LuceneFormat {

    /**
     *
     * @param query
     * @return
     */
    public static org.apache.lucene.search.Query formatSearch(Query query){
        BooleanQuery q = new BooleanQuery();
        q.add(new MatchAllDocsQuery(), BooleanClause.Occur.SHOULD);
        if(query!=null){
            org.apache.lucene.search.Query _q = formatQueryNode(query.getNode());
            if(_q!=null)
                q.add(_q, BooleanClause.Occur.MUST);
        }
        return q;
    }

    /**
     *
     * @param qn
     * @return
     */
    public static org.apache.lucene.search.Query formatQueryNode(QueryNode qn){
        org.apache.lucene.search.Query o = null;
        if(qn instanceof GroupQueryNode){
            BooleanQuery bq = new BooleanQuery();
            BooleanClause.Occur oc = ((GroupQueryNode) qn).getOperation()== GroupQueryNode.GroupOperation.OR? BooleanClause.Occur.SHOULD:BooleanClause.Occur.MUST;
            for(QueryNode n:((GroupQueryNode) qn).getChildren()){
                org.apache.lucene.search.Query _q = formatQueryNode(n);
                if(_q!=null)
                    bq.add(_q, oc);
            }
            if(bq.getClauses().length>0)
                o = bq;
        }else if(qn instanceof FieldQueryNode){
            FieldQueryNode.FieldOperation op = ((FieldQueryNode) qn).getOperation();
            String f = ((FieldQueryNode) qn).getField();
            Object val = ((FieldQueryNode) qn).getValue();
            if(op == FieldQueryNode.FieldOperation.EQUALS){
                if(val instanceof Integer || val instanceof Long || val instanceof BigInteger){
                    o = NumericRangeQuery.newLongRange(f, Objects.cast(val, Long.class),
                            Objects.cast(val, Long.class), true, true);
                }else if(val instanceof Float || val instanceof Double || val instanceof BigDecimal){
                    o = NumericRangeQuery.newDoubleRange(f, Objects.cast(val, Double.class),
                            Objects.cast(val, Double.class), true, true);
                }else if(val instanceof Date){
                    o = NumericRangeQuery.newLongRange(f, ((Date) val).getTime(),
                            ((Date) val).getTime(), true, true);
                }else{
                    o = new TermQuery(new Term(f,new BytesRef(""+val)));
                }
            }else if(op == FieldQueryNode.FieldOperation.CONTAINS){
                o = new TermQuery(new Term(f,new BytesRef(""+val)));
            }else if(op == FieldQueryNode.FieldOperation.NULL){
                //o = new BooleanQuery();
                //((BooleanQuery) o).add(new TermRangeQuery(f, new BytesRef("*"), new BytesRef("*"), true, true), BooleanClause.Occur.MUST_NOT);
            }else if(op == FieldQueryNode.FieldOperation.GT){
                if(val instanceof Integer || val instanceof Long || val instanceof BigInteger){
                    o = NumericRangeQuery.newLongRange(f, Objects.cast(val, Long.class),
                            Long.MAX_VALUE, false, false);
                }else if(val instanceof Float || val instanceof Double || val instanceof BigDecimal){
                    o = NumericRangeQuery.newDoubleRange(f, Objects.cast(val, Double.class),
                            Double.MAX_VALUE, false, false);
                }else if(val instanceof Date){
                    o = NumericRangeQuery.newLongRange(f, ((Date) val).getTime(),
                            Long.MAX_VALUE, false, false);
                }
            }else if(op == FieldQueryNode.FieldOperation.GTE){
                if(val instanceof Integer || val instanceof Long || val instanceof BigInteger){
                    o = NumericRangeQuery.newLongRange(f, Objects.cast(val,Long.class),
                            Long.MAX_VALUE,true,false);
                }else if(val instanceof Float || val instanceof Double || val instanceof BigDecimal){
                    o = NumericRangeQuery.newDoubleRange(f, Objects.cast(val,Double.class),
                            Double.MAX_VALUE,true,false);
                }else if(val instanceof Date){
                    o = NumericRangeQuery.newLongRange(f, ((Date) val).getTime(),
                            Long.MAX_VALUE, true, false);
                }
            }else if(op == FieldQueryNode.FieldOperation.LT){
                if(val instanceof Integer || val instanceof Long || val instanceof BigInteger){
                    o = NumericRangeQuery.newLongRange(f, Long.MIN_VALUE, Objects.cast(val,Long.class),
                            false,false);
                }else if(val instanceof Float || val instanceof Double || val instanceof BigDecimal){
                    o = NumericRangeQuery.newDoubleRange(f, Double.MIN_VALUE, Objects.cast(val,Double.class),
                            false,false);
                }else if(val instanceof Date){
                    o = NumericRangeQuery.newLongRange(f, Long.MIN_VALUE, ((Date) val).getTime(),
                            false, false);
                }
            }else if(op == FieldQueryNode.FieldOperation.LTE){
                if(val instanceof Integer || val instanceof Long || val instanceof BigInteger){
                    o = NumericRangeQuery.newLongRange(f, Long.MIN_VALUE, Objects.cast(val,Long.class),
                            false,true);
                }else if(val instanceof Float || val instanceof Double || val instanceof BigDecimal){
                    o = NumericRangeQuery.newDoubleRange(f, Double.MIN_VALUE, Objects.cast(val,Double.class),
                            false,true);
                }else if(val instanceof Date){
                    o = NumericRangeQuery.newLongRange(f, Long.MIN_VALUE, ((Date) val).getTime(),
                            false, true);
                }
            }
        }
        if(qn.isNot()){
            BooleanQuery bq = new BooleanQuery();
            bq.add(o, BooleanClause.Occur.MUST_NOT);
            o = bq;
        }
        return o;
    }

}
