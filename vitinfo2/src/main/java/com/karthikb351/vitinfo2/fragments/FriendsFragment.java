package com.karthikb351.vitinfo2.fragments;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.nfc.NfcAdapter;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.karthikb351.vitinfo2.Application;
import com.karthikb351.vitinfo2.Home;
import com.karthikb351.vitinfo2.R;
import com.karthikb351.vitinfo2.adapters.FriendsAdapter;
import com.karthikb351.vitinfo2.api.Objects.AddFriendResponse;
import com.karthikb351.vitinfo2.api.Objects.VITxApi;
import com.karthikb351.vitinfo2.objects.BarCodeScanner.IntentIntegrator;
import com.karthikb351.vitinfo2.objects.BarCodeScanner.ZXingLibConfig;
import com.karthikb351.vitinfo2.objects.DataHandler;
import com.karthikb351.vitinfo2.objects.RecyclerViewOnClickListener;
import com.koushikdutta.ion.Ion;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

/**
 * Created by saurabh on 7/20/14.
 */
public class FriendsFragment extends Fragment {

    private DataHandler dat;
    private ZXingLibConfig zxingLibConfig;
    private RecyclerView listView;
    private SwipeRefreshLayout mPullToRefreshLayout;
    private ProgressDialog pdiag;
    Refresh_Data refresh_data;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_friends,container, false);

        Tracker t = ((Application) getActivity().getApplication()).getTracker(Application.TrackerName.GLOBAL_TRACKER);
        t.setScreenName("Friends Fragment");
        t.send(new HitBuilders.AppViewBuilder().build());

        mPullToRefreshLayout = (SwipeRefreshLayout) v.findViewById(R.id.friends_pull);

        mPullToRefreshLayout.setEnabled(false);

        mPullToRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.accent));

        listView = (RecyclerView) v.findViewById(R.id.enhanced_list);
        listView.setLayoutManager(new LinearLayoutManager(getActivity()));
        listView.setHasFixedSize(false);

        setHasOptionsMenu(true);
        dat = DataHandler.getInstance(getActivity());
        zxingLibConfig = new ZXingLibConfig();
        zxingLibConfig.useFrontLight = true;

        new Load_Data().execute();

        refresh_data = new Refresh_Data();
        refresh_data.execute();
        return v;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_fragment_friends, menu);
        try
        {
            //TODO: Why is this even crashing!? :/
            if(dat.isFacebookLogin())
                menu.removeItem(R.id.menu_fb_login);

        }catch (Exception ignore){}
    }

    private ProgressDialog diag;

    private void showShareAlert(){
        diag = new ProgressDialog(getActivity());
        diag.setMessage("Loading");
        diag.setCancelable(false);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Share TimeTable")
                .setItems(R.array.freinds_share, new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int which) {
                        diag.show();

                        VITxApi.getInstance(getActivity()).getToken(new VITxApi.onTaskCompleted() {
                            @Override
                            public void onCompleted(Object result, Exception e) {
                                if(e != null)
                                    Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                                else{
                                    Home h = (Home) getActivity();
                                    h.token = (String) result;

                                    //QR CODE
                                    if(which == 0)
                                        h.selectItem_Async(6);
                                        //NFC
                                    else{
                                        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH){
                                            if(((Home) getActivity()).hasNFC){
                                                ((Home) getActivity()).enableNdefExchangeMode();
                                                ((Home) getActivity()).selectItem_Async(7);
                                            }
                                            else
                                                Toast.makeText(getActivity(), "Could not connect to a NFC service.", Toast.LENGTH_SHORT).show();
                                        }
                                        else
                                        {
                                            Toast.makeText(getActivity(), "NFC not supported on your device", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                }
                                diag.dismiss();
                            }
                        });
                    }});
        builder.show();
    }

    public String TOKEN = "";


    public void addFriendToList(){
        pdiag = new ProgressDialog(getActivity());
        pdiag.setMessage("Adding Friend");
        pdiag.setTitle("Please wait");
        pdiag.setCancelable(false);
        pdiag.show();
        VITxApi.getInstance(getActivity()).addFriend(TOKEN, new VITxApi.onTaskCompleted() {
            @Override
            public void onCompleted(Object result, Exception e) {
                if (e == null) {
                    Toast.makeText(getActivity(), "Friend Added!", Toast.LENGTH_SHORT).show();
                    ((Home) getActivity()).selectItem_Async(3);
                } else
                    Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                pdiag.dismiss();
            }
        });
    }

    private void showAddAlert(){
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Add Friend")
                .setItems(R.array.freinds_add, new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int which) {

                        //Scan Barcode
                        if(which == 1)
                            IntentIntegrator.initiateScan(getActivity(), zxingLibConfig);

                        //Enter PIN
                        else if(which == 0){
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setTitle("Enter PIN");
                            final EditText input = new EditText(getActivity());
                            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
                            builder.setView(input);
                            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    TOKEN = input.getText().toString();
                                    addFriendToList();
                                }
                            });
                            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            });

                            builder.show();
                        }

                        else if(which == 2){
                            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            // Get the layout inflater
                            LayoutInflater inflater = getActivity().getLayoutInflater();

                            // Inflate and set the layout for the dialog
                            // Pass null as the parent view because its going in the dialog layout
                            builder.setTitle("Enter Credentials");
                            final View dView = inflater.inflate(R.layout.dialog_add_freind, null);
                            builder.setView(dView)
                                    // Add action buttons
                                    .setPositiveButton("Add", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(final DialogInterface dialog, int id) {
                                            VITxApi.getInstance(getActivity()).addFriendWithCredentials(
                                                    ((EditText) dView.findViewById(R.id.username)).getText().toString().toUpperCase(),
                                                    ((EditText) dView.findViewById(R.id.password)).getText().toString(),
                                                    new VITxApi.onTaskCompleted() {
                                                        @Override
                                                        public void onCompleted(Object result, Exception e) {
                                                            if (e == null) {
                                                                Toast.makeText(getActivity(), "Friend Added!", Toast.LENGTH_SHORT).show();
                                                                ((Home) getActivity()).selectItem_Async(3);
                                                            } else
                                                                Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                                                            pdiag.dismiss();
                                                        }
                                                    }
                                            );
                                            pdiag = new ProgressDialog(getActivity());
                                            pdiag.setMessage("Adding Friend");
                                            pdiag.setTitle("Please wait");
                                            pdiag.setCancelable(false);
                                            pdiag.show();
                                            dialog.dismiss();
                                        }
                                    })
                                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            dialog.cancel();
                                        }
                                    });
                            builder.show();
                        }
                        else if(which == 3){
                            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH){
                                NfcAdapter mNfcAdapter = NfcAdapter.getDefaultAdapter(getActivity());

                                if(mNfcAdapter!=null && mNfcAdapter.isEnabled()){
                                    ((Home) getActivity()).enableNdefExchangeMode();
                                    ((Home) getActivity()).selectItem_Async(8);
                                }
                                else
                                    Toast.makeText(getActivity(), "Could not connect to a NFC service.", Toast.LENGTH_SHORT).show();

                            }
                            else
                            {
                                Toast.makeText(getActivity(), "NFC not supported on your device", Toast.LENGTH_SHORT).show();
                            }
                        }

                    }
                });
        builder.show();
    }

    private void showFriendClickDetailsDialog(final AddFriendResponse f){
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setItems(R.array.friends_details, new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int which) {
                        switch (which){
                            case 0:
                                Runnable runnable = new Runnable() {
                                    @Override
                                    public void run() {
                                        dat.deleteFriend(f);
                                        ((Home) getActivity()).selectItem_Async(3);
                                    }
                                };

                                new Thread(runnable).start();
                                Toast.makeText(getActivity(), f.getData().title + " has been deleted.", Toast.LENGTH_SHORT).show();
                                break;

                            case 1:
                                showEditFriendName(f);
                                break;
                            case 2:
                                break;
                        }
                    }
                });
        builder.show();
    }

    private void showEditFriendName(final AddFriendResponse f){
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Display Name");
        final EditText input = new EditText(getActivity());
        input.setHint(f.getData().title);
        builder.setView(input);

        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        dat.deleteFriend(f);
                        f.getData().title = input.getText().toString();
                        dat.addFriend(f);
                        ((Home) getActivity()).selectItem_Async(3);
                    }};
                new Thread(runnable).start();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();
    }

    @Override
    public void onPause(){
        super.onPause();
        refresh_data.cancel(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final Handler handler = new Handler();
        // handle item selection
        switch (item.getItemId()) {
            case R.id.menu_fb_login:
                ((Home) getActivity()).selectItem_Async(5);
                return true;
            case R.id.menu_add_person:
                showAddAlert();
                return true;
            case R.id.menu_share_tt:
                showShareAlert();
                return true;
            case R.id.menu_backup:
                mPullToRefreshLayout.setRefreshing(true);
                Runnable runnable = new Runnable() {
                    boolean isDone = false;
                    @Override
                    public void run() {
                        isDone = dat.saveFriendstoSDCard();
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                if(isDone)
                                    Toast.makeText(getActivity(), "Friend list backed up to SD card", Toast.LENGTH_SHORT).show();
                                else
                                    Toast.makeText(getActivity(), "Error occured while backing up. Please try again!", Toast.LENGTH_SHORT).show();
                                mPullToRefreshLayout.setRefreshing(false);
                            }
                        });
                    }};
                new Thread(runnable).start();
                return true;

            case R.id.menu_restore:
                mPullToRefreshLayout.setRefreshing(true);
                Runnable runnable_restore = new Runnable() {
                    boolean isDone = false;
                    @Override
                    public void run() {
                        isDone = dat.loadFriendsfromSDCard();
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                if(isDone)
                                    Toast.makeText(getActivity(), "Restored from SD card", Toast.LENGTH_SHORT).show();
                                else
                                    Toast.makeText(getActivity(), "Error occured while restoring. Check if backup file is at root of SD Card.", Toast.LENGTH_SHORT).show();
                                mPullToRefreshLayout.setRefreshing(false);
                                ((Home) getActivity()).selectItem_Async(3);
                            }
                        });
                    }};
                new Thread(runnable_restore).start();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private class Load_Data extends AsyncTask<Void,Void,Void> {
        private ArrayList<AddFriendResponse> friends;

        protected void onPreExecute(){
            friends = new ArrayList<AddFriendResponse>();
            mPullToRefreshLayout.setRefreshing(true);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try
            {
                friends = DataHandler.getInstance(getActivity()).getFreinds();
                for(int i = 0; i < friends.size(); i++)
                {
                    if(friends.get(i).getData().isFb){
                        File file = new File(getActivity().getCacheDir().getPath() + "/" + friends.get(i).getData().fbId + ".jpg");
                        try {
                            BitmapFactory.Options options = new BitmapFactory.Options();
                            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                            friends.get(i).getData().img_profile = BitmapFactory.decodeStream(new FileInputStream(file), null, options);
                        }catch (Exception e){e.printStackTrace();}
                    }
                }

            }catch (Exception ignore){}
            return null;
        }

        protected void onPostExecute(Void voids){
            mPullToRefreshLayout.setRefreshing(false);
            try{
                FriendsAdapter adapter = new FriendsAdapter(getActivity(), friends, new RecyclerViewOnClickListener() {
                    @Override
                    public void onClick(Object result, boolean isLongPress) {
                        AddFriendResponse f = (AddFriendResponse) result;
                        //Not long click
                        if(!isLongPress){
                            showFriendClickDetailsDialog(f);
                        }
                    }
                });
                listView.setAdapter(adapter);
                registerForContextMenu(listView);
                listView.setLayoutManager(new LinearLayoutManager(getActivity()));
                listView.setItemAnimator(new DefaultItemAnimator());
                if(friends.size() == 0)
                    Toast.makeText( getActivity(),"Add people to get their details!", Toast.LENGTH_SHORT).show();
            }catch (Exception e){e.printStackTrace();}

            if(friends.size() >= 1){
                if(!dat.getviewShowCaseFrnd()){

                    dat.saveviewShowCaseFrnd(true);
                }
            }

        }
    }

    private class Refresh_Data extends AsyncTask<Void,Void,Void>{

        private ArrayList<AddFriendResponse> friends;

        protected void onPreExecute(){
            friends = new ArrayList<>();
        }

        private boolean haveNetworkConnection() {
            boolean haveConnectedWifi = false;
            boolean haveConnectedMobile = false;

            ConnectivityManager cm = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo[] netInfo = cm.getAllNetworkInfo();
            for (NetworkInfo ni : netInfo) {
                if (ni.getTypeName().equalsIgnoreCase("WIFI"))
                    if (ni.isConnected())
                        haveConnectedWifi = true;
                if (ni.getTypeName().equalsIgnoreCase("MOBILE"))
                    if (ni.isConnected())
                        haveConnectedMobile = true;
            }
            return haveConnectedWifi || haveConnectedMobile;
        }

        private void downloadProfileImage(final ArrayList<AddFriendResponse> friends){
            for(int i = 0; i < friends.size(); i++){
                String fbId;
                fbId = friends.get(i).getData().fbId;
                final int j = i;
                if(friends.get(i).getData().isFb && getActivity() != null){
                    try {
                        if(isCancelled())
                        Ion.with(getActivity())
                                .load("http://graph.facebook.com/" + fbId + "/picture?type=square")
                                .write(new File(getActivity().getCacheDir().getPath() + "/" + fbId + ".jpg"))
                                .get();

                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                        friends.get(j).getData().img_profile = BitmapFactory.decodeFile(getActivity().getCacheDir().getPath() + "/" + fbId + ".jpg", options);
                    }
                    catch (InterruptedException e2){e2.printStackTrace();}
                    catch (ExecutionException e3){e3.printStackTrace();}

                }
            }
        }
        private boolean needSaving = false;
        @Override
        protected Void doInBackground(Void... voids) {
            if(!haveNetworkConnection())
                return null;
            try {
                friends = DataHandler.getInstance(getActivity()).getFreinds();
                for(int i = 0; i < friends.size(); i++){
                    if (isCancelled())
                        return null;
                    if(!friends.get(i).getData().isFb){
                        ParseQuery<ParseUser> query = ParseUser.getQuery();
                        ParseUser u = (query.whereEqualTo("username",friends.get(i).getData().getRegNo())).getFirst();

                        String isIt = u.getString("isSignedIn");
                        if(isIt != null && isIt.equals("true")){
                            friends.get(i).getData().isFb = true;
                            friends.get(i).getData().fbId = u.get("facebookID").toString();
                            friends.get(i).getData().title = u.get("facebookName").toString();
                            needSaving = true;
                        }
                    }
                }
                if(needSaving)
                    dat.saveFriends(friends);
                downloadProfileImage(friends);
            }catch (Exception e){e.printStackTrace();}

            return null;
        }

        protected void onPostExecute(Void voids){
            if(needSaving){
                Toast.makeText(getActivity(), "Friends list was updated.", Toast.LENGTH_SHORT).show();
                ((Home) getActivity()).selectItem_Async(3);
            }
        }
    }
}