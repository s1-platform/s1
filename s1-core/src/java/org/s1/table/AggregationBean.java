package org.s1.table;

import org.s1.objects.Objects;

import java.util.Map;

/**
 * Aggregation bean
 */
public class AggregationBean {

    private Object min;
    private Object max;
    private long count;
    private Object avg;
    private Object sum;

    /**
     *
     */
    public AggregationBean() {
    }

    /**
     *
     * @return
     */
    public Map<String,Object> toMap(){
        Map<String,Object> m = Objects.newHashMap(
                "min",min,
                "max",max,
                "count",count,
                "avg",avg,
                "sum",sum
        );
        return m;
    }

    /**
     *
     * @return
     */
    public String toString(){
        return toMap().toString();
    }

    /**
     *
     * @return
     */
    public <T> T getMin() {
        return (T)min;
    }

    /**
     *
     * @param min
     */
    public void setMin(Object min) {
        this.min = min;
    }

    /**
     *
     * @return
     */
    public <T> T getMax() {
        return (T)max;
    }

    /**
     *
     * @param max
     */
    public void setMax(Object max) {
        this.max = max;
    }

    /**
     *
     * @return
     */
    public long getCount() {
        return count;
    }

    /**
     *
     * @param count
     */
    public void setCount(long count) {
        this.count = count;
    }

    /**
     *
     * @return
     */
    public <T> T getAvg() {
        return (T)avg;
    }

    /**
     *
     * @param avg
     */
    public void setAvg(Object avg) {
        this.avg = avg;
    }

    /**
     *
     * @return
     */
    public <T> T getSum() {
        return (T)sum;
    }

    /**
     *
     * @param sum
     */
    public void setSum(Object sum) {
        this.sum = sum;
    }
}
