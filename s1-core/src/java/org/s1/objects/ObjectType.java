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

package org.s1.objects;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.Map;

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
            if (obj == null || (""+obj).isEmpty())
                obj = "0";
            obj = new BigInteger("" + obj);
        } else if (type == BigDecimal.class) {
            if (obj == null || (""+obj).isEmpty())
                obj = "0";
            obj = new BigDecimal("" + obj);
        } else if (type == Integer.class || type == int.class) {
            if (obj == null || (""+obj).isEmpty())
                obj = "0";
            if(obj instanceof Integer){

            }else
                obj = new BigDecimal(""+obj).intValue();
        } else if (type == Long.class || type==long.class) {
            if (obj == null || (""+obj).isEmpty())
                obj = "0";
            if(obj instanceof Long){

            }else
                obj = new BigDecimal(""+obj).longValue();
        } else if (type == Float.class || type==float.class) {
            if (obj == null || (""+obj).isEmpty())
                obj = "0";
            if(obj instanceof Float){

            }else
                obj = new BigDecimal(""+obj).floatValue();
        } else if (type == Double.class || type==double.class) {
            if (obj == null || (""+obj).isEmpty())
                obj = "0";
            if(obj instanceof Double){

            }else
                obj = new BigDecimal(""+obj).doubleValue();
        } else if (type == Boolean.class || type==boolean.class) {
            if (obj == null || (""+obj).isEmpty())
                obj = "false";
            if(obj instanceof Number){
                obj = ((Number) obj).intValue()!=0;
            }else
                obj = Boolean.parseBoolean("" + obj);
        } else if (type == Date.class) {
            if((""+obj).isEmpty())
                obj = null;
            if (obj != null) {
                if (obj instanceof String) {
                    obj = new Date(Long.parseLong("" + obj));
                } else if (obj instanceof Long) {
                    obj = new Date((Long) obj);
                }
            }
        } else if(type.isEnum()){
            if(obj != null && obj instanceof String){
                obj = Enum.valueOf((Class<? extends Enum>)type,((String) obj).toUpperCase());
            }
        }

        return (T) obj;
    }

}
