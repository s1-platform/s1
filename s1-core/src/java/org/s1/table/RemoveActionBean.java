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

package org.s1.table;

import org.s1.objects.BadDataException;
import org.s1.table.errors.ActionBusinessException;
import org.s1.user.AccessDeniedException;

import java.util.Map;

/**
 * Table action
 */
public class RemoveActionBean extends ActionBean{

    public RemoveActionBean(String name) {
        super(name);
    }

    public void run(String id, Map<String,Object> record, Map<String,Object> data)
            throws AccessDeniedException, ActionBusinessException, BadDataException {
        removeInternal(id);
    }

    protected final void removeInternal(String id){
        getTable().collectionRemove(id);
    }

}
