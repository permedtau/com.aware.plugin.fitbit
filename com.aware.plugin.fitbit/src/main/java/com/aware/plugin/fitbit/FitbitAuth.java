package com.aware.plugin.fitbit;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Browser;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;

import com.aware.Aware;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth2AccessToken;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by denzil on 10/01/2017.
 */

public class FitbitAuth extends AppCompatActivity {

    @Override
    protected void onResume() {
        super.onResume();

        // erez - added this if
        if (Aware.getSetting(getApplicationContext(), Settings.PREF_FITBIT_AUTHORIZATION_REQUIRED).equals("true")) {
            Aware.setSetting(getApplicationContext(), Settings.PREF_FITBIT_AUTHORIZATION_REQUIRED, "false");

            Plugin.fitbitOAUTHToken = null;
            Plugin.fitbitAPI = null;
            Plugin.devicesPicker = null;

            authorizeFitbit();
        }
        else {
            if (Plugin.fitbitOAUTHToken != null && Plugin.fitbitAPI != null) {
                Toast.makeText(getApplicationContext(), "Authentication to Fitbit succeeded!", Toast.LENGTH_SHORT).show();

                //erez
                Aware.setSetting(getApplicationContext(), Settings.PREF_DEVICE_PICKER_REQUIRED, "true");

                Intent fitbit = new Intent(this, Plugin.class);
                startService(fitbit);

                finish();
            } else {
                Toast.makeText(getApplicationContext(), "Authentication to Fitbit failed!", Toast.LENGTH_SHORT).show();

                finish();
            }
        }
    }

    public void authorizeFitbit() {
        String scopes = "activity heartrate sleep settings";

        Plugin.fitbitAPI = new ServiceBuilder(Aware.getSetting(getApplicationContext(), Settings.API_KEY_PLUGIN_FITBIT))
                .apiKey(Aware.getSetting(getApplicationContext(), Settings.API_KEY_PLUGIN_FITBIT))
                .scope(scopes)
                .responseType("token")
                .callback("fitbit://logincallback")
                .apiSecret(Aware.getSetting(getApplicationContext(), Settings.API_SECRET_PLUGIN_FITBIT))
                .build(FitbitAPI.instance());

        /*
        Intent auth = new Intent(Intent.ACTION_VIEW);
        auth.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        //erez
        //String url = Plugin.fitbitAPI.getAuthorizationUrl() + "&prompt=login%20consent";
        String url = Plugin.fitbitAPI.getAuthorizationUrl() + "&prompt=consent";
        auth.setData(Uri.parse(url));
        //auth.setData(Uri.parse(Plugin.fitbitAPI.getAuthorizationUrl()));

        auth.putExtra(Browser.EXTRA_APPLICATION_ID, getPackageName());

        startActivity(auth);
        */


        String url = Plugin.fitbitAPI.getAuthorizationUrl() + "&prompt=login%20consent";
        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
        //builder.setToolbarColor(Color.BLUE);
        //builder.setToolbarColor(0xff33b5e5);
        CustomTabsIntent customTabsIntent = builder.build();
        customTabsIntent.launchUrl(this, Uri.parse(url));
    }



    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (intent.getData() != null) {
            try {
                Uri URL_Fragment = intent.getData();
                String URL_Fragment_String = URL_Fragment.toString();

                // Retrieve information about access token.
                String access_Token = URL_Fragment_String.substring(URL_Fragment_String.indexOf("access_token=") + 13, URL_Fragment_String.indexOf("&user_id"));
                String data_scope = URL_Fragment_String.substring(URL_Fragment_String.indexOf("scope=") + 6, URL_Fragment_String.indexOf("&token_type"));
                String token_Type = URL_Fragment_String.substring(URL_Fragment_String.indexOf("token_type=") + 11, URL_Fragment_String.indexOf("&expires_in"));
                int expires_In = Integer.parseInt(URL_Fragment_String.substring(URL_Fragment_String.indexOf("expires_in=") + 11, URL_Fragment_String.length()));

                Aware.setSetting(this, Settings.OAUTH_TOKEN, access_Token);

                try {
                    JSONObject data_scopes = new JSONObject();
                    data_scopes.put("activity", data_scope.toLowerCase().contains("activity".toLowerCase()));
                    data_scopes.put("heartrate", data_scope.toLowerCase().contains("heartrate".toLowerCase()));
                    data_scopes.put("sleep", data_scope.toLowerCase().contains("sleep".toLowerCase()));
                    data_scopes.put("settings", data_scope.toLowerCase().contains("settings".toLowerCase()));

                    //the ones the user has accepted to share
                    Aware.setSetting(this, Settings.OAUTH_SCOPES, data_scopes.toString());

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                Aware.setSetting(this, Settings.OAUTH_TOKEN_TYPE, token_Type);
                Aware.setSetting(this, Settings.OAUTH_VALIDITY, expires_In);

                Plugin.fitbitOAUTHToken = new OAuth2AccessToken(Aware.getSetting(this, Settings.OAUTH_TOKEN),
                        Aware.getSetting(this, Settings.OAUTH_TOKEN_TYPE),
                        Integer.valueOf(Aware.getSetting(this, Settings.OAUTH_VALIDITY)),
                        "null",
                        Aware.getSetting(this, Settings.OAUTH_SCOPES),
                        "null");

                return;
            } catch(Exception e1) {
                e1.printStackTrace();
            }
        } else {
        }

        Plugin.fitbitOAUTHToken = null;
        Plugin.fitbitAPI = null;
        Plugin.devicesPicker = null;
        Aware.setSetting(this, Settings.OAUTH_TOKEN, "");
        Aware.setSetting(this, Settings.OAUTH_SCOPES, "");
        Aware.setSetting(this, Settings.OAUTH_TOKEN_TYPE, "");
        Aware.setSetting(this, Settings.OAUTH_VALIDITY, "");
    }
}
