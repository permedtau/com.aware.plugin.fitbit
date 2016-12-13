package com.aware.plugin.Fitbit;

import android.util.Log;

import com.github.scribejava.core.builder.api.DefaultApi20;
import com.github.scribejava.core.model.AbstractRequest;
import com.github.scribejava.core.model.OAuth2AccessToken;
import com.github.scribejava.core.model.OAuthConfig;
import com.github.scribejava.core.model.OAuthConstants;
import com.github.scribejava.core.oauth.OAuth20Service;

/**
 * Created by sklakegg on 12/12/16.
 * No custom changed so far.
 */

public class FitbitOAuthServiceImpl extends OAuth20Service {

    public FitbitOAuthServiceImpl(DefaultApi20 api, OAuthConfig config) {
        super(api, config);
    }

    @Override
    public void signRequest(OAuth2AccessToken accessToken, AbstractRequest request) {
        request.addHeader("Authorization", "Bearer " + accessToken.getAccessToken());
    }
}
