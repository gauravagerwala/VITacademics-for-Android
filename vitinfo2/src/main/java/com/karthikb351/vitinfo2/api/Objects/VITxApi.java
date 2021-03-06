package com.karthikb351.vitinfo2.api.Objects;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;

import com.karthikb351.vitinfo2.api.HomeCall;
import com.karthikb351.vitinfo2.objects.DataHandler;
import com.koushikdutta.ion.Ion;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.io.File;

/**
 * Created by saurabh on 14-12-09.
 */
public class VITxApi {
    private static VITxApi mInstance;

    private Context context;

    private String regno;
    private String dob;
    private String campus;

    public interface onTaskCompleted{ public void onCompleted(Object result, Exception e); }

    public VITxApi(Context context){
        this.context = context;
        refreshCredentials();
    }

    public static VITxApi getInstance(Context context){
        if(mInstance == null)
            mInstance = new VITxApi(context);
        return mInstance;
    }

    public void addFriendWithCredentials(final String regno, final String dob, final onTaskCompleted listener){
        refreshCredentials();

        class bgTask extends AsyncTask<Void, Void, Object>{
            private Exception e;
            @Override
            protected Object doInBackground(Void... params) {
                try{
                    if(!isConnected())
                        throw new Exception("No network connection!");

                    AddFriendResponse temp = HomeCall.getFriendTimeTableCredentials(campus, regno, dob);
                    if(temp.getStatus().getCode() == 0){
                        Data data = temp.getData();

                        ParseQuery<ParseUser> query = ParseUser.getQuery();
                        ParseUser u = (query.whereEqualTo("username",data.getRegNo())).getFirst();

                        //Is logged in with Facebook
                        if(u.getString("isSignedIn").equals("true")){
                            data.isFb = true;
                            data.fbId = u.getString("facebookID");
                            data.title = u.getString("facebookName");

                            Ion.with(context)
                                    .load("http://graph.facebook.com/" + data.fbId + "/picture?type=large")
                                    .write(new File(context.getCacheDir().getPath() + "/" + data.fbId + ".jpg"))
                                    .get();
                        }
                        else{
                            data.title = data.getRegNo();
                        }

                        temp.setData(data);
                        DataHandler.getInstance(context).addFriend(temp);
                        return DataHandler.getInstance(context).getToken();

                    }
                    else
                        throw new Exception(temp.getStatus().getMessage());
                }catch (Exception e){
                    e.printStackTrace();
                    this.e = e;
                }
                return null;
            }

            @Override
            protected void onPostExecute(Object o) {
                super.onPostExecute(o);
                listener.onCompleted(o, e);
            }
        }
        new bgTask().execute();
    }

    public void saveServerStatus(final onTaskCompleted listener){

        class bgTask extends AsyncTask<Void, Void, Object>{
            private Exception e;
            boolean ret = false;
            @Override
            protected Object doInBackground(Void... params) {
                try {
                    HttpClient client = new DefaultHttpClient();
                    HttpGet request = new HttpGet("http://vitacademicsrel.appspot.com/status");
                    String res = EntityUtils.toString(client.execute(request).getEntity());
                    JSONObject obj = new JSONObject(res);

                    int ver = Integer.parseInt(obj.getString("msg_no"));
                    String saved = DataHandler.getInstance(context).getServerStatus();

                    if(!saved.equals("")){
                        int sv = Integer.parseInt(new JSONObject(saved).getString("msg_no"));
                        if(sv != ver)
                            ret =  true;
                    }
                    else
                        ret = true;

                    if(ret)
                        DataHandler.getInstance(context).saveServerStatus(res);

                }catch (Exception e){this.e = e;}
                return null;
            }
            @Override
            protected void onPostExecute(Object o) {
                super.onPostExecute(o);
                listener.onCompleted(ret, e);
            }
        }
        new bgTask().execute();
    }

    public void addFriend(final String token, final onTaskCompleted listener){
        refreshCredentials();

        class bgTask extends AsyncTask<Void, Void, Object>{
            private Exception e;
            @Override
            protected Object doInBackground(Void... params) {
                try{
                    if(!isConnected())
                        throw new Exception("No network connection!");

                    AddFriendResponse temp = HomeCall.getFriendTimeTable(campus, token);
                    if(temp.getStatus().getCode() == 0){
                        Data data = temp.getData();

                        ParseQuery<ParseUser> query = ParseUser.getQuery();
                        ParseUser u = (query.whereEqualTo("username",data.getRegNo())).getFirst();

                        //Is logged in with Facebook
                        if(u.getString("isSignedIn").equals("true")){
                            data.isFb = true;
                            data.fbId = u.getString("facebookID");
                            data.title = u.getString("facebookName");

                            Ion.with(context)
                                    .load("http://graph.facebook.com/" + data.fbId + "/picture?type=large")
                                    .write(new File(context.getCacheDir().getPath() + "/" + data.fbId + ".jpg"))
                                    .get();
                        }
                        else{
                            data.title = data.getRegNo();
                        }

                        temp.setData(data);
                        DataHandler.getInstance(context).addFriend(temp);
                        return DataHandler.getInstance(context).getToken();

                    }
                    else
                        throw new Exception(temp.getStatus().getMessage());
                }catch (Exception e){
                    e.printStackTrace();
                    this.e = e;
                }
                return null;
            }

            @Override
            protected void onPostExecute(Object o) {
                super.onPostExecute(o);
                listener.onCompleted(o, e);
            }
        }
        new bgTask().execute();
    }

    public void getToken(final onTaskCompleted listener){
        refreshCredentials();

        class bgTask extends AsyncTask<Void, Void, Object>{
            private Exception e;

            @Override
            protected Object doInBackground(Void... params) {
                try{
                    String token = DataHandler.getInstance(context).getToken();
                    if(token.equals("expired")){
                        if(!isConnected())
                            throw new Exception("No network connection!");

                        Response temp = HomeCall.sendRequest(regno, dob, campus, "/friends/regenerate");
                        if(temp.getStatus().getCode() == 0){
                            DataHandler.getInstance(context).saveShareJSON(HomeCall.json_response);
                            return DataHandler.getInstance(context).getToken();
                        }
                        else
                            throw new Exception(temp.getStatus().getMessage());
                    }
                    else
                        return token;

                }catch (Exception e){
                    e.printStackTrace();
                    this.e = e;
                }
                return null;
            }

            @Override
            protected void onPostExecute(Object o) {
                super.onPostExecute(o);
                listener.onCompleted(o, e);
            }
        }
        new bgTask().execute();
    }

    public void loginUser(final onTaskCompleted listener){
        refreshCredentials();
        class bgTask extends AsyncTask<Void, Void, Object>{
            private Exception e;

            @Override
            protected Object doInBackground(Void... params) {
                try{
                    if(!isConnected())
                        throw new Exception("No network connection!");
                    Response temp = HomeCall.sendRequest(regno, dob, campus, "/login/auto");
                    if(temp.getStatus().getCode() == 0)
                        return temp;
                    else
                        throw new Exception(temp.getStatus().getMessage());
                }catch (Exception e){
                    this.e = e;
                }
                return null;
            }

            @Override
            protected void onPostExecute(Object o) {
                super.onPostExecute(o);
                listener.onCompleted(o, e);
            }
        }
        new bgTask().execute();
    }

    public void firstUser(final onTaskCompleted listener){
        refreshCredentials();
        class bgTask extends AsyncTask<Void, Void, Object>{
            Exception e;
            @Override
            protected Object doInBackground(Void... params) {
                try{
                    if(!isConnected())
                        throw new Exception("No network connection!");
                    Response temp = HomeCall.sendRequest(regno, dob, campus, "/data/first");
                    if(temp.getStatus().getCode() == 0){
                        DataHandler.getInstance(context).saveFirstJSON(HomeCall.json_response);
                        DataHandler.getInstance(context).saveRefreshJSON(HomeCall.json_response);
                        DataHandler.getInstance(context).saveShareJSON(HomeCall.json_response);
                        return temp;
                    }
                    else
                        throw new Exception(temp.getStatus().getMessage());
                }catch (Exception e){
                    this.e = e;
                }
                return null;
            }

            @Override
            protected void onPostExecute(Object o) {
                super.onPostExecute(o);
                listener.onCompleted(o, e);
            }
        }

        loginUser(new onTaskCompleted() {
            @Override
            public void onCompleted(Object result, Exception e) {
                if(e == null)
                    new bgTask().execute();
                else
                    listener.onCompleted(null, e);
            }
        });
    }

    public void refreshData(final onTaskCompleted listener){
        refreshCredentials();
        class bgTask extends AsyncTask<Void, Void, Object>{
            Exception e;

            @Override
            protected Object doInBackground(Void... params) {
                try{
                    if(!isConnected())
                        throw new Exception("No network connection!");
                    Response temp = HomeCall.sendRequest(regno, dob, campus, "/data/refresh");
                    if(temp.getStatus().getCode() == 0){
                        DataHandler.getInstance(context).saveRefreshJSON(HomeCall.json_response);
                        return temp;
                    }
                    else
                        throw new Exception(temp.getStatus().getMessage());
                }catch (Exception e){
                    this.e = e;
                }
                return null;
            }

            @Override
            protected void onPostExecute(Object o) {
                super.onPostExecute(o);
                listener.onCompleted(o, e);
            }
        }

        loginUser(new onTaskCompleted() {
            @Override
            public void onCompleted(Object result, Exception e) {
                if(e == null)
                    new bgTask().execute();
                else
                    listener.onCompleted(null, e);
            }
        });
    }

    private void refreshCredentials(){
        regno = DataHandler.getInstance(context).getRegNo();
        dob = DataHandler.getInstance(context).getDOBString();
        if(DataHandler.getInstance(context).isVellore())
            campus = "vellore";
        else
            campus = "chennai";
    }

    public boolean isConnected(){
        ConnectivityManager cm =
            (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }
}
