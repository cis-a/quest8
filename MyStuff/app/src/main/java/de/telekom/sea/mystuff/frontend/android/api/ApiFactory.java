package de.telekom.sea.mystuff.frontend.android.api;

import android.content.Intent;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.security.cert.Certificate;
import java.util.Collection;

import lombok.Getter;
import okhttp3.Authenticator;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import timber.log.Timber;


/**
 * Creates API instances for performing REST calls.
 */
public class ApiFactory {

    @Getter
    private final Retrofit retrofit;
    @Getter
    private final String baseRestUrl;
    private final String hostName;
    /**
     * place real IP address of backend machine here
     * needs a
     * <b>android:usesCleartextTraffic="true"</b>
     * in AndroidManifest.xml in section Application
     * if not choose https protocol
     */

    public ApiFactory(
            String hostName,
            String protocol,
            int port,
            String certificateValidationApproach,
            Collection<? extends Certificate> acceptedCustomCaCertificates) {

        this.baseRestUrl = protocol + "://" + hostName + ":" + port;
        this.hostName = hostName;

        TokenAuthenticator authenticator = new TokenAuthenticator(this);
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient okHttpClient;
        // create OkHttp client
        okHttpClient = new OkHttpClient.Builder()
                    .authenticator(authenticator)
                    .addInterceptor(loggingInterceptor)
                    .build();


        Gson gson = new GsonBuilder()
                .setLenient()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
                .create();

        retrofit = new Retrofit.Builder()
                .baseUrl(this.baseRestUrl)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .addCallAdapterFactory(new LiveDataCallAdapterFactory())
                .client(okHttpClient)
                .build();
    }

    /**
     * @param retrofitApiInterface defines the REST interface, must not be null
     * @param <S>
     * @return API instance for performing REST calls, never null
     */
    public <S> S createApi(Class<S> retrofitApiInterface) {
        return retrofit.create(retrofitApiInterface);
    }

    public String getBackendBaseUrl() {
        return this.baseRestUrl;
    }

    private static class TokenAuthenticator implements Authenticator {

        static final int HTTP_UNAUTHORIZED = 401;
        static final int MAX_RETRY = 5;
        private final ApiFactory apiFactory;
        String authToken = null;

        private TokenAuthenticator(ApiFactory factory) {
            this.apiFactory = factory;
        }

        @Override
        public Request authenticate(@Nullable Route route, @NotNull Response response) {

            int retry = responseCount(response);
            // retrieve authenticationApi and update refreshTokens
            AuthenticationApi authenticationApi = SmartCredentialsAuthenticationFactory.getAuthenticationApi();
            authenticationApi.performActionWithFreshTokens(new OnFreshTokensRetrievedListener() {

                @Override
                public void onRefreshComplete(@Nullable String accessToken, @Nullable String idToken, @Nullable AuthorizationException exception) {
                    if (exception != null) {
                        if ("Invalid refresh_token".equals(exception.getErrorDescription())) {
                            // Refresh token invalid - New login required!
                            Intent intent = new Intent();
                            intent.setAction(MayoBroadcastActions.MAYO_ON_REFRESH_TOKEN_INVALID.toString());
                            intent.putExtra(MayoBroadcastReceiver.KEY_EXTRA, exception.getErrorDescription());
                            TokenAuthenticator.this.apiFactory.broadcastSender.sendBroadcast(intent);
                        } else {
                            // Other token related problem
                            Timber.e("Retry %s: Authorization failed! %s", retry, exception.getErrorDescription());
                        }
                    } else {
                        // valid token available - use obtained token
                        if ("TELEKOM".equalsIgnoreCase(TokenAuthenticator.this.apiFactory.identityProviderId)) {
                            authToken = accessToken;
                        } else {
                            authToken = idToken;
                        }

                        JwtConsumer jwtConsumer = new JwtConsumerBuilder()
                                .setSkipAllValidators()
                                .setSkipSignatureVerification().build();
                        try {
                            Timber.d("Retry %s: Token Expiration time = %s", retry, jwtConsumer.processToClaims(authToken).getExpirationTime());
                            Timber.d("Retry %s: AuthToken = %s", retry, authToken);
                        } catch (Exception e) {
                            Timber.e(e, "Retry %s: Invalid JWT of token!", retry);
                        }
                    }

                }

                @Override
                public void onFailed(AuthenticationError errorDescription) {
                    // this branch is never reached
                }
            });

            // Authentication is required , but we have no auth token:
            // Retry a few times; might be network problem during token refresh
            if (authToken == null) {
                Timber.e("Retry %s: Bearer authentication failed - no auth token available: %s", retry, response);
                if (retry > MAX_RETRY) {
                    return null;
                }
            } else {
                // We have an auth token; check what went wrong in the previous request:
                // 2) we failed already with the current authToken
                String authHeaderUsedInFormerRequest = response.request().header("Authorization");
                if ((response.code() == TokenAuthenticator.HTTP_UNAUTHORIZED) &&
                        (authHeaderUsedInFormerRequest != null) &&
                        authHeaderUsedInFormerRequest.equals("Bearer " + authToken)) {
                    Timber.e("Retry %s: Bearer authentication failed - token is not accepted by backend: %s", retry, response);
                    return null;
                }

                // 3) we had a non-authentication related error in the last call
                if (response.code() == 400 || response.code() > 401) {
                    Timber.e("Retry %s: Rest call failed, http response status <> 401: %s", retry, response);
                    return null;
                }
            }

            // return a new request with a fresh authToken
            return response.request().newBuilder()
                    .header("Authorization", "Bearer " + authToken)
                    .build();

        }

        private int responseCount(Response response) {
            int result = 1;
            while ((response = response.priorResponse()) != null) {
                result++;
            }
            return result;
        }
    }

}
