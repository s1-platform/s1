package org.s1.table;

import org.s1.objects.Objects;

import java.util.Map;

/**
 * Count group bean
 */
public class CountGroupBean {

    private long count;
    private Object value;
    private Object from;
    private Object to;
    private boolean other;

    /**
     *
     */
    public CountGroupBean() {
    }

    /**
     *
     * @return
     */
    public Map<String,Object> toMap(){
        Map<String,Object> m = Objects.newHashMap(
                "value", value,
                "other", other,
                "count", count,
                "from", from,
                "to", to
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
    public boolean isOther() {
        return other;
    }

    /**
     *
     * @param other
     */
    public void setOther(boolean other) {
        this.other = other;
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
     * @param <T>
     * @return
     */
    public <T> T getValue() {
        return (T) value;
    }

    /**
     *
     * @param value
     */
    public void setValue(Object value) {
        this.value = value;
    }

    /**
     *
     * @param <T>
     * @return
     */
    public <T> T getFrom() {
        return (T)from;
    }

    /**
     *
     * @param from
     */
    public void setFrom(Object from) {
        this.from = from;
    }

    /**
     *
     * @param <T>
     * @return
     */
    public <T> T getTo() {
        return (T)to;
    }

    /**
     *
     * @param to
     */
    public void setTo(Object to) {
        this.to = to;
    }
}
