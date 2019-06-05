package com.foxfire.user.APICALL.UserData;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class UserData {

    @Expose
    @SerializedName("master_password")
    public String master_password;
    @Expose
    @SerializedName("master_id")
    public String master_id;
    @Expose
    @SerializedName("lock_password")
    public String lock_password;
    @Expose
    @SerializedName("imei")
    public String imei;
    @Expose
    @SerializedName("user_password")
    public String user_password;
    @Expose
    @SerializedName("name")
    public String name;
    @Expose
    @SerializedName("email")
    public String email;
    @Expose
    @SerializedName("id")
    public int id;
}
