/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package cn.tzauto.octopus.common.ws;

import cn.tzauto.octopus.biz.recipe.domain.Attach;
import cn.tzauto.octopus.biz.recipe.domain.Recipe;
import cn.tzauto.octopus.biz.recipe.domain.RecipePara;
import cn.tzauto.octopus.common.util.tool.JsonMapper;
import java.util.List;
import java.util.Map;
import javax.swing.JOptionPane;
import org.apache.axis.client.Call;
import org.apache.axis.client.Service;

/**
 *
 * @author rain
 */
public class CallWebService {

    public static void main(String args[]) {

        try {
            String endpoint = "http://172.17.200.155:7011/autoServer/services/recipeService";
            Service service = new Service();
            Call call = (Call) service.createCall();
            call.setTargetEndpointAddress(new java.net.URL(endpoint));
            String deviceCode = "A1-MD-D38";
            String recipeName = "FB240X76.3 SE1";

            call.setOperationName("findLatestRecipe");
//            Map result = (Map) call.invoke(new Object[]{deviceCode, recipeName});
            Map map = CallWebService.getLatestRecipe(deviceCode, recipeName);
            if (map != null) {
                if (map.containsKey("recipe")) {
                    Recipe recipe = (Recipe) JsonMapper.fromJsonString(map.get("recipe").toString(), Recipe.class);
                    List<RecipePara> recipeParas = (List<RecipePara>) JsonMapper.String2List(map.get("recipePara").toString(), RecipePara.class);
                    Attach attach = (Attach) JsonMapper.fromJsonString(map.get("arAttach").toString(), Attach.class);
//                        recipeService.saveDownLoadRcpToDBAndFtp(deviceInfo, recipe, recipeParas, attach, deviceService, recipeService);
                    System.out.println(recipe.getId() + "::::" + recipeParas.size()+":::"+attach.getId());
                }
                if (map.containsKey("message")) {
                    JOptionPane.showMessageDialog(null, map.get("message").toString());
                    return;
                }
            }
//            for (Object v : result.values()) {
//                System.out.println("value= " + v);
//            }
        } catch (Exception e) {
            System.err.println(e.toString());
        }
        System.out.println("Finished the invoking.");
    }

    public static Map getLatestRecipe(String deviceCode, String recipeName) {
        try {
            String endpoint = "http://172.17.200.155:7011/autoServer/services/recipeService";
            Service service = new Service();
            Call call = (Call) service.createCall();
            call.setTargetEndpointAddress(new java.net.URL(endpoint));
            call.setOperationName("findLatestRecipe");
            Map result = (Map) call.invoke(new Object[]{deviceCode, recipeName});
//            for (Object v : result.values()) {
//                System.out.println("value= " + v);
//            }
//            System.out.println("Finished the invoking.");
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    public void callService_string() {
        try {
            String endpoint = "http://172.17.200.53:8080/autoServer/services/idGenService";
            Service service = new Service();
            Call call = (Call) service.createCall();
            call.setTargetEndpointAddress(new java.net.URL(endpoint));
            String sendMessage = "12";
            call.setOperationName("genId");
            String replyMeaage = String.valueOf(call.invoke(new Object[]{sendMessage}));
            System.out.println("Finished:\n" + replyMeaage);
        } catch (Exception e) {
            System.err.println(e.toString());
        }
        System.out.println("Finished the invoking.");
    }

    public void callService_list() {
        try {
            String endpoint = "http://172.17.200.53:8080/autoServer/services/idGenService";
            Service service = new Service();
            Call call = (Call) service.createCall();
            call.setTargetEndpointAddress(new java.net.URL(endpoint));
            String sendMessage = "12";
            call.setOperationName("genId");
            String replyMeaage = String.valueOf(call.invoke(new Object[]{sendMessage}));
            System.out.println("Finished:\n" + replyMeaage);
        } catch (Exception e) {
            System.err.println(e.toString());
        }
        System.out.println("Finished the invoking.");
    }
}
