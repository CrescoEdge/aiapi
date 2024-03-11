package io.cresco.aiapi;

import io.cresco.library.messaging.MsgEvent;
import io.cresco.library.plugin.Executor;
import io.cresco.library.plugin.PluginBuilder;
import io.cresco.library.utilities.CLogger;
import org.eclipse.jetty.util.IO;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
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

    private MsgEvent getLlmAdapter(MsgEvent msg) {

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
            getObjectBytes(accessKey, secretKey, urlString, bucketName, keyName, path);
            logger.error("GET ADAPTER 3");

        } else {
            logger.error("GET ADAPTER 4");
            String missingListString = String.join(", ", missingParamList);
            msg.setParam("status_code","9");
            msg.setParam("status_desc","missing parameters: " + missingListString);
        }

        return msg;

    }

    public void getObjectBytes(String accessKey, String secretKey, String urlString, String bucketName, String keyName, String path) {

        try {
            logger.error("GET ADAPTER 2.1");
            AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey,secretKey);
            AwsCredentialsProvider provider = StaticCredentialsProvider.create(credentials);
            logger.error("GET ADAPTER 2.2");
            URI myURI = new URI(urlString);

            Region region = Region.US_EAST_1;
            logger.error("GET ADAPTER 2.3");
            S3Client s3 = S3Client.builder()
                    .credentialsProvider(provider)
                    .region(region)
                    .endpointOverride(myURI)
                    .forcePathStyle(true) // <-- this fixes runing localhost
                    .build();
            logger.error("GET ADAPTER 2.4");
            GetObjectRequest objectRequest = GetObjectRequest
                    .builder()
                    .key(keyName)
                    .bucket(bucketName)
                    .build();
            logger.error("GET ADAPTER 2.5");
            ResponseBytes<GetObjectResponse> objectBytes = s3.getObjectAsBytes(objectRequest);
            logger.error("GET ADAPTER 2.6");
            InputStream inputStream = objectBytes.asInputStream();
            OutputStream outputStream = Files.newOutputStream(Paths.get(path));
            logger.error("GET ADAPTER 2.7");
            IO.copy(inputStream, outputStream);
            logger.error("GET ADAPTER 2.8");
            s3.close();
            logger.error("GET ADAPTER 2.9");


        } catch (IOException ex) {
            logger.error("getObjectBytes: " + ex.getMessage());
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            ex.printStackTrace(pw);
            logger.error(sw.toString());
        } catch (S3Exception e) {
            logger.error("getObjectBytes: " + e.getMessage());
            //System.err.println(e.awsErrorDetails().errorMessage());
            //System.exit(1);
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            logger.error(sw.toString());
        } catch (Exception exc) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            exc.printStackTrace(pw);
            logger.error(sw.toString());
        }
    }

}