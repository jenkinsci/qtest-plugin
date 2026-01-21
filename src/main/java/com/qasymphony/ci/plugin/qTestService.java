package com.qasymphony.ci.plugin;

import com.qasymphony.ci.plugin.model.qtest.Setting;
import com.qasymphony.ci.plugin.utils.ClientRequestException;
import com.qasymphony.ci.plugin.utils.HttpClientUtils;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.lang.StringUtils;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class qTestService {
    public static JSONObject getContainerInfo(String qTestUrl, Map<String, String> headers, Long projectID, String containerType, Long containerId)
    throws ClientRequestException
    {
        String type = "releases";
        switch (containerType.toLowerCase()) {
            case "release":
                break;
            case "test-suite":
                type = "test-suites";
                break;
            case "test-cycle":
                type = "test-cycles";
                break;
        }
        String url = String.format("%s/api/v3/projects/%s/%s/%s", qTestUrl, projectID, type, containerId);
        return JSONObject.fromObject(HttpClientUtils.get(url, headers).getBody());
    }

    public static JSONObject getProjectInfo(String qTestURL, Map<String, String> headers, Long projectID)
            throws ClientRequestException
    {
        String url = String.format("%s/api/v3/projects/%s", qTestURL, projectID);
        return JSONObject.fromObject(HttpClientUtils.get(url, headers).getBody());
    }

    public static JSONObject getQtestInfo(String qTestUrl) {
        JSONObject res = new JSONObject();
        //get project from qTest
        try {
            Object qTestInfo = ConfigService.getQtestInfo(qTestUrl);
            if (null != qTestInfo) {
                res.put("qTestInfo", JSONObject.fromObject(qTestInfo));
                return res;
            }
        } catch (Exception ex) {

        }
        return null;
    }

    public static JSONObject getContainerChildren(final  String qTestUrl, final String apiKey, final String secretKey, final Long projectId, final Long parentId, final String parentType) {
        final JSONObject res = new JSONObject();
        String token = null;
        try {
            token = OauthProvider.getAccessToken(qTestUrl, apiKey, secretKey);
        } catch (Exception e) {
           // LOG.log(Level.WARNING, "Error while get projectData:" + e.getMessage());
            e.printStackTrace();
        }
        final String accessToken = token;

        Object project = ConfigService.getProject(qTestUrl, accessToken, projectId);
        if (null == project) {
            //if project not found, we return empty data
            res.put("testCycles", "");
            res.put("testSuites", "");
            return res;
        }
        final int threadCount = 2;
        final CountDownLatch countDownLatch = new CountDownLatch(threadCount);
        ExecutorService fixedPool = Executors.newFixedThreadPool(threadCount);
        Callable<Object> caGetTestCycles = new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                try {
                    Object testCycles = ConfigService.getTestCycleChildren(qTestUrl, accessToken, projectId, parentId, parentType);
                    res.put("testCycles", null == testCycles ? "" : JSONArray.fromObject(testCycles));
                    return testCycles;
                } finally {
                    countDownLatch.countDown();
                }
            }
        };
        Callable<Object> caGetTestSuites = new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                try {
                    Object testSuites = ConfigService.getTestSuiteChildren(qTestUrl, accessToken, projectId, parentId, parentType);
                    res.put("testSuites", null == testSuites ? "" : JSONArray.fromObject(testSuites));
                    return testSuites;
                } finally {
                    countDownLatch.countDown();
                }
            }
        };
        fixedPool.submit(caGetTestCycles);
        fixedPool.submit(caGetTestSuites);

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            //LOG.log(Level.WARNING, e.getMessage());
            e.printStackTrace();
        } finally {
            fixedPool.shutdownNow();
            return res;
        }
    }

    public static JSONObject getProjectData(final String qTestUrl, final String apiKey, final String secretKey, final Long projectId, final String jenkinsProjectName, final String jenkinsServerName) {
        final JSONObject res = new JSONObject();
        String token = null;
        try {
            token = OauthProvider.getAccessToken(qTestUrl, apiKey, secretKey);
        } catch (Exception e) {
            //LOG.log(Level.WARNING, "Error while get projectData:" + e.getMessage());
            e.printStackTrace();
        }
        final String accessToken = token;

        Object project = ConfigService.getProject(qTestUrl, accessToken, projectId);
        if (null == project) {
            //if project not found, we return empty data
            res.put("setting", "");
            res.put("releases", "");
            res.put("environments", "");
            res.put("testCycles", "");
            res.put("testSuites", "");
            return res;
        }
        final int threadCount = StringUtils.isEmpty(jenkinsProjectName) ? 4 : 5;
        final CountDownLatch countDownLatch = new CountDownLatch(threadCount);
        ExecutorService fixedPool = Executors.newFixedThreadPool(threadCount);
        Callable<Object> caGetSetting = null;
        if (StringUtils.isNotEmpty(jenkinsProjectName)) {
            caGetSetting = new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    try {
                        //get saved setting from qtest
                        Object setting = ConfigService.getConfiguration(new Setting().setJenkinsServer(jenkinsServerName)
                                        .setJenkinsProjectName(jenkinsProjectName)
                                        .setProjectId(projectId)
                                        .setServerId(ConfigService.getServerId(jenkinsServerName)),
                                qTestUrl, accessToken);
                        res.put("setting", null == setting ? "" : JSONObject.fromObject(setting));
                        return setting;
                    } finally {
                        countDownLatch.countDown();
                    }
                }
            };
        }
        Callable<Object> caGetReleases = new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                try {
                    Object releases = ConfigService.getReleases(qTestUrl, accessToken, projectId);
                    res.put("releases", null == releases ? "" : JSONArray.fromObject(releases));
                    return releases;
                } finally {
                    countDownLatch.countDown();
                }
            }
        };
        Callable<Object> caGetTestCycles = new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                try {
                    Object testCycles = ConfigService.getTestCycleChildren(qTestUrl, accessToken, projectId, (long) 0, "root");
                    res.put("testCycles", null == testCycles ? "" : JSONArray.fromObject(testCycles));
                    return testCycles;
                } finally {
                    countDownLatch.countDown();
                }
            }
        };
        Callable<Object> caGetTestSuites = new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                try {
                    Object testSuites = ConfigService.getTestSuiteChildren(qTestUrl, accessToken, projectId, (long) 0, "root");
                    res.put("testSuites", null == testSuites ? "" : JSONArray.fromObject(testSuites));
                    return testSuites;
                } finally {
                    countDownLatch.countDown();
                }
            }
        };
        Callable<Object> caGetEnvs = new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                try {
                    Object environments = ConfigService.getEnvironments(qTestUrl, accessToken, projectId);
                    res.put("environments", null == environments ? "" : environments);
                    return environments;
                } finally {
                    countDownLatch.countDown();
                }
            }
        };
        if (null != caGetSetting) {
            fixedPool.submit(caGetSetting);
        }
        fixedPool.submit(caGetReleases);
        fixedPool.submit(caGetEnvs);
        fixedPool.submit(caGetTestCycles);
        fixedPool.submit(caGetTestSuites);

        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            //LOG.log(Level.WARNING, e.getMessage());
            e.printStackTrace();
        } finally {
            fixedPool.shutdownNow();
            return res;
        }
    }
}
