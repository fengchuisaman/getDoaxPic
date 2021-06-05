package utils;


import bean.Constant;
import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import org.apache.http.*;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLException;
import javax.net.ssl.SSLHandshakeException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Utils {
    private static PoolingHttpClientConnectionManager cm;

    public static String sendGet(String url, Map<String, String> headerMap, Map<String, String> paramsMap) {
        String returnValue = "";
        CloseableHttpResponse response = null;
        CloseableHttpClient httpClient = null;
        boolean isUserProxy = Boolean.parseBoolean(Constant.useProxy);
        try {
            httpClient = getClient(true);
            URIBuilder urlBuilder = new URIBuilder(url);
            if(paramsMap!=null && !paramsMap.isEmpty()){
                for (String key : paramsMap.keySet()) {
                    urlBuilder.setParameter(key, paramsMap.get(key));
                }
            }
            HttpGet httpGet = new HttpGet(urlBuilder.build());
            for (String key : headerMap.keySet()) {
                httpGet.setHeader(key, headerMap.get(key));
            }
            if(isUserProxy){
                HttpHost proxy = new HttpHost("127.0.0.1", 1081, "HTTP");
                RequestConfig config = RequestConfig.custom()
                        .setProxy(proxy)
                        .setConnectTimeout(50000)
                        .setSocketTimeout(50000)
                        .build();
                httpGet.setConfig(config);
                response = httpClient.execute(httpGet);
            }else{
                response = httpClient.execute(httpGet);
            }
            HttpEntity entity = response.getEntity();
            String respContent = EntityUtils.toString(entity, "UTF-8");
            returnValue = respContent;
            if (response.getStatusLine().getStatusCode() != 200) {
                returnValue = "请求出现问题 ----->"+ response.getStatusLine()+entity;
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            try {
                if(response!=null){
                    response.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                httpClient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return returnValue;
    }

    public static String sendPost(String url, boolean isForm, Map<String, String> headerMap, Map<String, String> paramsMap, List<? extends NameValuePair> paramsList) {
        String returnValue = null;
        CloseableHttpClient httpClient = getClient(true);
        CloseableHttpResponse response = null;
        boolean isUserProxy = Boolean.parseBoolean(Constant.useProxy);
        try {
            HttpPost httppost = new HttpPost(url);
            for (String key : headerMap.keySet()) {
                httppost.setHeader(key, headerMap.get(key));
            }
            // body json提交
            if (!isForm) {
                StringEntity entity = new StringEntity(new Gson().toJson(paramsMap), ContentType.APPLICATION_JSON);
                httppost.setEntity(entity);
            }
            //body 表单提交
            if (isForm) {
                UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(paramsList, "UTF-8");
                httppost.setEntity(formEntity);
            }
            if(isUserProxy){
                HttpHost proxy = new HttpHost("127.0.0.1", 1082, "HTTP");
                RequestConfig config = RequestConfig.custom()
                        .setProxy(proxy)
                        .setConnectTimeout(50000)
                        .setSocketTimeout(50000)
                        .build();
                httppost.setConfig(config);
                response = httpClient.execute(proxy,httppost);
            }else{
                response = httpClient.execute(httppost);
            }
            if (response.getStatusLine().getStatusCode() == 200) {
                response.setHeader("Content-Type","application/json");
                HttpEntity entity = response.getEntity();
                String respContent = EntityUtils.toString(entity, "UTF-8");
                returnValue = respContent;
            }
        } catch (SocketTimeoutException e) {
            e.printStackTrace();
            returnValue = "timeout";
        } catch (Exception e) {
            System.out.println("请求失败，请求错误码："+response.getStatusLine().getStatusCode()+"  错误内容："+response.getStatusLine());
            e.printStackTrace();
        } finally {
            // 关闭连接,释放资源
            try {
                response.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                httpClient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return returnValue;
    }


    public static String downloadUseHttpClient(Map<String, String> headerMap,String urlPath, String dirPath, String fileName) throws IOException {
        String filePath = dirPath + fileName;
        String currOS = System.getProperty("os.name").toLowerCase();
        //如果文件夹不存在，创建
        if(!currOS.contains("windows")){
            dirPath = dirPath.replace("\\","/");
        }
        File dirFile = new File(dirPath);
        if (!dirFile.exists()) {
            dirFile.mkdirs();
        }

        if(!currOS.contains("windows")){
            filePath=filePath.replace("\\","/");
        }
        File file = new File(filePath);
        //默认创建 httpClient
//        CloseableHttpClient httpClient = HttpClients.createDefault();
        CloseableHttpClient httpClient = getClient(true);
        CloseableHttpResponse response ;
        long begin = 0;
        InputStream is = null;
        try {
            URIBuilder uriBuilder = new URIBuilder(urlPath);

            HttpGet httpGet = new HttpGet(uriBuilder.build());
            for(String headerKey: headerMap.keySet()){
                httpGet.addHeader(headerKey, headerMap.get(headerKey));
            }
            if(Boolean.valueOf(Constant.useProxyForDownLoad)){
                HttpHost proxy = new HttpHost(Constant.ProxyHost, Constant.ProxyPort, "HTTP");
                RequestConfig config = RequestConfig.custom()
                        .setProxy(proxy)
                        .setConnectTimeout(50000)
                        .setSocketTimeout(50000)
                        .build();
                httpGet.setConfig(config);
                response = httpClient.execute(httpGet);
            }else{
                response = httpClient.execute(httpGet);
            }
            HttpEntity entity = response.getEntity();
            if (file.exists()) {
                if (entity.getContentLength() > file.length()) {
                    System.out.println("发现重复文件：   " + fileName + "，且文件损坏，删除文件，重新下载");
                    file.delete();
                } else {
                    System.out.println("发现重复文件：   " + fileName);
                    return filePath;
                }
            }
            is = entity.getContent();
            begin = System.currentTimeMillis();
            FileUtils.copyInputStreamToFile(is,file);
        } catch (Exception e) {
            // 如果发生Connection reset 就输出到错误信息到文件中
            if(e.getMessage().contains("Connection reset")){
//                e.printStackTrace();
                System.out.println("下载"+filePath+"发生异常："+e.getMessage());
//                File file1 = new File(filePath);
//                String txtContent = "在"+new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())+" 下载图片"+fileName+"出现异常\n，异常："+e.getMessage()+"\n图片地址是:"+urlPath;
//                FileUtils.write(file1,txtContent,false);
                downloadUseHttpClient( headerMap,urlPath, dirPath,fileName);
            }
        } finally {
            if (is != null) {
                is.close();

            }
        }
        long end = System.currentTimeMillis();
        System.out.println("时间" + LocalDateTime.now() + "文件已下载：" + dirPath + fileName + "  共耗时：" + (end - begin) / 1000.0 + "秒");
        return filePath;
    }

    /**
     * 设置请求头，放入的map value值为
     * @param map
     * @return
     */
    public static HashMap<String,String> getHeader(HashMap<String,String> map) {
        HashMap headerMap = new HashMap(6);
        headerMap.put("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:88.0) Gecko/20100101 Firefox/88.0");
        if(map!=null&&!map.isEmpty()){
            for(String key :map.keySet()){
                if(map.get(key)==null){
                    headerMap.remove(key);
                }else{
                    headerMap.put(key,map.get(key));
                }
            }
        }
        return headerMap;
    }









    /**
     * @param isPooled 是否使用连接池
     */
    public static CloseableHttpClient getClient(boolean isPooled) {
        cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(1000);
        cm.setDefaultMaxPerRoute(1000);
        HttpRequestRetryHandler handler = new HttpRequestRetryHandler() {
            @Override
            public boolean retryRequest(IOException arg0, int retryTimes, HttpContext arg2) {
                if (retryTimes > 2) {
                    return false;
                }
                if (arg0 instanceof UnknownHostException || arg0 instanceof ConnectTimeoutException
                        || !(arg0 instanceof SSLException) || arg0 instanceof NoHttpResponseException
                        || arg0 instanceof SSLHandshakeException || arg0 instanceof SSLHandshakeException) {
                    return true;
                }

                HttpClientContext clientContext = HttpClientContext.adapt(arg2);
                HttpRequest request = clientContext.getRequest();
                boolean idempotent = !(request instanceof HttpEntityEnclosingRequest);
                if (idempotent) {
                    // 如果请求被认为是幂等的，那么就重试。即重复执行不影响程序其他效果的
                    return true;
                }
                return false;
            }
        };


        if (isPooled) {
            return HttpClients.custom().setConnectionManager(cm).setRetryHandler(handler).build();
        }
        return HttpClients.createDefault();
    }


}
