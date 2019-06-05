package com.foxfire.user.APICALL;

import androidx.annotation.NonNull;

import com.foxfire.user.APICALL.UserData.UserData;
import com.foxfire.user.Notification.MyResponse;
import com.foxfire.user.Notification.Sender;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface APIInterface {
    //http://demoadhya.pythonanywhere.com/api/profile/{uid}/
    @GET("profile/{uid}")
    Call<UserData> getUserData(@Path("uid") int id);

    @NonNull
    @Headers({
            "Content-Type:application/json",
            "Authorization:key=AAAAfOQNgiw:APA91bFa8RP6NfChcC4NSS8M7TkUqkB2j9cIcTkibWn638mg2biaUcIEdi94r4Y4_MY18EZF4Q-e9bXMU9y58tJrkvVU8w-1t5Ml78FXjwUkboJfee42GFM-ATwvst7lxUVd6lar6gjI"
    })
    @POST("fcm/send")
    retrofit2.Call<MyResponse> sendNotification(@Body Sender body);

}
