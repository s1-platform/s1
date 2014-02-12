package org.s1.cluster.datasource;

import org.s1.S1SystemError;
import org.s1.misc.FileUtils;
import org.s1.misc.IOUtils;
import org.s1.objects.Objects;
import org.s1.options.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Number sequence local implementation
 */
public class NumberSequenceLocalStorage {

    private static final Logger LOG = LoggerFactory.getLogger(NumberSequenceLocalStorage.class);

    /**
     *
     * @param name
     * @return
     */
    public long read(String name){
        String s = null;
        try {
            s = FileUtils.readFileToString(new File(getFile(name)), "UTF-8");
        } catch (IOException e) {
            throw S1SystemError.wrap(e);
        }
        if(s==null)
            s = "-1";
        long value = Objects.cast(s,Long.class);
        if(LOG.isDebugEnabled())
            LOG.debug("Read value: "+value+" from sequence local storage: "+name);
        return value;
    }

    /**
     *
     * @param name
     * @param value
     */
    public void write(String name, long value){
        try {
            FileUtils.writeStringToFile(new File(getFile(name)),""+value,"UTF-8");
        } catch (IOException e) {
            S1SystemError.wrap(e);
        }
        if(LOG.isDebugEnabled())
            LOG.debug("Write successfully value: "+value+" to sequence: "+name);
    }

    private static String baseDirectory;

    /**
     * Get file and ensure dir exists, cache directory
     *
     * @param name
     * @return
     */
    private static synchronized String getFile(String name) {
        if(Objects.isNullOrEmpty(baseDirectory)){
            baseDirectory = Options.getStorage().getSystem("numberSequence.home", System.getProperty("user.home") + File.separator + ".s1-sequences");
        }
        File dir = new File(baseDirectory);
        if(!dir.exists())
            dir.mkdirs();
        if(!dir.isDirectory())
            throw new S1SystemError("Directory error: "+baseDirectory);
        return dir.getAbsolutePath()+File.separator+name;
    }

}
