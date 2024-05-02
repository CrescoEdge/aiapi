package io.cresco.aiapi;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.cresco.library.data.TopicType;
import io.cresco.library.messaging.MsgEvent;
import io.cresco.library.plugin.PluginBuilder;
import io.cresco.library.utilities.CLogger;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpHeader;

import javax.jms.TextMessage;
import java.lang.reflect.Type;
import java.util.*;

public class AIClientEngine {

    private PluginBuilder plugin;
    private final Gson gson;
    private CLogger logger;

    private Timer serviceBroadcastTimer;

    private final Type mapType = new TypeToken<Map<String, Object>>() {}.getType();

    Type typeOfListMap = new TypeToken<List<Map<String,String>>>(){}.getType();

    public AIClientEngine(PluginBuilder plugin) {
        this.plugin = plugin;
        logger = plugin.getLogger(PluginExecutor.class.getName(),CLogger.Level.Info);
        gson = new Gson();
        startServiceBroadcast();
    }

    public void subMessage(String serviceMapString) {

        try {

            TextMessage updateMessage = plugin.getAgentService().getDataPlaneService().createTextMessage();
            updateMessage.setText(serviceMapString);
            updateMessage.setStringProperty(plugin.getConfig().getStringParam("ident_key"), plugin.getConfig().getStringParam("ident_id"));
            plugin.getAgentService().getDataPlaneService().sendMessage(TopicType.AGENT,updateMessage);

        } catch (Exception ex) {
            logger.error("failed to update subscribers");
            logger.error(ex.getMessage());
        }

    }

    public void startServiceBroadcast() {

        long delay =  plugin.getConfig().getLongParam("broadcast_delay", 5000L);
        long period =  plugin.getConfig().getLongParam("broadcast_period", 15000L);

        //create timer task
        TimerTask repoBroadcastTask = new TimerTask() {
            public void run() {
                try {

                    if(plugin.isActive()) {
                        String serviceMapString = getServiceMapString();
                        subMessage(serviceMapString);
                        //logger.info("SUBMITTING: " + serviceMapString);

                    } else {
                        logger.error("NOT ACTIVE");
                    }

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };

        serviceBroadcastTimer = new Timer("BroadCastTimer");
        serviceBroadcastTimer.scheduleAtFixedRate(repoBroadcastTask, delay, period);
        logger.debug("broadcasttimer : set : " + period);
    }

    public MsgEvent getLlmResponse(MsgEvent msg) {

        try {

            String url = msg.getParam("endpoint_url_chat");
            String inputText = msg.getParam("input_text");
            String adapterId = null;
            if(msg.paramsContains("adapter_id")) {
                adapterId = msg.getParam("adapter_id");
            }
            int maxTokens = Integer.parseInt(msg.getParam("max_tokens"));

            String inputString = "[INST] " + inputText + " [/INST]";
            String requestString = null;
            if(adapterId != null) {
                requestString = "{\"inputs\":\"" + inputString + "\",\"parameters\":{\"max_new_tokens\":" + maxTokens +
                        ", \"adapter_id\":\"" + adapterId + "\", \"adapter_source\":\"local\"}}";
            } else {
                requestString = "{\"inputs\":\"" + inputString + "\",\"parameters\":{\"max_new_tokens\":" + maxTokens + "}}";
            }
            logger.error("requesting String: " + requestString);

            HttpClient client = new HttpClient();
            client.setFollowRedirects(false);
            client.start();

            Request request = client.POST(url);
            request.header(HttpHeader.CONTENT_TYPE, "application/json");

            request.content(new StringContentProvider(requestString,"utf-8"));
            ContentResponse response = request.send();
            String contentString = response.getContentAsString();
            logger.error("content: " + contentString);
            List<Map<String,String>> lmresponse = gson.fromJson(contentString, typeOfListMap);
            String llmResponse = String.valueOf(lmresponse.get(0).get("generated_text"));
            int responseStatus = response.getStatus();

            client.stop();

            msg.setParam("status_code","10");
            msg.setParam("status_desc","Query executed properly");
            msg.setParam("response_status_code", String.valueOf(responseStatus));
            msg.setParam("output_text", llmResponse);


        } catch (Exception e) {
            e.printStackTrace();
        }
        return msg;
    }

    public MsgEvent getLlmGenerate(MsgEvent msg) {

        try {

            String url = msg.getParam("endpoint_url_chat");
            String requestString = msg.getParam("endpoint_payload");

            logger.error("URL: " + url);
            logger.error("Payload: " + requestString);

            HttpClient client = new HttpClient();
            client.setFollowRedirects(false);
            client.start();

            Request request = client.POST(url);
            request.header(HttpHeader.CONTENT_TYPE, "application/json");

            request.content(new StringContentProvider(requestString,"utf-8"));
            ContentResponse response = request.send();
            String contentString = response.getContentAsString();
            logger.error("content: " + contentString);
            //List<Map<String,String>> lmresponse = gson.fromJson(contentString, typeOfListMap);
            //String llmResponse = String.valueOf(lmresponse.get(0).get("generated_text"));
            int responseStatus = response.getStatus();

            client.stop();

            msg.setParam("status_code","10");
            msg.setParam("status_desc","Query executed properly");
            msg.setParam("response_status_code", String.valueOf(responseStatus));
            //msg.setParam("llm_response", contentString);
            msg.setCompressedParam("llm_response_compressed", contentString);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return msg;
    }

    private String remoteLeadingSlash(String str) {
        char ch = '/';
        if (str.charAt(str.length() - 1) == ch) {
            // Remove the last character of the string
            str = str.substring(0, str.length() - 1);
        }
        return str;
    }

    private String getServiceMapString() {
        String serviceMapString = null;
        try {

            Map<String,Map<String,Object>> serviceMap = new HashMap<>();

            serviceMap.put("agent", new HashMap<>());
            serviceMap.get("agent").put("region_id",plugin.getRegion());
            serviceMap.get("agent").put("agent_id",plugin.getAgent());
            serviceMap.get("agent").put("plugin_id",plugin.getPluginID());

            if(plugin.getConfig().getStringParam("endpoint_url_chat") != null) {
                String chatUrl = remoteLeadingSlash(plugin.getConfig().getStringParam("endpoint_url_chat")) + "/info";

                Map<String,Object> chatResponseMap = getInfo(chatUrl);
                serviceMap.put("chat", new HashMap<>());
                serviceMap.get("chat").put("info", chatResponseMap);
            }

            if(plugin.getConfig().getStringParam("endpoint_url_emb") != null) {
                String embUrl = remoteLeadingSlash(plugin.getConfig().getStringParam("endpoint_url_emb")) + "/info";

                Map<String,Object> embResponseMap = getInfo(embUrl);
                serviceMap.put("emb", new HashMap<>());
                serviceMap.get("emb").put("info", embResponseMap);

            }

            serviceMapString = gson.toJson(serviceMap);

        } catch (Exception ex) {
            logger.error("getServiceMap: " + ex.getMessage());
        }
        return serviceMapString;
    }


    private Map<String,Object> getInfo(String url) {

        Map<String,Object> responseMap = null;
        try {
            HttpClient client = new HttpClient();
            client.setFollowRedirects(false);
            client.start();
            ContentResponse response = client.GET(url);
            client.stop();

            String contentString = response.getContentAsString();
            int responseStatus = response.getStatus();

            if(responseStatus == 200) {
                if(contentString != null) {
                    responseMap = gson.fromJson(contentString, mapType);
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return responseMap;
    }

    public void shutdown(){
        serviceBroadcastTimer.cancel();
    }

}
