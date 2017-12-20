package com.sample.mfpoauthloginandroid;
/**
* Copyright 2016 IBM Corp.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.worklight.wlclient.api.WLFailResponse;
import com.worklight.wlclient.api.WLResourceRequest;
import com.worklight.wlclient.api.WLResponse;
import com.worklight.wlclient.api.WLResponseListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;

public class ProtectedActivity extends AppCompatActivity {

    private ProtectedActivity _this;
    private TextView resultTextView;
    private BroadcastReceiver logoutReceiver, loginRequiredReceiver;
    private final String DEBUG_NAME = "ProtectedActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_protected);

        _this = this;

        Button getBalanceButton = (Button) findViewById(R.id.getBalance);
        Button invokeAPIButton = (Button) findViewById(R.id.invokeAPI);
        Button logoutButton = (Button) findViewById(R.id.logout);
        resultTextView = (TextView)findViewById(R.id.resultText);
        TextView helloLabel = (TextView) findViewById(R.id.helloLabel);

        //Show the display name
        try {
            SharedPreferences preferences = _this.getSharedPreferences(Constants.PREFERENCES_FILE, Context.MODE_PRIVATE);
            JSONObject user = new JSONObject(preferences.getString(Constants.PREFERENCES_KEY_USER,null));
            helloLabel.setText(getString(R.string.hello_user, user.getString("displayName")));
        } catch (JSONException e) {
            e.printStackTrace();
        }

        getBalanceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                URI adapterPath = null;

                try {
                    adapterPath = new URI("/adapters/ResourceAdapter/balance");
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
                WLResourceRequest request = new WLResourceRequest(adapterPath, WLResourceRequest.GET);
                

                request.send(new WLResponseListener() {
                    @Override
                    public void onSuccess(WLResponse wlResponse) {
                        updateTextView("Balance: " + wlResponse.getResponseText());
                    }

                    @Override
                    public void onFailure(WLFailResponse wlFailResponse) {
                        Log.d("Failure", wlFailResponse.getErrorMsg());
                        Log.d("Failure", wlFailResponse.getResponseJSON().toString());

                        updateTextView("Failed to get balance.");
                    }
                });
            }
        });

        invokeAPIButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                URI apicPathUri = null;
                try {

                    //please modify the URI below
                    apicPathUri = new
                            URI("https://<gateway-host-name>/<orgname>/<catalogname>/invokebackend/getdetails");


                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }


                    WLResourceRequest request = new WLResourceRequest(apicPathUri, WLResourceRequest.GET, "accessRestricted");
                // please modify APIC Client ID 
                    request.addHeader("X-IBM-Client-Id","APIC_CLIENT_ID");


                    request.send(new WLResponseListener() {
                        @Override
                        public void onSuccess(final WLResponse wlResponse) {
                            updateTextView("Success : "+wlResponse.getResponseText());
                        }
                        @Override
                        public void onFailure(final WLFailResponse wlFailResponse) {
                            //updateTextView("Failed:" + wlFailResponse.getErrorMsg());
                            //Log.d("Failure", wlFailResponse.getResponseJSON().toString());
                            String statuscode = wlFailResponse.getErrorStatusCode();
                            if (statuscode.equals("AUTHORIZATION_FAILURE")) {

                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        final RelativeLayout RelativeLayout1 = (RelativeLayout) findViewById(R.id.rl);
                                        LayoutInflater layoutInflater = (LayoutInflater) ProtectedActivity.this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                                        final View customView = layoutInflater.inflate(R.layout.popup,null);

                                        final Button closePopupBtn = (Button) customView.findViewById(R.id.closePopupBtn);


                                        //instantiate popup window
                                        final PopupWindow popupWindow = new PopupWindow(customView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

                                        //display the popup window
                                        popupWindow.showAtLocation(RelativeLayout1, Gravity.CENTER, 0, 0);

                                        //close the popup window on button click


                                        closePopupBtn.setOnClickListener(new View.OnClickListener() {
                                            @Override
                                            public void onClick(View v) {
                                                popupWindow.dismiss();
                                                Intent intent = new Intent();
                                                intent.setAction(Constants.ACTION_LOGOUT);
                                                LocalBroadcastManager.getInstance(_this).sendBroadcast(intent);
                                            }
                                        });

                                    }
                                });

                            }
                            else{
                                updateTextView("Failed:" + wlFailResponse.getErrorMsg());
                            }
                        }
                    });

            }
        });


        logoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setAction(Constants.ACTION_LOGOUT);
                LocalBroadcastManager.getInstance(_this).sendBroadcast(intent);
            }
        });

        logoutReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Intent login = new Intent(_this, LoginActivity.class);
                //Make sure to start with the clean stack
                login.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                login.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                _this.startActivity(login);
            }
        };

        loginRequiredReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Intent login = new Intent(_this, LoginActivity.class);
                _this.startActivity(login);
            }
        };
    }

    public void updateTextView(final String str){
        Runnable run = new Runnable() {
            public void run() {
                resultTextView.setText(str);
            }
        };
        this.runOnUiThread(run);
    }

    @Override
    protected void onStart() {
        Log.d(DEBUG_NAME, "onStart");
        super.onStart();
        LocalBroadcastManager.getInstance(this).registerReceiver(logoutReceiver, new IntentFilter(Constants.ACTION_LOGOUT_SUCCESS));
        LocalBroadcastManager.getInstance(this).registerReceiver(loginRequiredReceiver, new IntentFilter(Constants.ACTION_LOGIN_REQUIRED));
    }

    @Override
    protected void onPause() {
        Log.d(DEBUG_NAME, "onPause");
        LocalBroadcastManager.getInstance(this).unregisterReceiver(logoutReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(loginRequiredReceiver);
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Do you want to logout?").setTitle("Logout");
        builder.setPositiveButton("Logout", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent();
                intent.setAction(Constants.ACTION_LOGOUT);
                LocalBroadcastManager.getInstance(_this).sendBroadcast(intent);
            }
        });
        builder.setNegativeButton("Cancel", null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

}
