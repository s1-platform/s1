package org.s1.objects;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.*;

/**
 * Type utils
 */
public class ObjectType {

    /**
     * Get class by type name
     * <p>Supported types are:</p>
     * <ul>
     * <li>String</li>
     * <li>Integer</li>
     * <li>Float</li>
     * <li>Long</li>
     * <li>Double</li>
     * <li>BigDecimal</li>
     * <li>BigInteger</li>
     * <li>Boolean</li>
     * <li>Date</li>
     * </ul>
     *
     * @param type
     * @return
     */
    static Class resolveType(String type) {
        Class cls = Object.class;
        if ("String".equals(type))
            cls = String.class;
        else if ("Integer".equals(type))
            cls = Integer.class;
        else if ("Long".equals(type))
            cls = Long.class;
        else if ("Float".equals(type))
            cls = Float.class;
        else if ("Double".equals(type))
            cls = Double.class;
        else if ("Boolean".equals(type))
            cls = Boolean.class;
        else if ("BigDecimal".equals(type))
            cls = BigDecimal.class;
        else if ("BigInteger".equals(type))
            cls = BigInteger.class;
        else if ("Date".equals(type))
            cls = Date.class;

        return cls;
    }

    /**
     * Cast object to type
     *
     * @param obj
     * @param type
     * @param <T>
     * @return
     */
    static <T> T cast(Object obj, Class<T> type) {
        if (type == String.class) {
            if (obj == null)
                obj = "";
            obj = obj.toString();
        } else if (type == BigInteger.class) {
            if (obj == null)
                obj = "0";
            obj = new BigInteger("" + obj);
        } else if (type == BigDecimal.class) {
            if (obj == null)
                obj = "0";
            obj = new BigDecimal("" + obj);
        } else if (type == Integer.class) {
            if (obj == null)
                obj = "0";
            obj = Integer.parseInt("" + obj);
        } else if (type == Long.class) {
            if (obj == null)
                obj = "0";
            obj = Long.parseLong("" + obj);
        } else if (type == Float.class) {
            if (obj == null)
                obj = "0";
            obj = Float.parseFloat("" + obj);
        } else if (type == Double.class) {
            if (obj == null)
                obj = "0";
            obj = Double.parseDouble("" + obj);
        } else if (type == Boolean.class) {
            if (obj == null)
                obj = "false";
            obj = Boolean.parseBoolean("" + obj);
        } else if (type == Date.class) {
            if (obj != null) {
                if (obj instanceof String) {
                    obj = new Date(Long.parseLong("" + obj));
                } else if (obj instanceof Number) {
                    obj = new Date((Long) obj);
                }
            }
        }

        return (T) obj;
    }

}
