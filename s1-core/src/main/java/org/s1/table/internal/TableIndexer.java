package org.s1.table.internal;

import org.s1.cluster.datasource.AlreadyExistsException;
import org.s1.cluster.datasource.MoreThanOneFoundException;
import org.s1.cluster.datasource.NotFoundException;
import org.s1.objects.Objects;
import org.s1.table.IndexBean;
import org.s1.table.Table;
import org.s1.table.format.FieldQueryNode;
import org.s1.table.format.GroupQueryNode;
import org.s1.table.format.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Table indexes
 */
class TableIndexer {

    private static final Logger LOG = LoggerFactory.getLogger(TableIndexer.class);

    private TableBase table;

    /**
     *
     * @param table
     */
    public TableIndexer(TableBase table) {
        this.table = table;
    }

    /**
     * @return
     */
    public List<IndexBean> getLogIndexes() {
        return Objects.newArrayList(IndexBean.class,
                new IndexBean(Objects.newArrayList(String.class, "date"), false, null),
                new IndexBean(Objects.newArrayList(String.class, "user"), false, null),
                new IndexBean(Objects.newArrayList(String.class, "id"), true, "id"),
                new IndexBean(Objects.newArrayList(String.class, "record"), false, null),
                new IndexBean(Objects.newArrayList(String.class, "action"), false, null)
        );
    }

    /**
     *
     */
    public void checkIndexes() {
        int i = 0;
        for (IndexBean b : table.getIndexes()) {
            table.collectionIndex(table.getCollection(), "index_" + i, b);
            i++;
        }

        i = 0;
        List<IndexBean> l = getLogIndexes();
        for (IndexBean b : l) {
            table.collectionIndex(table.getCollection(), "index_" + i, b);
            i++;
        }
    }

    /**
     *
     * @param object
     * @param isNew
     * @throws org.s1.cluster.datasource.AlreadyExistsException
     */
    protected void checkUnique(Map<String, Object> object, boolean isNew) throws AlreadyExistsException {
        //validate unique
        for (IndexBean ind : table.getIndexes()) {
            if (ind.isUnique()) {
                //check unique
                GroupQueryNode gqn = new GroupQueryNode(GroupQueryNode.GroupOperation.AND);
                Query search = new Query(gqn);
                String err = "";

                int i = 0;
                for (String f : ind.getFields()) {
                    i++;
                    gqn.getChildren().add(new FieldQueryNode(f, FieldQueryNode.FieldOperation.EQUALS, Objects.get(object, f)));
                    err += table.getAttributeLabel(f);
                    if (i < ind.getFields().size())
                        err += "; ";
                }
                if (!Objects.isNullOrEmpty(ind.getUniqueErrorMessage())) {
                    err = ind.getUniqueErrorMessage();
                }

                String id = Objects.get(object, "id");


                if(!isNew){
                    FieldQueryNode f = new FieldQueryNode("id", FieldQueryNode.FieldOperation.EQUALS, id);
                    f.setNot(true);
                    gqn.getChildren().add(f);
                }
                try {
                    try {
                        Map<String,Object> m = table.collectionGet(table.getCollection(), search);
                    } catch (MoreThanOneFoundException e) {
                    }
                    throw new AlreadyExistsException(err);
                } catch (NotFoundException e) {
                    //ok
                }
            }
        }
    }
}
