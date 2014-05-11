package org.s1.objects;

import java.util.Map;

/**
 * @author Grigory Pykhov
 */
public interface MapSerializableObject {

    public Map<String,Object> toMap();

    public void fromMap(Map<String,Object> m);

}
