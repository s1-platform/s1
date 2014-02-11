package org.s1.script;

/**
 * Script limit exception
 */
public class ScriptLimitException extends RuntimeException {

    public static enum Limits{
        MEMORY, SIZE, TIME
    }

    private long limit;
    private Limits type;

    public ScriptLimitException(Limits type, long limit) {
        this.type = type;
        this.limit = limit;
    }

    public long getLimit() {
        return limit;
    }

    public Limits getType() {
        return type;
    }
}