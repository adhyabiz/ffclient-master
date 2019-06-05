package com.foxfire.user.Notification;

public class Notification {
    public String body, title, click_action;

    public Notification(String body, String title, String click_action) {
        this.body = body;
        this.title = title;
        this.click_action = click_action;
    }
}
