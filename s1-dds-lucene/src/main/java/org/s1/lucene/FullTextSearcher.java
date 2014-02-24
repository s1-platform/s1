package org.s1.lucene;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.*;
import org.apache.lucene.queries.TermsFilter;
import org.apache.lucene.search.*;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.highlight.Fragmenter;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.InvalidTokenOffsetsException;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleSpanFragmenter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.s1.S1SystemError;
import org.s1.cluster.Locks;
import org.s1.cluster.node.ClusterNode;
import org.s1.misc.Closure;
import org.s1.misc.ClosureException;
import org.s1.objects.Objects;
import org.s1.options.Options;
import org.s1.table.format.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Full-Text searcher
 */
public class FullTextSearcher {

    private static final Logger LOG = LoggerFactory.getLogger(FullTextSearcher.class);

    private long writerLivetime = 1800000L;
    private long readerLivetime = 600000L;
    private String name;
    private Map<String,FieldBean> fields = Objects.newHashMap();

    private Directory directory;
    private IndexWriter writer;
    private long writerCreated = 0;
    private IndexSearcher searcher;
    private long readerCreated = 0;
    private Analyzer analyzer;
    private Closure<org.s1.table.format.Query, Object> prepareFilter;

    public static final int WRITE_LOCK_TIMEOUT = 120000;

    /**
     *
     */
    public void init(){
        if(Objects.isNullOrEmpty(name))
            throw new IllegalStateException("Name must not be null or empty");
        String path = Options.getStorage().getSystem("lucene.storagePath");
        new File(path+"/"+name).mkdirs();
        new File(path+"/"+name+"/write.lock").delete();
        try {
            this.directory = FSDirectory.open(new File(path+"/"+name));
        } catch (IOException e) {
            throw S1SystemError.wrap(e);
        }
        LOG.info("Lucene full-text searcher opened: "+path+"/"+name);
    }

    public Map<String, FieldBean> getFields() {
        return fields;
    }

    public void setFields(Map<String, FieldBean> fields) {
        this.fields = fields;
    }

    public long getWriterLivetime() {
        return writerLivetime;
    }

    public void setWriterLivetime(long writerLivetime) {
        this.writerLivetime = writerLivetime;
    }

    public long getReaderLivetime() {
        return readerLivetime;
    }

    public void setReaderLivetime(long readerLivetime) {
        this.readerLivetime = readerLivetime;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Closure<org.s1.table.format.Query, Object> getPrepareFilter() {
        return prepareFilter;
    }

    public void setPrepareFilter(Closure<org.s1.table.format.Query, Object> prepareFilter) {
        this.prepareFilter = prepareFilter;
    }

    /**
     *
     * @return
     */
    public Version getVersion(){
        return Version.LUCENE_46;
    }

    /**
     *
     * @return
     */
    public synchronized IndexWriter getWriter(){
        if(writer!=null && (System.currentTimeMillis()-writerCreated)>writerLivetime){
            try {
                writer.close();
            } catch (IOException e) {
                throw S1SystemError.wrap(e);
            }
            writer = null;
        }

        if(writer==null){
            try {
                writer = new IndexWriter(directory, getWriterConfig());
            } catch (IOException e) {
                throw S1SystemError.wrap(e);
            }
            LOG.info("Lucene index writer opened");
            writerCreated = System.currentTimeMillis();
        }
        return writer;
    }

    /**
     *
     * @return
     */
    public IndexWriterConfig getWriterConfig(){
        IndexWriterConfig config = new IndexWriterConfig(getVersion(), getAnalyzer());
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        return config;
    }

    /**
     *
     * @return
     */
    public synchronized IndexSearcher getSearcher(){
        try{
            if(searcher!=null
                    && !((DirectoryReader)searcher.getIndexReader()).isCurrent()
                    && (System.currentTimeMillis()-readerCreated>readerLivetime)){
                searcher = null;
            }
            if(searcher==null){
                IndexReader reader = DirectoryReader.open(this.directory);
                searcher = new IndexSearcher(reader);
                LOG.info("Lucene index searcher opened: "+searcher.getIndexReader().numDocs()+" documents total");
                readerCreated = System.currentTimeMillis();
            }
            return searcher;
        }catch (IOException e){
            throw S1SystemError.wrap(e);
        }
    }


    /**
     *
     * @return
     */
    public synchronized Analyzer getAnalyzer(){
        if(analyzer==null){
            analyzer = new StandardAnalyzer(getVersion());
        }
        return analyzer;
    }

    /**
     *
     * @param text
     * @return
     */
    public List<String> getWords(String text){
        try{
            TokenStream t = getAnalyzer().tokenStream(null,text);
            List<String> result = Objects.newArrayList();
            t.reset();
            while (t.incrementToken()) {
                result.add(t.getAttribute(CharTermAttribute.class).toString());
            }
            t.end();
            t.close();
            return result;
        }catch (IOException e){
            throw S1SystemError.wrap(e);
        }
    }

    /**
     *
     * @param query
     * @param lastWildcard
     * @param fuzzy
     * @return
     */
    public Query getQuery(String query, boolean lastWildcard, boolean fuzzy){

        Query q = null;

        if(fields!=null){
            String originalQuery = query;
            q = new BooleanQuery();

            //phrases
            List<String> phrases = Objects.newArrayList();
            final Pattern expr = Pattern.compile(Pattern.quote("\"")+"(.+?)"+Pattern.quote("\""));
            final Matcher matcher = expr.matcher(query);
            while (matcher.find()) {
                String text = matcher.group(1);
                query = query.replace("\""+text+"\"", "");
                phrases.add(text);
            }

            //words
            List<String> words = getWords(query);
            String wildcardWord = null;
            if(lastWildcard && words.size()>0 && originalQuery.endsWith(words.get(words.size()-1))){
                wildcardWord = words.get(words.size()-1);
                words.remove(words.size()-1);
            }

            DisjunctionMaxQuery dmaxWords = new DisjunctionMaxQuery(0.0F);
            DisjunctionMaxQuery dmaxPhrases = new DisjunctionMaxQuery(0.0F);
            for(String k:fields.keySet()){
                float boost = fields.get(k).getBoost();
                if(boost>0){
                    for(String w:words){
                        Query tq = new TermQuery(new Term(k,w));
                        if(fuzzy)
                            tq = new FuzzyQuery(new Term(k,w));
                        tq.setBoost(boost);
                        dmaxWords.add(tq);
                    }
                    for(String p:phrases){
                        PhraseQuery tq = new PhraseQuery();
                        List<String> ws = getWords(p);
                        for(String w:ws){
                            tq.add(new Term(k,w));
                        }
                        tq.setSlop(0);
                        tq.setBoost(boost);
                        dmaxPhrases.add(tq);
                    }
                    if(wildcardWord!=null){
                        WildcardQuery tq = new WildcardQuery(new Term(k,wildcardWord+"*"));
                        tq.setBoost(boost);
                        dmaxWords.add(tq);
                    }
                }
            }
            ((BooleanQuery)q).add(dmaxWords,BooleanClause.Occur.SHOULD);
            ((BooleanQuery)q).add(dmaxPhrases,BooleanClause.Occur.SHOULD);
        }/*else{
            try {
                q = new QueryParser(getVersion(),"text",getAnalyzer()).parse(query);
            } catch (ParseException e) {
                throw S1SystemError.wrap(e);
            }
        }*/

        return q;
    }

    /**
     *
     * @param filter
     * @return
     */
    public Filter getFilter(org.s1.table.format.Query filter){
        Filter f = null;
        if(filter==null)
            filter = new org.s1.table.format.Query();
        if(prepareFilter!=null){
            prepareFilter.callQuite(filter);
        }

        Query q = LuceneFormat.formatSearch(filter);
        if(q!=null)
            f = new QueryWrapperFilter(q);
        return f;
    }

    /**
     *
     * @param doc
     * @return
     */
    public Map<String,Object> toMap(Document doc){
        Map<String,Object> m = Objects.newHashMap();
        for(String k:fields.keySet()){
            m.put(k,fields.get(k).toValue(doc.getField(k)));
        }
        m.put("id",doc.getField("id").stringValue());
        return m;
    }

    /**
     *
     * @param m
     * @return
     */
    public Document fromMap(Map<String,Object> m){
        Document doc = new Document();
        String id = Objects.get(m, "id");
        doc.add(new StringField("id",id,Field.Store.YES));
        for(String k:fields.keySet()){
            Field f = fields.get(k).toField(k, m.get(k));
            if(f!=null){
                doc.add(f);
            }
        }
        return doc;
    }

    /**
     *
     * @param query
     * @param filter
     * @param highlight
     * @param lastWildcard
     * @param fuzzyIfNotFound
     * @param skip
     * @param max
     * @return
     */
    public Map<String,Object> search(String query, org.s1.table.format.Query filter, boolean highlight, boolean lastWildcard, boolean fuzzyIfNotFound, int skip, int max){
        List<Map<String,Object>> list = Objects.newArrayList();
        Map<String,Object> result = Objects.newHashMap(
                "list",list
        );

        IndexSearcher searcher = getSearcher();
        Query q = getQuery(query,lastWildcard,false);
        Filter f = getFilter(filter);
        long t = System.currentTimeMillis();
        TopDocs td = null;
        try {
            td = searcher.search(q,f,skip+max);
        } catch (IOException e) {
            throw S1SystemError.wrap(e);
        }
        if(LOG.isDebugEnabled())
            LOG.debug("Search: "+query+", filter: "+filter+", highlight:"+highlight+", lastWildcard:"+lastWildcard+", fuzzyIfNotFound:"+fuzzyIfNotFound+", skip:"+skip+", max:"+max+
                    "\n\t>query parsed: "+q+", filter parsed:"+f+
                    "\n\t>found:"+td.totalHits+" in "+(System.currentTimeMillis()-t)+"ms.");
        if(td.totalHits==0 && fuzzyIfNotFound){
            result.put("notFound",true);
            q = getQuery(query,lastWildcard,true);
            try {
                td = searcher.search(q,f,skip+max);
            } catch (IOException e) {
                throw S1SystemError.wrap(e);
            }
            if(LOG.isDebugEnabled())
                LOG.debug("Fuzzy search: "+query+", filter: "+filter+", highlight:"+highlight+", lastWildcard:"+lastWildcard+", fuzzyIfNotFound:"+fuzzyIfNotFound+", skip:"+skip+", max:"+max+
                        "\n\t>query parsed: "+q+", filter parsed:"+f+
                        "\n\t>found:"+td.totalHits+" in "+(System.currentTimeMillis()-t)+"ms.");
        }

        QueryScorer scorer = new QueryScorer(q);
        Fragmenter fragmenter = new SimpleSpanFragmenter(scorer);
        Highlighter highlighter = new Highlighter(scorer);
        highlighter.setTextFragmenter(fragmenter);
        Analyzer analyzer = getAnalyzer();
        for(int i=skip;i<td.scoreDocs.length;i++){
            Document doc = null;
            try {
                doc = searcher.doc(td.scoreDocs[i].doc);
            } catch (IOException e) {
                throw S1SystemError.wrap(e);
            }
            Map<String,Object> m = toMap(doc);
            Map<String,Object> o = Objects.newHashMap(
                    "id",m.get("id"),
                    "data",m,
                    "score",td.scoreDocs[i].score
            );
            if(highlight){
                Map<String,Object> hlMap = Objects.copy(m);
                o.put("highlight",hlMap);
                for(String k:fields.keySet()){
                    //for text fields
                    if(fields.get(k).isText()){
                        try {
                            String text = Objects.get(hlMap,k);
                            TokenStream tokenStream = analyzer.tokenStream(k,text);
                            String hl = highlighter.getBestFragment(tokenStream,text);
                            if(hl!=null)
                                hlMap.put(k,hl);
                        }catch (InvalidTokenOffsetsException e){
                        }catch (IOException e){
                            throw S1SystemError.wrap(e);
                        }
                    }
                }
            }
            list.add(o);
        }
        result.put("count",td.totalHits);
        return result;
    }

    /**
     *
     * @param id
     * @return
     */
    protected String getGroup(String id){
        return name+":"+id;
    }

    /**
     *
     * @param id
     */
    public void waitForRecord(String id){
        ClusterNode.flush(LuceneDDS.class,getGroup(id));
    }

    /**
     *
     * @param id
     * @param data
     * @return
     */
    public void addDocument(String id, Map<String,Object> data){
        ClusterNode.call(LuceneDDS.class, "add",Objects.newHashMap(String.class,Object.class,
                "name",name,
                "id",id,
                "data",data
        ),getGroup(id));
    }

    /**
     *
     * @param id
     * @param data
     * @return
     */
    public void setDocument(String id, Map<String,Object> data){
        ClusterNode.call(LuceneDDS.class, "set",Objects.newHashMap(String.class,Object.class,
                "name",name,
                "id",id,
                "data",data
        ),getGroup(id));
    }

    /**
     *
     * @param id
     * @return
     */
    public void removeDocument(String id){
        ClusterNode.call(LuceneDDS.class, "remove",Objects.newHashMap(String.class,Object.class,
                "name",name,
                "id",id
        ),getGroup(id));
    }

    /**
     *
     * @param id
     * @param doc
     * @return
     */
    public void localAddDocument(final String id, final Map<String,Object> doc){
        doc.put("id",id);
        try{
            Locks.waitAndRun(getLockName(),new Closure<String, Object>() {
                @Override
                public Object call(String input) throws ClosureException {
                    IndexWriter writer = getWriter();
                    try {
                        writer.addDocument(fromMap(doc));
                        writer.commit();
                    } catch (IOException e) {
                        throw S1SystemError.wrap(e);
                    }
                    return null;
                }
            },WRITE_LOCK_TIMEOUT, TimeUnit.MILLISECONDS);
        }catch (Exception e){
            throw S1SystemError.wrap(e);
        }
    }

    /**
     *
     * @param id
     * @param doc
     * @return
     */
    public void localSetDocument(final String id, final Map<String,Object> doc){
        doc.put("id",id);
        try{
            Locks.waitAndRun(getLockName(),new Closure<String, Object>() {
                @Override
                public Object call(String input) throws ClosureException {
                    IndexWriter writer = getWriter();
                    try {
                        writer.updateDocument(new Term("id",id),fromMap(doc));
                        writer.commit();
                    } catch (IOException e) {
                        throw S1SystemError.wrap(e);
                    }
                    return null;
                }
            },WRITE_LOCK_TIMEOUT, TimeUnit.MILLISECONDS);
        }catch (Exception e){
            throw S1SystemError.wrap(e);
        }
    }

    /**
     *
     * @param id
     * @return
     */
    public void localRemoveDocument(final String id){
        try{
            Locks.waitAndRun(getLockName(),new Closure<String, Object>() {
                @Override
                public Object call(String input) throws ClosureException {
                    IndexWriter writer = getWriter();
                    try {
                        writer.deleteDocuments(new Term("id",id));
                        writer.commit();
                    } catch (IOException e) {
                        throw S1SystemError.wrap(e);
                    }
                    return null;
                }
            },WRITE_LOCK_TIMEOUT, TimeUnit.MILLISECONDS);
        }catch (Exception e){
            throw S1SystemError.wrap(e);
        }
    }

    /**
     *
     * @return
     */
    protected String getLockName(){
        return "index.change:"+name;
    }
}
