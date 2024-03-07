package io.cresco.aiapi;

import io.cresco.library.messaging.MsgEvent;
import io.cresco.library.plugin.Executor;
import io.cresco.library.plugin.PluginBuilder;
import io.cresco.library.utilities.CLogger;

import java.util.*;

public class PluginExecutor implements Executor {

    private PluginBuilder plugin;
    private CLogger logger;

    private HttpUtils httpUtils;

    public PluginExecutor(PluginBuilder pluginBuilder) {
        this.plugin = pluginBuilder;
        logger = plugin.getLogger(PluginExecutor.class.getName(),CLogger.Level.Info);
        httpUtils = new HttpUtils(plugin);
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

        switch (ce.getParam("action")) {

            case "repolist":
                return repoList(ce);
            case "getllm":
                return getLlm(ce);
            case "getllmgenerate":
                return getLlmGenerate(ce);

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

        if(!msg.paramsContains("endpoint_url")) {
            if(plugin.getConfig().getStringParam("endpoint_url") != null) {
                msg.setParam("endpoint_url", plugin.getConfig().getStringParam("endpoint_url"));
            } else {
                msg.setParam("status_code","9");
                msg.setParam("status_desc","Url_endpoint is null");
            }
        }

        if(msg.paramsContains("endpoint_url")) {

            msg = httpUtils.getLlmResponse(msg);

        }

        return msg;

    }

    private MsgEvent getLlmGenerate(MsgEvent msg) {

        if(!msg.paramsContains("endpoint_url")) {
            if(plugin.getConfig().getStringParam("endpoint_url") != null) {
                msg.setParam("endpoint_url", plugin.getConfig().getStringParam("endpoint_url"));
            }
        }

        if((msg.paramsContains("endpoint_url") && (msg.paramsContains("endpoint_payload")))) {

            msg = httpUtils.getLlmGenerate(msg);

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

}