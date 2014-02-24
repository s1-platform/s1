package org.s1.table.internal;

import org.s1.table.ActionBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Table import
 */
class TableAccessControler {

    private static final Logger LOG = LoggerFactory.getLogger(TableAccessControler.class);

    private TableBase table;

    /**
     *
     * @param table
     */
    public TableAccessControler(TableBase table) {
        this.table = table;
    }

    /**
     * @return
     */
    public boolean isAccessAllowed() {
        if (table.getAccess() != null) {
            return table.getAccess().callQuite(null);
        }
        return true;
    }

    /**
     * @param record
     * @return
     */
    public boolean isLogAccessAllowed(Map<String, Object> record) {
        if (table.getLogAccess() != null) {
            return table.getLogAccess().callQuite(record);
        }
        return true;
    }

    /**
     * @return
     */
    public boolean isImportAllowed() {
        if (table.getImportAccess() != null) {
            return table.getImportAccess().callQuite(null);
        }
        return true;
    }

    /**
     * @param action
     * @param record
     * @return
     */
    public boolean isActionAllowed(ActionBean action, Map<String, Object> record) {
        if (action.getAccess() != null) {
            return action.getAccess().callQuite(record);
        }
        return true;
    }
}
