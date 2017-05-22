package com.qasymphony.ci.plugin.store;

import com.qasymphony.ci.plugin.utils.HttpClientUtils;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author trongle
 * @version $Id 5/22/2017 1:56 PM
 */
public class HttpClientUtilsTests {
  @Test
  public void testGetServerUrl() {
    int serverPort = 443;
    String scheme = "https";
    String serverName = "localhost";
    String contextPath = "/jenkins";
    String url = HttpClientUtils.getServerUrl(serverPort, scheme, serverName, contextPath);
    String expected = "https://localhost/jenkins";
    Assert.assertEquals("Url is " + expected, expected, url);

    serverPort = 80;
    scheme = "http";
    serverName = "localhost";
    contextPath = "/jenkins";
    url = HttpClientUtils.getServerUrl(serverPort, scheme, serverName, contextPath);
    expected = "http://localhost/jenkins";
    Assert.assertEquals("Url is " + expected, expected, url);

    serverPort = 8080;
    scheme = "http";
    serverName = "localhost";
    contextPath = "/jenkins";
    url = HttpClientUtils.getServerUrl(serverPort, scheme, serverName, contextPath);
    expected = "http://localhost:8080/jenkins";
    Assert.assertEquals("Url is " + expected, expected, url);


    serverPort = 8443;
    scheme = "https";
    serverName = "localhost";
    contextPath = "/jenkins";
    url = HttpClientUtils.getServerUrl(serverPort, scheme, serverName, contextPath);
    expected = "https://localhost:8443/jenkins";
    Assert.assertEquals("Url is " + expected, expected, url);
  }
}
