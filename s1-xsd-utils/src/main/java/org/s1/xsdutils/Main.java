package org.s1.xsdutils;

import org.s1.format.json.JSONFormat;
import org.s1.format.xml.XMLFormat;
import org.s1.misc.FileUtils;
import org.s1.objects.Objects;
import org.s1.options.Options;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * s1v2
 * User: GPykhov
 * Date: 27.01.14
 * Time: 17:51
 */
public class Main {

    private static final String USAGE="S1 XSD Utils\n" +
            "Usage:\n" +
            "\n" +
            "#XSD to ObjectSchema\n" +
            "s1-xsd-utils --to-objectschema --out-format=[json|options] source.xsd > dest\n" +
            "\n" +
            "#XSD to arrays list\n" +
            "s1-xsd-utils --to-arrayslist source.xsd > dest\n";

    public static void main(String[] args) throws Exception{
        if(args.length<1){
            System.out.println(USAGE);
            return;
        }
        String operation = args[0];
        if(!operation.startsWith("--")){
            System.out.println(USAGE);
            return;
        }
        operation=operation.substring(2);

        if(operation.equals("to-objectschema")){
            if(args.length!=3){
                System.out.println(USAGE);
                return;
            }
            String format = args[1].replace("--out-format=","").toLowerCase();
            if(!Objects.newArrayList("json","options").contains(format)){
                System.out.println(USAGE);
                return;
            }

            String source = args[2];

            Map<String,Object> m = XSD2ObjectSchema.toSchemaMap(XMLFormat.fromString(FileUtils.readFileToString(new File(source), "UTF-8")));

            if("json".equals(format)){
                System.out.println(JSONFormat.toJSON(m));
            }else if("options".equals(format)){
                System.out.println(Options.getStorage().formatMap(m));
            }
        }else if(operation.equals("to-arrayslist")){
            if(args.length!=2){
                System.out.println(USAGE);
                return;
            }

            String source = args[1];

            List<String> list = XSD2ArraysList.toArraysList(XMLFormat.fromString(FileUtils.readFileToString(new File(source), "UTF-8")));
            System.out.println(JSONFormat.toJSON(Objects.newHashMap(String.class,Object.class,"list",list)));
        }

    }

}
