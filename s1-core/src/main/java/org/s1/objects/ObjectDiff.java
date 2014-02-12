package org.s1.objects;

import org.s1.misc.Closure;

import java.util.List;
import java.util.Map;

/**
 * Object diff helper
 */
public class ObjectDiff {

    /**
     * Get difference between two maps
     *
     * @param oldObject
     * @param newObject
     * @return
     */
    static List<DiffBean> diff(Map<String, Object> oldObject, Map<String, Object> newObject) {
        if (oldObject == null)
            oldObject = Objects.newHashMap();
        if (newObject == null)
            newObject = Objects.newHashMap();
        oldObject = Objects.copy(oldObject);
        newObject = Objects.copy(newObject);
        final List<DiffBean> diff = Objects.newArrayList();

        Closure<TwoMapDiffBean, Object> itr = new Closure<TwoMapDiffBean, Object>() {
            @Override
            public Object call(final TwoMapDiffBean d) {
                ObjectIterator.iterate(d.getM1(), new Closure<ObjectIterator.IterateBean, Object>() {
                    @Override
                    public Object call(ObjectIterator.IterateBean i) {
                        Object o = i.getValue();
                        if (o instanceof Map) {

                        } else if (o instanceof List) {

                        } else {
                            Object oo = null;
                            if (!Objects.isNullOrEmpty(i.getPath()))
                                oo = ObjectPath.get(d.getM2(), i.getPath(), null);
                            if (d.isM1Old()) {
                                if (!Objects.equals(o, oo)) {
                                    final DiffBean res = new DiffBean(i.getPath(),o,oo);
                                    if (Objects.find(diff, new Closure<DiffBean, Boolean>() {
                                        @Override
                                        public Boolean call(DiffBean input) {
                                            return input.getPath().equals(res.getPath());
                                        }
                                    }) == null)
                                        diff.add(res);
                                }
                            } else {
                                if (!Objects.equals(o, oo)) {
                                    final DiffBean res = new DiffBean(i.getPath(),oo,o);
                                    if (Objects.find(diff, new Closure<DiffBean, Boolean>() {
                                        @Override
                                        public Boolean call(DiffBean input) {
                                            return input.getPath().equals(res.getPath());
                                        }
                                    }) == null)
                                        diff.add(res);
                                }
                            }
                        }
                        return o;
                    }
                });
                return null;
            }
        };
        itr.callQuite(new TwoMapDiffBean(oldObject, newObject, true));
        itr.callQuite(new TwoMapDiffBean(newObject, oldObject, false));

        return diff;
    }

    /**
     * Map diff result bean
     */
    public static class DiffBean {
        private String path;
        private Object oldValue;
        private Object newValue;

        public DiffBean(String path, Object oldValue, Object newValue) {
            this.path = path;
            this.oldValue = oldValue;
            this.newValue = newValue;
        }

        public String getPath() {
            return path;
        }

        public Object getOldValue() {
            return oldValue;
        }

        public Object getNewValue() {
            return newValue;
        }
    }

    /**
     *
     */
    private static class TwoMapDiffBean {
        private Map<String, Object> m1;
        private Map<String, Object> m2;
        private boolean m1Old;

        private TwoMapDiffBean(Map<String, Object> m1, Map<String, Object> m2, boolean m1Old) {
            this.m1 = m1;
            this.m2 = m2;
            this.m1Old = m1Old;
        }

        public Map<String, Object> getM1() {
            return m1;
        }

        public Map<String, Object> getM2() {
            return m2;
        }

        public boolean isM1Old() {
            return m1Old;
        }
    }

}
