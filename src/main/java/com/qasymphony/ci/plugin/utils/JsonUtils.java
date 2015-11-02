package com.qasymphony.ci.plugin.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author trongle
 * @version 10/21/2015 2:24 PM trongle $
 * @since 1.0
 */
public class JsonUtils {
  private static final Logger LOG = Logger.getLogger(JsonUtils.class.getName());
  public static final String UTC_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZZ";
  /**
   * Use for JSON
   */
  private static final ObjectMapper mapper = new ObjectMapper();

  private JsonUtils() {

  }

  static {
    // mapper
    mapper.setDateFormat(new SimpleDateFormat(UTC_DATE_FORMAT));
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  }

  /**
   * Create new ObjectNode
   *
   * @return
   */
  public static ObjectNode newNode() {
    return mapper.createObjectNode();
  }

  /**
   * @return
   */
  public static String getCurrentDateString() {
    return new SimpleDateFormat(UTC_DATE_FORMAT).format(new Date());
  }

  /**
   * Get text by field in node
   *
   * @param node  the {@link JsonNode}
   * @param field the field that use to get in {@link JsonNode}
   * @return text in node if field exists in node, otherwise return empty string
   */
  public static String getText(JsonNode node, String field) {
    if (node.get(field) != null)
      return node.get(field).asText();
    return "";
  }

  /**
   * Get int value from JsonNode
   *
   * @param node
   * @param field
   * @return
   */
  public static int getInt(JsonNode node, String field) {
    if (node.get(field) != null)
      return node.asInt();
    return 0;
  }

  /**
   * parse a string value to JsonNode
   *
   * @param body
   * @return
   */
  public static JsonNode readTree(String body) {
    JsonNode node = null;
    if (StringUtils.isEmpty(body))
      return node;
    try {
      node = mapper.readTree(body);
    } catch (IOException e) {
      LOG.log(Level.WARNING, "readTree: Cannot readTree from body string.", e);
    }

    return node;
  }

  /**
   * Get long from jsonnode
   *
   * @param node
   * @param defaultValue
   * @return
   */
  public static Long getLong(JsonNode node, Long defaultValue) {
    if (null == node)
      return defaultValue;
    return node.asLong(0);
  }

  /**
   * Get instance of valueType from JSON data
   *
   * @param body      JSON string
   * @param valueType class type to cast
   * @return instance of class valueType
   */
  public static <T> T fromJson(String body, Class<T> valueType) {
    try {
      if (StringUtils.isEmpty(body))
        return null;
      return mapper.readValue(body, valueType);
    } catch (IOException e) {
      LOG.log(Level.WARNING, String.format("Cannot mapping from JSON to %s", valueType), e);
      return null;
    }
  }

  public static <T> T fromJson(String body, TypeReference<T> type) {
    try {
      if (StringUtils.isEmpty(body))
        return null;
      return mapper.readValue(body, type);
    } catch (IOException e) {
      LOG.log(Level.WARNING, String.format("Cannot mapping from JSON to %s", type), e);
      return null;
    }
  }

  /**
   * Get JsonNode from object append field extraData to JsonNode
   *
   * @return JsonNode, if data is a instance of POJO. Otherwise return empty string
   */
  public static JsonNode toJsonNode(Object data) {
    if (null == data)
      return newNode();

    ObjectNode node = null;

    try {
      // convert data to ObjectNode
      node = mapper.valueToTree(data);
    } catch (IllegalArgumentException e) {
      return newNode();
    }
    return node;
  }

  /**
   * serial object to JSON string
   *
   * @param data
   * @return
   */
  public static String toJson(Object data) {
    if (null == data)
      return "";
    try {
      return mapper.writeValueAsString(data);
    } catch (JsonProcessingException e) {
      LOG.log(Level.WARNING, "Cannot serial object to JSON.", e);
      return "";
    }
  }
}
