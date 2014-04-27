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

package org.s1.mongodb.log;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.WriteConcern;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;
import org.s1.log.Loggers;
import org.s1.mongodb.MongoDBConnectionHelper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * Log4j Appender
 */
public class MongoDBAppender extends AppenderSkeleton {

    private volatile DBCollection coll;

    public MongoDBAppender(){
        coll = MongoDBLogStorage.getCollection();
    }

    @Override
    protected void append(LoggingEvent loggingEvent) {
        try{
            Map<String,Object> m = Loggers.toMap(loggingEvent);
            coll.insert(new BasicDBObject(m), WriteConcern.NONE);
        }catch (Throwable e){
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            System.err.println(sdf.format(new Date())+": MongoDBLog4jAppender error");
            e.printStackTrace();
        }
    }

    @Override
    public void close() {
    }

    @Override
    public boolean requiresLayout() {
        return false;
    }
}