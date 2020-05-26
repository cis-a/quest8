package de.telekom.sea.mystuff.frontend.android.api;

import android.content.Intent;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.jetbrains.annotations.NotNull;

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

        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient okHttpClient;
        // create OkHttp client
        okHttpClient = new OkHttpClient.Builder()
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


}
