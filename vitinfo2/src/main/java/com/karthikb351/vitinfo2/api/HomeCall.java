package com.karthikb351.vitinfo2.api;

import com.google.gson.Gson;
import com.karthikb351.vitinfo2.api.Objects.AddFriendResponse;
import com.karthikb351.vitinfo2.api.Objects.Response;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by sreeram on 7/12/14.
 */
public class HomeCall {
    public static final String HOST_URL = "vitacademics-rel.herokuapp.com/api/";
    public static String json_response;

    public static Response sendRequest(String regno, String dob, String campus, String path) throws Exception{
        try{
            final String USER_AGENT = "Mozilla/5.0";
            final String base_host;
            base_host = HOST_URL + campus;

            final List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("regno", regno));
            params.add(new BasicNameValuePair("dob", dob));
            final URI uri = URIUtils.createURI("http", base_host, -1, path, URLEncodedUtils.format(params,"UTF-8"), null);
            HttpClient httpClient = new DefaultHttpClient();
            HttpGet request = new HttpGet();
            request.addHeader("User-Agent", USER_AGENT);
            request.setURI(uri);
            final HttpResponse httpResponse = httpClient.execute(request);
            final HttpEntity responseEntity = httpResponse.getEntity();
            json_response =  EntityUtils.toString(responseEntity);
            Gson gson = new Gson();
            return gson.fromJson(json_response, Response.class);
        }catch (Exception e){
            e.printStackTrace();
            throw new Exception("Looks like VIT servers are down!");
        }
    }

    public static AddFriendResponse getFriendTimeTable(String campus, String token) throws Exception{
        try{
            final String USER_AGENT = "Mozilla/5.0";
            final String base_host;
            String path = "/friends/share";
            base_host = HOST_URL + campus;

            final List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("token", token));
            final URI uri = URIUtils.createURI("http", base_host, -1, path, URLEncodedUtils.format(params,"UTF-8"), null);
            HttpClient httpClient = new DefaultHttpClient();
            HttpGet request = new HttpGet();
            request.addHeader("User-Agent", USER_AGENT);
            request.setURI(uri);
            final HttpResponse httpResponse = httpClient.execute(request);
            final HttpEntity responseEntity = httpResponse.getEntity();
            json_response =  EntityUtils.toString(responseEntity);
            Gson gson = new Gson();
            return gson.fromJson(json_response, AddFriendResponse.class);
        }catch (Exception e){
            e.printStackTrace();
            throw new Exception("Looks like VIT servers are down!");
        }
    }

    public static AddFriendResponse getFriendTimeTableCredentials(String campus, String regno, String dob) throws Exception{
        try{
            final String USER_AGENT = "Mozilla/5.0";
            final String base_host;
            String path = "/friends/share";
            base_host = HOST_URL + campus;

            final List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("regno", regno));
            params.add(new BasicNameValuePair("dob", dob));
            final URI uri = URIUtils.createURI("http", base_host, -1, path, URLEncodedUtils.format(params,"UTF-8"), null);
            HttpClient httpClient = new DefaultHttpClient();
            HttpGet request = new HttpGet();
            request.addHeader("User-Agent", USER_AGENT);
            request.setURI(uri);
            final HttpResponse httpResponse = httpClient.execute(request);
            final HttpEntity responseEntity = httpResponse.getEntity();
            json_response =  EntityUtils.toString(responseEntity);
            Gson gson = new Gson();
            return gson.fromJson(json_response, AddFriendResponse.class);
        }catch (Exception e){
            e.printStackTrace();
            throw new Exception("Looks like VIT servers are down!");
        }
    }
}
