package org.s1.weboperation;

import org.s1.cluster.datasource.FileStorage;
import org.s1.cluster.datasource.NotFoundException;
import org.s1.misc.Closure;
import org.s1.misc.ClosureException;
import org.s1.misc.IOUtils;
import org.s1.objects.Objects;
import org.s1.objects.schema.ObjectSchema;
import org.s1.objects.schema.SimpleTypeAttribute;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.*;

/**
 * s1v2
 * User: GPykhov
 * Date: 24.01.14
 * Time: 22:39
 */
public class UploadWebOperation extends MapWebOperation{
    public static final String GROUP = "upload";

    @Override
    protected Map<String, Object> process(String method, Map<String, Object> params, HttpServletRequest request, HttpServletResponse response) throws Exception {
        Map<String, Object> result = new HashMap<String, Object>();
        if ("upload".equals(method)) {
            upload(params, result);
        } else if ("download".equals(method)) {
            download(params, response);
            result = null;
        } else if ("downloadAsFile".equals(method)) {
            downloadAsFile(params, response);
            result = null;
        } else {
            throwMethodNotFound(method);
        }
        return result;
    }

    public static void upload(Map<String, Object> params,
                              Map<String, Object> result) throws Exception{
        String group = (String)params.get("group");
        if(Objects.isNullOrEmpty(group))
            group = GROUP;

        if (params.containsKey("file")) {
            String id = UUID.randomUUID().toString();

            final FileParameter fp = (FileParameter) params.get("file");

            FileStorage.write(group, id, new Closure<OutputStream, Boolean>() {
                @Override
                public Boolean call(OutputStream os) throws ClosureException {
                    try {
                        IOUtils.copy(fp.getInputStream(),os);
                    } catch (IOException e) {
                        throw ClosureException.wrap(e);
                    }
                    return null;
                }
            }, new FileStorage.FileMetaBean(fp.getName(),fp.getExt(),fp.getContentType(),fp.getSize(),null));
            result.put("id", id);
        } else {
            List<String> ids = new ArrayList<String>();
            int i = 0;
            while (result.containsKey("file" + i)) {
                String id = UUID.randomUUID().toString();

                final FileParameter fp = (FileParameter) params.get("file"+i);

                FileStorage.write(group, id, new Closure<OutputStream, Boolean>() {
                    @Override
                    public Boolean call(OutputStream os) throws ClosureException {
                        try {
                            IOUtils.copy(fp.getInputStream(),os);
                        } catch (IOException e) {
                            throw ClosureException.wrap(e);
                        }
                        return null;
                    }
                }, new FileStorage.FileMetaBean(fp.getName(),fp.getExt(),fp.getContentType(),fp.getSize(),null));
                ids.add(id);
            }
            result.put("ids", ids);
        }
    }

    /**
     *
     * @param params
     * @param response
     */
    public static void download(Map<String, Object> params,
                                final HttpServletResponse response) throws Exception{

        params = new ObjectSchema(
                new SimpleTypeAttribute("id","id",String.class).setRequired(true),
                new SimpleTypeAttribute("group","group",String.class).setRequired(true).setDefault(GROUP)
        ).validate(params);

        try{
            FileStorage.read(Objects.get(String.class, params, "group"), Objects.get(String.class, params, "id"), new Closure<FileStorage.FileReadBean, Object>() {
                @Override
                public Object call(FileStorage.FileReadBean fb) throws ClosureException {
                    response.setContentType(fb.getMeta().getContentType());
                    try {
                        IOUtils.copy(fb.getInputStream(), response.getOutputStream());
                    } catch (IOException e) {
                        throw ClosureException.wrap(e);
                    }
                    return null;
                }
            });
        }catch (NotFoundException e){
            response.setStatus(404);
        }
    }

    /**
     * Download file from storage with content-disposition
     * @param params
     * @param response
     */
    public static void downloadAsFile(Map<String, Object> params,
                                      final HttpServletResponse response) throws Exception {
        final Map<String,Object> p = new ObjectSchema(
                new SimpleTypeAttribute("id","id",String.class).setRequired(true),
                new SimpleTypeAttribute("group","group",String.class).setRequired(true).setDefault(GROUP),
                new SimpleTypeAttribute("name","name",String.class)
        ).validate(params);

        try{
            FileStorage.read(Objects.get(String.class, p, "group"), Objects.get(String.class, p, "id"), new Closure<FileStorage.FileReadBean, Object>() {
                @Override
                public Object call(FileStorage.FileReadBean fb) throws ClosureException {
                    String name = Objects.get(p,"name");
                    if(Objects.isNullOrEmpty(name)){
                        name = fb.getMeta().getName();
                        if(name.length()>100)
                            name = name.substring(0,100);
                        if(!Objects.isNullOrEmpty(fb.getMeta().getExt()))
                            name += "."+fb.getMeta().getExt();
                    }
                    response.setContentType(fb.getMeta().getContentType());
                    try {
                        response.addHeader("Content-Disposition", "attachment;filename="+ URLEncoder.encode(name, "UTF-8"));
                        IOUtils.copy(fb.getInputStream(), response.getOutputStream());
                    } catch (IOException e) {
                        throw ClosureException.wrap(e);
                    }
                    return null;
                }
            });
        }catch (NotFoundException e){
            response.setStatus(404);
        }
    }


}
