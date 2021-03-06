package org.apdplat.demo.search;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GoogleSearcher {

    private static final Logger LOG = LoggerFactory.getLogger(GoogleSearcher.class);

    public static List<Webpage> searchGoogle(String url) {
        List<Webpage> webpages = new ArrayList<>();
        try {
            HttpClient httpClient = new HttpClient();
            GetMethod getMethod = new GetMethod(url);

            httpClient.executeMethod(getMethod);
            getMethod.getParams().setParameter(HttpMethodParams.RETRY_HANDLER,
                    new DefaultHttpMethodRetryHandler());

            int statusCode = httpClient.executeMethod(getMethod);
            if (statusCode != HttpStatus.SC_OK) {
                LOG.error("搜索失败: " + getMethod.getStatusLine());
                return null;
            }
            InputStream in = getMethod.getResponseBodyAsStream();
            byte[] responseBody = Tools.readAll(in);
            String response = new String(responseBody, "UTF-8");
            LOG.debug("搜索返回数据：" + response);
            JSONObject json = new JSONObject(response);
            String totalResult = json.getJSONObject("responseData").getJSONObject("cursor").getString("estimatedResultCount");
            int totalResultCount = Integer.parseInt(totalResult);
            LOG.info("搜索返回记录数： " + totalResultCount);

            JSONArray results = json.getJSONObject("responseData").getJSONArray("results");

            LOG.debug("搜索结果:");
            for (int i = 0; i < results.length(); i++) {
                Webpage webpage = new Webpage();
                JSONObject result = results.getJSONObject(i);
                //提取标题
                String title = result.getString("titleNoFormatting");
                LOG.debug("标题：" + title);
                webpage.setTitle(title);
                //提取摘要
                String summary = result.get("content").toString();
                summary = summary.replaceAll("<b>", "");
                summary = summary.replaceAll("</b>", "");
                summary = summary.replaceAll("\\.\\.\\.", "");
                LOG.debug("摘要：" + summary);
                webpage.setSummary(summary);
                //从URL中提取正文
                String _url = result.get("url").toString();
                webpage.setUrl(_url);
                String content = Tools.getHTMLContent(_url);
                LOG.debug("正文：" + content);
                webpage.setContent(content);
                webpages.add(webpage);
            }
        } catch (IOException | JSONException | NumberFormatException e) {
            LOG.error("执行搜索失败：", e);
        }
        return webpages;
    }

    public static void main(String args[]) {
        String query = "杨尚川";
        try {
            query = URLEncoder.encode(query, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            LOG.error("url构造失败", e);
            return;
        }
        String url = "http://ajax.googleapis.com/ajax/services/search/web?start=0&rsz=large&v=1.0&q=" + query;
        List<Webpage> webpages = searchGoogle(url);
        if (webpages != null) {
            int i = 1;
            for (Webpage webpage : webpages) {
                LOG.info("搜索结果 " + (i++) + " ：");
                LOG.info("标题：" + webpage.getTitle());
                LOG.info("URL：" + webpage.getUrl());
                LOG.info("摘要：" + webpage.getSummary());
                LOG.info("正文：" + webpage.getContent());
                LOG.info("");
            }
        } else {
            LOG.error("没有搜索到结果");
        }
    }
}