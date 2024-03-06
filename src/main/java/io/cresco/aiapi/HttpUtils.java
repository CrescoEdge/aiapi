package io.cresco.aiapi;

import com.google.gson.Gson;
import io.cresco.library.messaging.MsgEvent;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpHeader;

import java.util.Map;

public class HttpUtils {

    private final Gson gson;
    public HttpUtils() {
        gson = new Gson();
    }

    public MsgEvent getLlmResponse(MsgEvent msg) {

        try {

            String url = msg.getParam("endpoint_url");
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
                        ", \"adapter_id\":" + adapterId + ", \"adapter_source\":\"local\"}}";
            } else {
                requestString = "{\"inputs\":\"" + inputString + "\",\"parameters\":{\"max_new_tokens\":" + maxTokens + "}}";
            }

            HttpClient client = new HttpClient();
            client.setFollowRedirects(false);
            client.start();
            
            Request request = client.POST(url);
            request.header(HttpHeader.CONTENT_TYPE, "application/json");

            request.content(new StringContentProvider(requestString,"utf-8"));
            ContentResponse response = request.send();

            String llmResponse = String.valueOf(gson.fromJson(response.getContentAsString(), Map.class).get("generated_text"));
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

}
