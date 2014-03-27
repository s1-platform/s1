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

package org.s1.cluster.dds.file;

import org.s1.cluster.dds.beans.Id;
import org.s1.objects.Objects;
import org.s1.objects.schema.ComplexType;
import org.s1.objects.schema.errors.CustomValidationException;
import org.s1.objects.schema.errors.ValidationException;
import org.s1.table.Table;
import org.s1.table.Tables;
import org.s1.table.errors.NotFoundException;

import java.util.Map;

/**
 * Join complex type
 */
public class FileComplexType extends ComplexType{

    @Override
    public Map<String, Object> expand(Map<String, Object> m, boolean expand) throws Exception {
        String database = Objects.get(m,"database");
        String collection = Objects.get(m,"collection");
        String entity = Objects.get(m,"entity");
        FileStorage.FileReadBean b = null;
        try{
            b = FileStorage.read(new Id(database,collection,entity));
            m.put("meta", b.getMeta().toMap());
        }finally {
            FileStorage.closeAfterRead(b);
        }
        return m;
    }

    @Override
    public Map<String, Object> validate(Map<String, Object> m) throws ValidationException {
        String database = Objects.get(m,"database");
        String collection = Objects.get(m,"collection");
        String entity = Objects.get(m,"entity");
        FileStorage.FileReadBean b = null;
        try{
            b = FileStorage.read(new Id(database,collection,entity));
        }catch (NotFoundException e){
            throw new CustomValidationException(e.getMessage(),e);
        }finally {
            FileStorage.closeAfterRead(b);
        }

        return m;
    }
}
