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

package org.s1.log;

import org.apache.log4j.Category;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.s1.S1SystemError;
import org.s1.objects.Objects;
import org.s1.options.Options;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

/**
 * Logging settings and helpers.<br>
 * ERROR - only system errors that make using of application impossible<br>
 * WARN - system errors that allow further using of application<br>
 * INFO - system state change messages<br>
 * DEBUG - all important data of every message crossing system perimeter<br>
 * TRACE - entire message<br>
 */
public class Loggers {

    /**
     * Convert logging event to map
     *
     * @param e
     * @return
     */
    public static Map<String,Object> toMap(LoggingEvent e){
        final Map<String,Object> m = Objects.newHashMap(
                "name",e.getLoggerName(),
                "date",new Date(e.getTimeStamp()),
                "level",e.getLevel().toString(),
                "thread",e.getThreadName(),
                "message",""+e.getMessage(),
                "fileName",e.getLocationInformation().getFileName(),
                "methodName",e.getLocationInformation().getMethodName(),
                "lineNumber",e.getLocationInformation().getLineNumber(),
                "requestId",e.getMDC("requestId"),
                "sessionId",e.getMDC("sessionId"),
                "freeMemory",Runtime.getRuntime().freeMemory(),
                "throwable",null
        );
        if(e.getThrowableInformation()!=null && e.getThrowableInformation().getThrowable()!=null){
            Throwable t = e.getThrowableInformation().getThrowable();
            m.put("throwable", Objects.newHashMap(
                    "message",t.getMessage(),
                    "class",t.getClass().getName(),
                    "stackTrace",getStackTrace(t)
            ));
        }
        return m;
    }

    private static String getStackTrace(Throwable e){
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString();
    }

    /**
     * Get all log classes names
     *
     * @return
     */
    public static Map<String,Object> getLogClasses(){
        SortedMap<String,Object> loggers = new TreeMap<String, Object>();
        Map<String,Object> res = Objects.newHashMap(
                "level",LogManager.getRootLogger().getLevel().toString().toUpperCase(),
                "loggers",loggers
        );

        Enumeration<Category> en = LogManager.getCurrentLoggers();
        while(en.hasMoreElements()){
            Category e = en.nextElement();
            loggers.put(e.getName(),e.getEffectiveLevel().toString().toUpperCase());
        }

        return res;
    }

    /**
     * Set log level
     *
     * @param cls if null, then rootLogger
     * @param level one of ['TRACE','DEBUG','INFO','WARN','ERROR']
     */
    public static void setLogLevel(String cls, String level){
        Logger l = null;
        if(Objects.isNullOrEmpty(cls))
            l = LogManager.getRootLogger();
        else
            l = LogManager.getLogger(cls);
        if(level.equalsIgnoreCase("TRACE"))
            l.setLevel(Level.TRACE);
        else if(level.equalsIgnoreCase("DEBUG"))
            l.setLevel(Level.DEBUG);
        else if(level.equalsIgnoreCase("INFO"))
            l.setLevel(Level.INFO);
        else if(level.equalsIgnoreCase("WARN"))
            l.setLevel(Level.WARN);
        else if(level.equalsIgnoreCase("ERROR"))
            l.setLevel(Level.ERROR);
    }

    private static LogStorage storage;

    /**
     * Get log storage
     * @return
     */
    public static synchronized LogStorage getLogStorage(){
        if(storage==null){
            String cls = Options.getStorage().getSystem("log.storageClass", LogStorage.class.getName());
            try{
                storage = (LogStorage)Class.forName(cls).newInstance();
            }catch (Exception e){
                throw S1SystemError.wrap(e);
            }
        }
        return storage;
    }

}
