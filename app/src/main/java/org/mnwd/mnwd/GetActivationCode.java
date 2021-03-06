package org.mnwd.mnwd;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

public class GetActivationCode extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private DrawerLayout drawer;
    private NavigationView navigationView;
    private Toolbar toolbar = null;
    private FloatingActionButton fab;

    private TextView txtActivateInfo, txtAcctNo;
    private Spinner spinnerAccountNo;
    private Button btnGetActivationCode;
    private ProgressBar progressBar;

    private String accountno, result;
    private String [] arrAccountNo;

    //refresh
    private SwipeRefreshLayout swipeRefreshLayout;

    //content
    private String JSON_STRING;
    //

    //notification
    private String notif1, notif2, notif3, mixed;

    //SESSION
    private String session_accountid;
    private String session_userid;
    private String session_email;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_get_activation_code);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //refresh
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.idSwipeRefreshLayout);
        swipeRefreshLayout.setColorSchemeResources(R.color.swipe1, R.color.swipe2, R.color.swipe3);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener(){
            @Override
            public void onRefresh() {
                swipeRefreshLayout.setRefreshing(true);
                (new Handler()).postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        swipeRefreshLayout.setRefreshing(false);
                        Intent startIntent = new Intent(getApplicationContext(), GetActivationCode.class);
                        startActivity(startIntent);
                    }
                }, 1000);
            }
        });

        //notif
        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setVisibility(View.GONE);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent startIntent = new Intent(getApplicationContext(), Notification.class);
                mixed = notif1 + "~" + notif2 + "~" + notif3;
                startIntent.putExtra("mixed", mixed);
                startActivity(startIntent);
            }
        });

        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        //SESSION
        sharedPreferences = getSharedPreferences(Config.FILENAME_SESSION, Context.MODE_PRIVATE);

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        ReusableFunctions.changeNavDrawerTitle (navigationView, sharedPreferences);

        txtAcctNo = (TextView) findViewById(R.id.idTxtAcctNo);
        txtActivateInfo = (TextView) findViewById(R.id.idTxtActivateInfo2);
        spinnerAccountNo = (Spinner) findViewById(R.id.idSpinnerActivateAccount);
        progressBar = (ProgressBar) findViewById(R.id.idProgressBar);

        btnGetActivationCode = (Button) findViewById(R.id.idBtnGetActivationCode);
        btnGetActivationCode.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        activateAccount();
                    }
                }
        );

        //content
        getJSON();
        //

        //notification
        checkNotification ();
    }

    //notification
    private void showNotificationIcon () {
        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(JSON_STRING);
            JSONArray result = jsonObject.getJSONArray(Config.TAG_JSON_ARRAY);

            JSONObject jo = result.getJSONObject(0);
            notif1 = jo.getString("notif1");
            notif2 = jo.getString("notif2");
            notif3 = jo.getString("notif3");

            if (notif1 != "-1" || notif2 != "-1" || notif3 != "-1") {
                fab.setVisibility(View.VISIBLE);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void checkNotification () {
        class CheckNotification extends AsyncTask<Void,Void,String> {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);

                String conn_success = "connection success~";
                if (s.contains(conn_success)) {
                    JSON_STRING = s.replaceAll(conn_success, "");
                    showNotificationIcon();
                }
                else {
                    Toast.makeText (GetActivationCode.this, s, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            protected String doInBackground(Void... params) {
                //RETRIEVE SESSION DATA
                session_accountid = sharedPreferences.getString(Config.SESSION_ACCOUNTID, null);
                session_userid = sharedPreferences.getString(Config.SESSION_USERID, null);

                //argument for the php script
                HashMap<String,String> parameter = new HashMap<> ();
                parameter.put(Config.KEY_CON_ACCOUNTID, session_accountid);
                parameter.put(Config.KEY_CON_USERID, session_userid);

                RequestHandler rh = new RequestHandler();
                String s = rh.sendPostRequest(Config.URL_CHECKNOTIFICATION, parameter);
                return s;
            }
        }
        CheckNotification cn = new CheckNotification();
        cn.execute();
    }
    //

    //content
    private void showAllInactiveAccounts() {
        JSONObject jsonObject = null;
        ArrayList<HashMap<String,String>> list = new ArrayList<HashMap<String, String>>();
        try {
            jsonObject = new JSONObject(JSON_STRING);
            JSONArray result = jsonObject.getJSONArray(Config.TAG_JSON_ARRAY);

            arrAccountNo = new String [result.length()];
            for(int i = 0; i < result.length(); i++){
                JSONObject jo = result.getJSONObject(i);
                String accountnumber = jo.getString(Config.TAG_ACCOUNT_ACCOUNTNO);
                arrAccountNo[i] = accountnumber;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        ArrayAdapter<String> arrayAdapterAccountNo = new ArrayAdapter<String>(this, android.R.layout.simple_dropdown_item_1line, arrAccountNo);
        spinnerAccountNo = (Spinner) findViewById (R.id.idSpinnerActivateAccount);
        spinnerAccountNo.setAdapter(arrayAdapterAccountNo);
    }

    private void getJSON(){
        class GetJSON extends AsyncTask<Void,Void,String> {

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);

                String conn_success = "connection success~";
                if (s.contains(conn_success)) {
                    JSON_STRING = s.replaceAll(conn_success, "");
                    String account404 = "~";
                    if (JSON_STRING.contains(account404)) {
                        txtActivateInfo.setText(JSON_STRING.replaceAll(account404, ""));
                    }
                    else {
                        txtAcctNo.setVisibility(View.VISIBLE);
                        spinnerAccountNo.setVisibility(View.VISIBLE);
                        btnGetActivationCode.setVisibility(View.VISIBLE);
                        showAllInactiveAccounts();
                    }
                }
                else {
                    Toast.makeText (GetActivationCode.this, s, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            protected String doInBackground(Void... params) {
                //RETRIEVE SESSION DATA
                session_userid = sharedPreferences.getString(Config.SESSION_USERID, null);

                //argument for the php script
                HashMap<String,String> parameter = new HashMap<> ();
                parameter.put(Config.KEY_CON_USERID, session_userid);

                RequestHandler rh = new RequestHandler();
                String s = rh.sendPostRequest(Config.URL_GETALLINACTIVEACCOUNTS, parameter);
                return s;
            }
        }
        GetJSON gj = new GetJSON();
        gj.execute();
    }
    //

    private void activateAccount() {
        accountno = String.valueOf(spinnerAccountNo.getSelectedItem());

        class ActivateAccountNo extends AsyncTask<Void, Void, String> {
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                progressBar.setVisibility(View.GONE);

                String conn_success = "connection success~";
                if (s.contains(conn_success)) {
                    String activate_status = s.replaceAll(conn_success, "");
                    String activate_success = "Activation code successfully set.";
                    String already_sent = "was already sent to your email address";
                    if (activate_status.contains(activate_success)) {
                        Toast.makeText (GetActivationCode.this, "Activation code successfully sent", Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(getApplicationContext(), ActivateAccount.class);
                        intent.putExtra(Config.ACCOUNT_NO, accountno);
                        startActivity(intent);
                    }
                    else {
                        Toast.makeText (GetActivationCode.this, activate_status, Toast.LENGTH_LONG).show();
                        if (activate_status.contains(already_sent)) {
                            Intent intent = new Intent(getApplicationContext(), ActivateAccount.class);
                            intent.putExtra(Config.ACCOUNT_NO, accountno);
                            startActivity(intent);
                        }
                    }
                }
                else {
                    Toast.makeText (GetActivationCode.this, s, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            protected String doInBackground(Void... voids) {
                session_email = sharedPreferences.getString(Config.SESSION_EMAIL, null);

                HashMap<String,String> params = new HashMap<> ();
                params.put(Config.KEY_CON_ACCOUNTNO, accountno);
                params.put(Config.KEY_CON_EMAIL, session_email);

                RequestHandler rh = new RequestHandler();
                result = rh.sendPostRequest(Config.URL_SENDACTIVATIONCODE, params);
                return result;
            }
        }
        ActivateAccountNo a = new ActivateAccountNo();
        a.execute();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        SharedPreferences.Editor editor = sharedPreferences.edit();
        Intent intent = ReusableFunctions.navigateOptions(id, GetActivationCode.this, editor);
        startActivity(intent);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}