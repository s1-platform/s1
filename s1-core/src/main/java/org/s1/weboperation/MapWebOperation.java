package org.s1.weboperation;

import org.s1.S1SystemError;
import org.s1.format.json.JSONFormat;
import org.s1.format.json.JSONFormatException;
import org.s1.misc.IOUtils;
import org.s1.objects.Objects;
import org.slf4j.MDC;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Base class for Map-in/Map-out web operations
 */
public abstract class MapWebOperation extends WebOperation<Map<String,Object>,Map<String,Object>> {

    public static final String CALLBACK_PARAMETER = "_callback";
    public static final String PARAMS_PARAMETER = "_params";

    /**
     * Parse request to make Map. Try application/json, url params, application/x-www-form-urlencoded, multipart/form-data
     *
     * @param request
     * @return
     */
    public static Map<String, Object> convertRequestToMap(
            HttpServletRequest request) throws IOException, ServletException, JSONFormatException {

        Map<String, Object> inParams = new HashMap<String, Object>();

        //GET
        String q = request.getQueryString();
        if(!org.s1.objects.Objects.isNullOrEmpty(q)){
            String [] arr = q.split("&");
            for(String it:arr){
                String nv [] = it.split("=");
                String n = nv[0];
                String v = nv[1];
                try {
                    v = URLDecoder.decode(v, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    throw S1SystemError.wrap(e);
                }
                if(PARAMS_PARAMETER.equals(n)){
                    inParams.putAll(Objects.fromWire(JSONFormat.evalJSON(v)));
                }else{
                    inParams.put(n,v);
                }
            }
        }
        if (request.getContentType()!=null && request.getContentType().contains("application/json")) {
            //JSON
            String s = IOUtils.toString(request.getInputStream(), "UTF-8");
            inParams = JSONFormat.evalJSON(s);
        } else if (request.getContentType()!=null
                && request.getContentType().contains("application/x-www-form-urlencoded")
                ) {
            //POST
            Iterator<Map.Entry<String, String[]>> it = request
                    .getParameterMap().entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, String[]> e = it.next();
                String v = "" + e.getValue()[0];
                inParams.put(e.getKey(), v);
            }
        } else if (request.getContentType()!=null
                && request.getContentType().contains("multipart/form-data")
                ) {
            //parts
            for(Part p : request.getParts()){
                if(p.getContentType()!=null){
                    if(p.getSize()==0)
                        continue;
                    String name = p.getName();
                    //file
                    for (String content : p.getHeader("content-disposition").split(";")) {
                        if (content.trim().startsWith("filename")) {
                            name = content.substring(
                                    content.indexOf('=') + 1).trim().replace("\"", "");
                        }
                    }
                    String ext = "";
                    int ei = name.lastIndexOf(".");
                    if(ei!=-1 && ei<name.length()-1){
                        ext = name.substring(ei+1);
                        name = name.substring(0,ei);
                    }

                    inParams.put(p.getName(), new FileParameter(p.getInputStream(),name,ext,p.getContentType(),p.getSize()));
                }else{
                    //param
                    inParams.put(p.getName(), IOUtils.toString(p.getInputStream(),"UTF-8"));
                }
            }
        }
        inParams = Objects.fromWire(inParams);
        return inParams;
    }

    /**
     * Bean for file request parameter
     */
    public static class FileParameter {
        private InputStream inputStream;
        private String name;
        private String ext;
        private String contentType;
        private long size;

        public FileParameter(InputStream inputStream, String name, String ext, String contentType, long size) {
            this.inputStream = inputStream;
            this.name = name;
            this.ext = ext;
            this.contentType = contentType;
            this.size = size;
        }

        public InputStream getInputStream() {
            return inputStream;
        }

        public String getName() {
            return name;
        }

        public String getExt() {
            return ext;
        }

        public String getContentType() {
            return contentType;
        }

        public long getSize() {
            return size;
        }

        public String toString(){
            return "FileParameter {name: "+name+", ext: "+ext+", contentType: "+contentType+", size: "+size+"}";
        }
    }

    @Override
    protected Map<String,Object> parseInput(HttpServletRequest request) throws Exception{
        return convertRequestToMap(request);
    }

    @Override
    protected void formatOutput(Map<String, Object> out, boolean error,
                                HttpServletRequest request, HttpServletResponse response) throws Exception{
        if (out != null) {
            Map<String, Object> m = new HashMap<String, Object>();
            m.put("success", !error);
            m.put("data", out);
            /*if (request.getContentType() != null
                    && request.getContentType().contains("application/json"))*/
            response.setCharacterEncoding("UTF-8");
            String callback = request.getParameter(CALLBACK_PARAMETER);
            m = Objects.toWire(m);
            if(request.getMethod().equalsIgnoreCase("get") && !Objects.isNullOrEmpty(callback)){
                response.setContentType("text/javascript");
                response.getWriter().write(callback+"("+JSONFormat.toJSON(m)+");");
            }else{
                response.setContentType("application/json");
                response.getWriter().write(JSONFormat.toJSON(m));
            }
        }
    }

    @Override
    protected Map<String, Object> transformError(Throwable e, HttpServletRequest request, HttpServletResponse response) {
        if (e instanceof MethodNotFoundException) {
            response.setStatus(404);
        }
        return errorToMap(e);
    }

    /**
     * Convert error to map
     *
     * @param e
     * @return
     */
    public static Map<String, Object> errorToMap(Throwable e) {
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("requestId", MDC.get("requestId"));
        m.put("message", e.getMessage());
        m.put("errorClass", e.getClass().getName());
        return m;
    }
}
