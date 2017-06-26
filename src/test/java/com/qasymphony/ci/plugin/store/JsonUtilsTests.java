package com.qasymphony.ci.plugin.store;

import com.qasymphony.ci.plugin.utils.JsonUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.Date;

/**
 * @author trongle
 * @version $Id 5/22/2017 1:56 PM
 */
public class JsonUtilsTests {
  @Test
  public void testParseTimestamp() {

    String timestamp = "2017-06-22T18:12:39.123+05:00";
    Date date = JsonUtils.parseTimestamp(timestamp);
    Assert.assertNotNull(date);

    timestamp = "2017-06-22T18:12:39.123124+05:00";
    date = JsonUtils.parseTimestamp(timestamp);
    Assert.assertNotNull(date);

    timestamp = "2017-06-22T18:12:39+05:00";
    date = JsonUtils.parseTimestamp(timestamp);
    Assert.assertNotNull(date);

    timestamp = "2017-06-22T18:12:39";
    date = JsonUtils.parseTimestamp(timestamp);
    Assert.assertNotNull(date);

  }
}
