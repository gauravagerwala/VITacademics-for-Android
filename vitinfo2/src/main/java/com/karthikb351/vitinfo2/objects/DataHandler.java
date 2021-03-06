package com.karthikb351.vitinfo2.objects;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;

import com.google.gson.Gson;
import com.karthikb351.vitinfo2.api.Objects.AddFriendResponse;
import com.karthikb351.vitinfo2.api.Objects.Course;
import com.karthikb351.vitinfo2.api.Objects.Response;
import com.karthikb351.vitinfo2.api.Objects.Share;
import com.karthikb351.vitinfo2.api.Objects.Timetable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Created by saurabh on 4/22/14.
 */
public class DataHandler {
    public Context context;
    SharedPreferences preferences;
    private static Timetable firstJSON;
    private static Response refresh;
    private static Share shareJSON;
    private static DataHandler mInstance;

    public DataHandler(Context context){
        this.context = context;
        try
        {
            preferences = PreferenceManager.getDefaultSharedPreferences(context);

            SharedPreferences prefs = context.getSharedPreferences("firstJSON", Context.MODE_PRIVATE);
            Gson gson = new Gson();

            //Load Timetable from /first
            firstJSON = gson.fromJson(prefs.getString("firstJSON", ""), Response.class).getTimetable();

            //Load refreshed subject data
            prefs = context.getSharedPreferences("refreshJSON", Context.MODE_PRIVATE);
            refresh =  gson.fromJson(prefs.getString("refreshJSON", ""), Response.class);

            //Laod share token data
            prefs = context.getSharedPreferences("shareJSON", Context.MODE_PRIVATE);
            shareJSON = gson.fromJson(prefs.getString("shareJSON", ""), Response.class).getShare();
        }catch (Exception ignore){ignore.printStackTrace();}
    }

    public static DataHandler getInstance(Context context){
        if(mInstance == null)
            mInstance = new DataHandler(context.getApplicationContext());
        return mInstance;
    }

    public static void DELETE_ALL_DATA(Context context){
        try{
            PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit();
            context.deleteDatabase("subjectsManager");
        }catch (Exception ignore){}
    }

    public boolean isHeroku(){
        return preferences.getBoolean("isHeroku", false);
    }

    public void setIsHeroku(boolean val){
        preferences.edit().putBoolean("isHeroku", val).commit();
    }
    public void saveString(String key, String string){
        preferences.edit().putString(key, string).commit();
    }

    public void saveFirstJSON(String string){
        SharedPreferences prefs = context.getSharedPreferences("firstJSON", Context.MODE_PRIVATE);
        prefs.edit().putString("firstJSON", string).commit();
        Gson gson = new Gson();
        firstJSON = gson.fromJson(prefs.getString("firstJSON", ""), Response.class).getTimetable();
    }

    public void saveRefreshJSON(String string){
        SharedPreferences prefs = context.getSharedPreferences("refreshJSON", Context.MODE_PRIVATE);
        prefs.edit().putString("refreshJSON", string).commit();
        Gson gson = new Gson();
        refresh =  gson.fromJson(prefs.getString("refreshJSON", ""), Response.class);
    }

    public void saveShareJSON(String json){
        SharedPreferences prefs = context.getSharedPreferences("shareJSON", Context.MODE_PRIVATE);
        prefs.edit().putString("shareJSON", json).commit();
        Gson gson = new Gson();
        shareJSON = gson.fromJson(prefs.getString("shareJSON", ""), Response.class).getShare();
    }

    public void setShareJSON(Share share){
        shareJSON = share;
    }

    public Timetable getTimeTable2(){
        return firstJSON;
    }

    public Course getCourse(String clsnbr){
        return refresh.getCourse(clsnbr);
    }

    public List<Course> getAllCourses(){return refresh.getCourses();}

    public Share getShareJSON(){return shareJSON;}

    public String getToken(){

        try {
            String dt = shareJSON.getIssued();
            DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            Date issue_date =  df.parse(dt);

            Calendar cal = Calendar.getInstance();
            cal.setTime(issue_date);

            //Add validity
            cal.add(Calendar.HOUR_OF_DAY, shareJSON.getValidity());

            //Compare Dates
            if(cal.getTime().after(new Date())){
                return shareJSON.getToken();
            }
        }catch (Exception ignore){ignore.printStackTrace();}
        return "expired";
    }

    //Save Friend JSON
    public void addFriend(AddFriendResponse f){
        int cur = preferences.getInt("FRIENDJSONSIZE", 0);
        Gson gson = new Gson();
        saveString("FRIENDJSON"+cur ,gson.toJson(f));
        System.out.println(gson.toJson(f));
        saveInt("FRIENDJSONSIZE", ++cur);

    }

    public ArrayList<AddFriendResponse> getFreinds(){
        ArrayList<AddFriendResponse> t = new ArrayList<>();
        int size = preferences.getInt("FRIENDJSONSIZE", 0);

        Gson gson = new Gson();

        for(int i = 0; i < size; i++){
            t.add(gson.fromJson(preferences.getString("FRIENDJSON"+i,""), AddFriendResponse.class));
            t.get(i).getData().img_profile = null;
        }
        Collections.sort(t, new FriendComparator());
        return t;
    }

    public void saveFriends(ArrayList<AddFriendResponse> friends){
        Gson gson = new Gson();
        String t;
        for(int i = 0 ; i < friends.size(); i++){
            t = gson.toJson(friends.get(i));
            saveString("FRIENDJSON" + i , t);
        }
        saveInt("FRIENDJSONSIZE", friends.size());
    }

    public void deleteFriend(AddFriendResponse f){
        try {
            ArrayList<AddFriendResponse> t = getFreinds();

            for(int i = 0; i < t.size(); i++){
                if(f.getData().getRegNo().equals(t.get(i).getData().getRegNo())){
                    t.remove(i);
                    break;
                }
            }
            saveFriends(t);

        }catch (Exception e){e.printStackTrace();}
    }

    public boolean loadFriendsfromSDCard(){
        try {
            FileInputStream inputStream = new FileInputStream(new File(Environment.getExternalStorageDirectory() + "/VITacademics_backup.bak"));
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String receiveString;
            StringBuilder stringBuilder = new StringBuilder();
            while ( (receiveString = bufferedReader.readLine()) != null ) {
                stringBuilder.append(receiveString);
            }
            inputStream.close();
            JSONArray j = new JSONArray(SimpleCrypto.decrypt(Secret.backup_password,stringBuilder.toString()));

            for(int i = 0; i < j.length(); i++){
                addFriend(new Gson().fromJson(j.getString(i), AddFriendResponse.class));
            }
            return true;
        }catch (Exception e){
            e.printStackTrace();
            return false;
        }

    }

    public boolean saveFriendstoSDCard(){
        try {
            int size = preferences.getInt("FRIENDJSONSIZE", 0);

            JSONArray fl = new JSONArray();

            for(int i = 0; i < size; i++)
                fl.put(i, preferences.getString("FRIENDJSON"+i,""));

            File myFile = new File(Environment.getExternalStorageDirectory() + "/VITacademics_backup.bak");
            myFile.createNewFile();
            FileOutputStream fOut = new FileOutputStream(myFile);
            OutputStreamWriter myOutWriter = new OutputStreamWriter(fOut);

            /*
             * Create class in objectes called Secret.java and put this in:
             * public class Secret {public static String backup_password = "ANYSTRINGHERE";}
             *
             */
            myOutWriter.append(SimpleCrypto.encrypt(Secret.backup_password, fl.toString()));
            myOutWriter.close();
            fOut.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void saveServerStatus(String json){saveString("SERVERSTATUS",json);}

    private void saveInt(String key, int num){
        preferences.edit().putInt(key, num).commit();
    }

    public void addPushMessage(PushMessage pm){
        int cur = preferences.getInt("PUSHMESSAGEJSONSIZE", 0);
        Gson gson = new Gson();

        String t = gson.toJson(pm);
        saveString("PUSHMESSAGEJSON"+cur,t);

        saveInt("PUSHMESSAGEJSONSIZE", cur + 1);
    }

    public void savePushMessages(ArrayList<PushMessage> msgs){
        Gson gson = new Gson();
        for(int i = 0; i < msgs.size(); i++)
            saveString("PUSHMESSAGEJSON"+i, gson.toJson(msgs));

        saveInt("PUSHMESSAGEJSONSIZE", msgs.size());
    }


    public void saveviewShowCase(Boolean shown){preferences.edit().putBoolean("ViewShowCase",shown).commit();}

    public void saveviewShowCaseFrnd(Boolean shown){preferences.edit().putBoolean("ViewShowCaseFrnd",shown).commit();}

    public void saveRegNo(String regno){ saveString("REGNO", regno.trim());}

    public void saveDob(int[] dob){for(int i = 0; i < 3; i++) saveInt("DOB"+i, dob[i]);}

    public void saveCampus(Boolean isVellore){preferences.edit().putBoolean("isVellore",isVellore).commit();}

    public void setFbLogin(Boolean isFbLogin){preferences.edit().putBoolean("isFbLogin", isFbLogin).commit();}

    public String getRegNo(){
        return preferences.getString("REGNO", "").toUpperCase();
    }

    public boolean getviewShowCase(){return  preferences.getBoolean("ViewShowCase",false);}

    public boolean getviewShowCaseFrnd(){return  preferences.getBoolean("ViewShowCaseFrnd",false);}

    public int[] getDOB(){int[] dob = new int[3]; for(int i = 0; i < 3; i++)dob[i] = preferences.getInt("DOB"+i, 0); return dob;}

    public String getDOBString(){int[] dob = getDOB(); return check_dob(dob[0]) + check_dob(dob[1] + 1) + Integer.toString(dob[2]);}

    public boolean isVellore(){return preferences.getBoolean("isVellore", true);}

    public boolean isFacebookLogin(){return preferences.getBoolean("isFbLogin", false);}

    public int getDefUi(){return Integer.parseInt(preferences.getString("defUi","0"));}

    private class FriendComparator implements Comparator<AddFriendResponse> {
        @Override
        public int compare(AddFriendResponse friend, AddFriendResponse friend2) {
            return friend.getData().title.compareTo(friend2.getData().title);
        }
    }

    public String getServerStatus(){return  preferences.getString("SERVERSTATUS","");}

    public String getTokenExpiryTimeString(){
        try {
            JSONObject obj = new JSONObject(preferences.getString("PINJSON",""));
            String dt = obj.getString("expiry");

            DateFormat df = new SimpleDateFormat("yyyy-MM-dd kk:mm:ss");
            Date expiry_date =  df.parse(dt);

            return "Expires " + DateUtils.getRelativeTimeSpanString(expiry_date.getTime(), new Date().getTime() , 0).toString();
        }catch (Exception ignore){}
        return "";
    }

    private String check_dob(int num){
        String t = Integer.toString(num);
        if(t.length() == 1)
            t = "0"+t;
        return t;
    }

    public ArrayList<PushMessage> getAllPushMessage(){
        ArrayList<PushMessage> temp = new ArrayList<>();
        try{

            Gson gson = new Gson();
            int size = preferences.getInt("PUSHMESSAGEJSONSIZE",0);

            for(int i = 0; i < size; i++){
                PushMessage p = gson.fromJson(preferences.getString("PUSHMESSAGEJSON"+i,""),PushMessage.class);
                temp.add(p);
            }

        }catch (Exception e){e.printStackTrace();}

        return temp;
    }

    public void deletePushMessage(PushMessage msg){
        ArrayList<PushMessage> temp = getAllPushMessage();
        for(int i = 0 ; i < temp.size(); i++){
            if(msg.title.equals(temp.get(i).title) && msg.message.equals(temp.get(i).message))
            {
                temp.remove(i);
                break;
            }
        }
        savePushMessages(temp);

    }

    public boolean isNewUser(){return preferences.getBoolean("NewUser", true);}

    public void setNewUser(boolean bol){preferences.edit().putBoolean("NewUser", bol).commit();}

    public static float getPer(int num, int div)
    {
        return (float)((int)(((float)num/div)*1000))/10;
    }

    public static int getPerColor(int per){
        if (per < 80 && per >= 75)
            return Color.parseColor("#ffa500");
        else if (per < 75)
            return Color.parseColor("#ff0000");
        else
            return Color.parseColor("#008000");
    }

}
