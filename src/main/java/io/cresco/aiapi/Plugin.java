package io.cresco.aiapi;

import io.cresco.library.agent.AgentService;
import io.cresco.library.messaging.MsgEvent;
import io.cresco.library.plugin.Executor;
import io.cresco.library.plugin.PluginBuilder;
import io.cresco.library.plugin.PluginService;
import io.cresco.library.utilities.CLogger;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.annotations.*;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Map;


@Component(
        service = { PluginService.class },
        scope=ServiceScope.PROTOTYPE,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property="aiapi=core",
        reference= { @Reference(name="io.cresco.library.agent.AgentService", service=AgentService.class)}
)

public class Plugin implements PluginService {

    //public PluginBuilder getPluginBuilder() { return  pluginBuilder; }

    public BundleContext context;
    public static PluginBuilder pluginBuilder;
    private Executor executor;
    private CLogger logger;
    //private HttpService server;
    public String repoPath = null;
    private ConfigurationAdmin configurationAdmin;
    private Map<String,Object> map;

    private  AIClientEngine aiClientEngine;

    @Activate
    void activate(BundleContext context, Map<String,Object> map) {

        this.context = context;
        this.map = map;
    }

    @Reference
    protected void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }

    protected void unsetConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = null;
    }


    @Modified
    void modified(BundleContext context, Map<String,Object> map) {
        System.out.println("Modified Config Map PluginID:" + (String) map.get("pluginID"));
    }

    @Deactivate
    void deactivate(BundleContext context, Map<String,Object> map) {

        isStopped();

        if(this.context != null) {
            this.context = null;
        }

        if(this.map != null) {
            this.map = null;
        }

    }

    @Override
    public boolean isActive() {
        return pluginBuilder.isActive();
    }

    @Override
    public void setIsActive(boolean isActive) {
        pluginBuilder.setIsActive(isActive);
    }

    @Override
    public boolean inMsg(MsgEvent incoming) {
        pluginBuilder.msgIn(incoming);
        return true;
    }

    private Dictionary<String, String> getJerseyServletParams() {
        Dictionary<String, String> jerseyServletParams = new Hashtable<>();
        jerseyServletParams.put("javax.ws.rs.Application", Plugin.class.getName());
        return jerseyServletParams;
    }


    @Override
    public boolean isStarted() {
        try {

            if(pluginBuilder == null) {
                pluginBuilder = new PluginBuilder(this.getClass().getName(), context, map);
                this.logger = pluginBuilder.getLogger(Plugin.class.getName(), CLogger.Level.Info);
                aiClientEngine = new AIClientEngine(pluginBuilder);
                this.executor = new PluginExecutor(pluginBuilder, aiClientEngine);
                pluginBuilder.setExecutor(executor);

                while (!pluginBuilder.getAgentService().getAgentState().isActive()) {
                    logger.info("Plugin " + pluginBuilder.getPluginID() + " waiting on Agent Init");
                    //System.out.println("Plugin " + pluginBuilder.getPluginID() + " waiting on Agent Init");
                    Thread.sleep(1000);
                }

                pluginBuilder.setIsActive(true);
                logger.info("AIAPI Active");

            }
            return true;

        } catch(Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean isStopped() {


        if(pluginBuilder != null) {
            pluginBuilder.setExecutor(null);
            pluginBuilder.setIsActive(false);
            if(aiClientEngine != null) {
                aiClientEngine.shutdown();
            }
        }
        return true;
    }

}