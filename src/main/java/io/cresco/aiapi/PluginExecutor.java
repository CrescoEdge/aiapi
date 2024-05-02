package io.cresco.aiapi;

import io.cresco.library.messaging.MsgEvent;
import io.cresco.library.plugin.Executor;
import io.cresco.library.plugin.PluginBuilder;
import io.cresco.library.utilities.CLogger;
import java.util.*;

public class PluginExecutor implements Executor {

    private PluginBuilder plugin;
    private CLogger logger;

    private AIClientEngine aiClientEngine;

    public PluginExecutor(PluginBuilder pluginBuilder, AIClientEngine aiClientEngine) {
        this.plugin = pluginBuilder;
        logger = plugin.getLogger(PluginExecutor.class.getName(),CLogger.Level.Info);
        this.aiClientEngine = aiClientEngine;
    }

    @Override
    public MsgEvent executeCONFIG(MsgEvent incoming) {
        return null;
    }
    @Override
    public MsgEvent executeDISCOVER(MsgEvent incoming) {
        return null;
    }
    @Override
    public MsgEvent executeERROR(MsgEvent incoming) {
        return null;
    }
    @Override
    public MsgEvent executeINFO(MsgEvent incoming) {
        return null;
    }
    @Override
    public MsgEvent executeEXEC(MsgEvent ce) {
        logger.error("EXEC: " + ce.getParam("action"));
        switch (ce.getParam("action")) {

            case "repolist":
                return repoList(ce);
            case "getllm":
                return getLlm(ce);
            case "getllmgenerate":
                return getLlmGenerate(ce);
            case "getllmadapter":
                return getLlmAdapter(ce);

            default:
                logger.error("Unknown configtype found {} for {}:", ce.getParam("action"), ce.getMsgType().toString());

        }

        return null;
    }
    @Override
    public MsgEvent executeWATCHDOG(MsgEvent incoming) {
        return null;
    }
    @Override
    public MsgEvent executeKPI(MsgEvent incoming) {
        return null;
    }

    private MsgEvent getLlm(MsgEvent msg) {

        if(!msg.paramsContains("endpoint_url_chat")) {
            if(plugin.getConfig().getStringParam("endpoint_url_chat") != null) {
                msg.setParam("endpoint_url_chat", plugin.getConfig().getStringParam("endpoint_url"));
            } else {
                msg.setParam("status_code","9");
                msg.setParam("status_desc","Url_endpoint is null");
            }
        }

        if(msg.paramsContains("endpoint_url_chat")) {

            msg = aiClientEngine.getLlmResponse(msg);

        }

        return msg;

    }

    private MsgEvent getLlmGenerate(MsgEvent msg) {

        if(!msg.paramsContains("endpoint_url_chat")) {
            if(plugin.getConfig().getStringParam("endpoint_url_chat") != null) {
                msg.setParam("endpoint_url_chat", plugin.getConfig().getStringParam("endpoint_url"));
            }
        }

        if((msg.paramsContains("endpoint_url_chat") && (msg.paramsContains("endpoint_payload")))) {

            msg = aiClientEngine.getLlmGenerate(msg);

        } else {
            msg.setParam("status_code","9");
            msg.setParam("status_desc","Url_endpoint or endpoint_payload is null");
        }

        return msg;

    }


    private MsgEvent repoList(MsgEvent msg) {
        //todo fix repo list
        Map<String,List<Map<String,String>>> repoMap = new HashMap<>();
        //repoMap.put("plugins",getPluginInventory(mainPlugin.repoPath));

        //List<Map<String,String>> contactMap = getNetworkAddresses();
        //repoMap.put("server",contactMap);

        //msg.setCompressedParam("repolist",gson.toJson(repoMap));
        return msg;

    }

    private MsgEvent getLlmAdapter(MsgEvent msg) {
        //endpoint_url_chat_adapter_path

        logger.error("GET ADAPTER");

        List<String> paramList = new ArrayList<>();
        paramList.add("s3_access_key");
        paramList.add("s3_secret_key");
        paramList.add("s3_url");
        paramList.add("s3_bucket");
        paramList.add("s3_key");
        paramList.add("local_path");

        List<String> missingParamList = new ArrayList<>(paramList);

        for(String param : paramList) {
            if(msg.paramsContains(param)) {
                missingParamList.remove(param);
            }
        }

        if(missingParamList.isEmpty()) {
            logger.error("GET ADAPTER 1");
            //all params are here
            String accessKey = msg.getParam("s3_access_key");
            String secretKey = msg.getParam("s3_secret_key");
            String urlString = msg.getParam("s3_url");
            String bucketName = msg.getParam("s3_bucket");
            String keyName = msg.getParam("s3_key");
            String path = msg.getParam("local_path");
            logger.error("GET ADAPTER 2");
            //getObjectBytes(accessKey, secretKey, urlString, bucketName, keyName, path);
            logger.error("GET ADAPTER 3");

        } else {
            logger.error("GET ADAPTER 4");
            String missingListString = String.join(", ", missingParamList);
            msg.setParam("status_code","9");
            msg.setParam("status_desc","missing parameters: " + missingListString);
        }

        return msg;

    }



}