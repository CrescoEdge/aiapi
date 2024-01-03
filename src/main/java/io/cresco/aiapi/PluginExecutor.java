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
        httpUtils = new HttpUtils();
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

        String url = msg.getParam("url");
        String inputText = msg.getParam("input_text");
        int maxTokens = Integer.parseInt(msg.getParam("max_tokens"));
        String response = httpUtils.getLlmResponse(url, inputText, maxTokens);
        msg.setParam("outout_text", response);
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