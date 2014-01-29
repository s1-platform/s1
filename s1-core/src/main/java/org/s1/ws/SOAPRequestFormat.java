package org.s1.ws;

import org.s1.format.xml.XMLFormat;
import org.s1.misc.Closure;
import org.s1.objects.Objects;
import org.w3c.dom.Element;

import javax.xml.soap.SOAPMessage;
import java.util.Map;

/**
 * s1v2
 * User: GPykhov
 * Date: 25.01.14
 * Time: 12:13
 */
public class SOAPRequestFormat {

    /**
     *
     * @param descriptor
     * @param request
     * @param userData
     * @param ctx
     * @param beforeSend
     * @param afterSend
     * @return
     */
    public Map<String,Object> send(Map<String, Object> descriptor, Map<String, Object> request, Map<String, Object> userData, Map<String, Object> ctx,
                                   Closure beforeSend, Closure afterSend){

        boolean mtom = useMTOM(descriptor,request,userData,ctx);
        String endpoint = getEndpoint(descriptor,request,userData,ctx);
        SOAPMessage msg = createMessageFromTemplate(mtom, Objects.copy(request),
                Objects.copy(descriptor),
                Objects.copy(userData),
                ctx);
        //post
        msg = prepareMessage(mtom,msg,Objects.copy(request),
                Objects.copy(descriptor),
                Objects.copy(userData),
                ctx);

        //if(beforeSend!=null)
        //    beforeSend.call(msg,endpoint);
        SOAPMessage resultMsg = null;
        try{
            resultMsg = SOAPHelper.send(endpoint,msg);
        }catch(Throwable e){
            //throw new AppException(LocaleHelper.get("/com/pgg/s1/ws/i18n","error.request.send",[:])+": "
            //        +e.getClass().getName()+": "+e.getMessage(),e);
        }
        //if(afterSend!=null)
        //    afterSend.call(resultMsg);

        Map<String,Object> response = parseResponse(resultMsg,Objects.copy(request),
                Objects.copy(descriptor),
                Objects.copy(userData),
                ctx);

        processResponseAfter(resultMsg,response,Objects.copy(request),
                Objects.copy(descriptor),
                Objects.copy(userData),
                ctx);
        if(!Objects.get(response,"success",false)){
            //throw new AppException(LocaleHelper.get("/com/pgg/s1/ws/i18n","error.request.success",[:])+": "+response.message);
        }
        return Objects.get(response,"data");
    }

    /**
     *
     * @param descriptor
     * @param request
     * @param userData
     * @param ctx
     * @return
     */
    protected boolean useMTOM(Map<String, Object> descriptor, Map<String, Object> request, Map<String, Object> userData, Map<String, Object> ctx) {
        return Objects.get(ctx,"useMTOM",false);
    }

    /**
     *
     * @param descriptor
     * @param request
     * @param userData
     * @param ctx
     * @return
     */
    protected String getEndpoint(Map<String, Object> descriptor, Map<String, Object> request, Map<String, Object> userData, Map<String, Object> ctx) {
        return Objects.get(ctx,"endpoint",Objects.get(String.class,descriptor,"endpoint"));
    }

    /**
     *
     * @param request
     * @param descriptor
     * @param userData
     * @param ctx
     * @return
     */
    protected SOAPMessage createMessageFromTemplate(boolean mtom, Map<String, Object> request, Map<String, Object> descriptor, Map<String, Object> userData, Map<String, Object> ctx){
        /*try{
            SOAPMessage msg = SOAPHelper.createSoapFromString(
                    TemplateEngine.getInstance().bind(descriptor.template,[
                            request: request,
                    userData: userData,
                    descriptor: descriptor,
                    ctx: ctx
            ]).trim()
            )
            return msg;
        }catch (Exception e){
            throw new AppException(LocaleHelper.get("/com/pgg/s1/ws/i18n","error.request.template",[:])+": "+e.getMessage(),e);
        }*/
        return null;
    }

    /**
     *
     * @param msg
     * @param request
     * @param descriptor
     * @param userData
     * @param ctx
     * @return
     */
    protected SOAPMessage prepareMessage(boolean mtom, SOAPMessage msg, Map<String, Object> request, Map<String, Object> descriptor, Map<String, Object> userData, Map<String, Object> ctx){
        /*if(descriptor.prepare){
            try{
                def m = TemplateEngine.getInstance().eval(descriptor.prepare.script,[
                        request: request,
                        userData: userData,
                        descriptor: descriptor,
                        ctx: ctx,
                        msg: msg
                ]);
                if(m && m instanceof SOAPMessage)
                    msg = m;
            }catch (Exception e){
                throw new AppException(LocaleHelper.get("/com/pgg/s1/ws/i18n","error.request.prepare",[:])+": "+e.getMessage(),e);
            }
        }*/
        return msg;
    }

    /**
     *
     * @param msg
     * @param request
     * @param descriptor
     * @param userData
     * @param ctx
     * @return
     */
    protected Map<String,Object> parseResponse(SOAPMessage msg, Map<String, Object> request, Map<String, Object> descriptor, Map<String, Object> userData, Map<String, Object> ctx){
        Map<String,Object> m = Objects.newHashMap();
        Element body = XMLFormat.getElement(SOAPHelper.getEnvelope(msg), "Body", null);
        //check fault
        Element fault = XMLFormat.getElement(SOAPHelper.getEnvelope(msg),"Body.Fault",null);
        if(fault!=null){
            return Objects.newHashMap("success",false,"message",XMLFormat.get(fault,"faultstring",null));
        }
        return Objects.newHashMap("success",true,"data",m);
    }

    /**
     *
     * @param msg
     * @param response
     * @param request
     * @param descriptor
     * @param userData
     * @param ctx
     */
    protected void processResponseAfter(SOAPMessage msg, Map<String, Object> response, Map<String, Object> request, Map<String, Object> descriptor, Map<String, Object> userData, Map<String, Object> ctx){
        /*if(descriptor.after){
            try{
                TemplateEngine.getInstance().eval(descriptor.after.script,[
                        request: request,
                        userData: userData,
                        descriptor: descriptor,
                        ctx: ctx,
                        msg: msg,
                        response: response
                ]);
            }catch (Exception e){
                response.clear();
                response.success = false;
                response.message = LocaleHelper.get("/com/pgg/s1/ws/i18n","error.outbox.after",[:])+": "+e.getMessage();
                //throw new AppException(LocaleHelper.get("/com/pgg/interdept/i18n","outbox.after",[:])+": "+e.getMessage(),e);
            }
        }*/
    }

}
