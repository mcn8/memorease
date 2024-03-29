package com.getmemorease.memorease;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.text.format.Time;
import android.view.View;

import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.ArrayList;
import java.util.Calendar;

import it.gmariotti.cardslib.library.internal.Card;
import it.gmariotti.cardslib.library.internal.CardArrayAdapter;
import it.gmariotti.cardslib.library.internal.CardHeader;
import it.gmariotti.cardslib.library.view.CardListView;

/**
 * Created by Tommy on 7/22/2014.
 */
public class MemorizeCard extends Card {

    protected String mTitleHeader;
    protected String mTitleMain;
    private Time nextTimer;
    private int currentLevel;
    private String objectId;
    private Context _context;
    private SingletonCardList cards;
    private AlarmService as;

    public MemorizeCard(Context context, String titleHeader, int currentLevel, Time startTime, Boolean newTime) {
        //add to db
        super(context);
        Time currentTime = new Time();
        currentTime.setToNow();
        this._context = context;
        this.mTitleHeader = titleHeader;
        this.currentLevel = currentLevel;
        this.nextTimer = new Time();
        this.nextTimer = this.timeOfNextTimer(currentLevel, startTime);
        if (!newTime) {
            this.nextTimer.set(startTime);
        }
        this.updateTime(currentTime);
        init();

        Bundle extras = new Bundle();
        extras.putString("item", mTitleHeader);
        extras.putString("objectId", objectId);
        extras.putLong("time", nextTimer.normalize(false));

        this.as = new AlarmService(context, extras);
        this.as.startAlarm();

        this.cards = SingletonCardList.getInstance();
        this.cards.add(this);
        //need to figure out how to push at certain time
        //pushNotification();
    }

    private void pushNotification() {
        int notificationId = 001;

        Intent memorizeScreen = new Intent();
        memorizeScreen.setClassName("com.getmemorease.memorease", "com.getmemorease.memorease.MemorizeCardActivity");
        Bundle extras = new Bundle();
        extras.putString("item", mTitleHeader);
        extras.putBoolean("dismiss", false);
        extras.putString("objectId", objectId);
        memorizeScreen.putExtras(extras);
        memorizeScreen.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent viewPendingIntent =
                PendingIntent.getActivity(getContext(), 0, memorizeScreen,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);

        // Create an intent for the reply action
        Intent actionIntent = new Intent();
        actionIntent.setClassName("com.getmemorease.memorease", "com.getmemorease.memorease.MemorizeCardActivity");
        Bundle actionExtras = new Bundle();
        actionExtras.putString("item", mTitleHeader);
        actionExtras.putBoolean("dismiss", true);
        actionExtras.putString("objectId", objectId);
        actionIntent.putExtras(actionExtras);
        actionIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent actionPendingIntent =
                PendingIntent.getActivity(getContext(), 0, actionIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_ONE_SHOT);

        // Create the action
        NotificationCompat.Action action =
                new NotificationCompat.Action.Builder(R.drawable.ic_circle,
                        "Got it!", actionPendingIntent)
                        .build();

        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(getContext())
                        .setSmallIcon(R.drawable.ic_circle)
                        .setContentTitle("Memorease")
                        .setContentText("1 card ready for memorization")
                        .setContentIntent(viewPendingIntent)
                        .extend(new NotificationCompat.WearableExtender().addAction(action));

        // Create a big text style for the second page
        NotificationCompat.BigTextStyle secondPageStyle = new NotificationCompat.BigTextStyle();
        secondPageStyle.setBigContentTitle("Page 2")
                .bigText(this.mTitleHeader);

        // Create second page notification
        Notification secondPageNotification =
                new NotificationCompat.Builder(getContext())
                        .setStyle(secondPageStyle)
                        .build();

        // Add second page with wearable extender and extend the main notification
        Notification twoPageNotification =
                new NotificationCompat.WearableExtender()
                        .addPage(secondPageNotification)
                        .extend(notificationBuilder)
                        .build();

        twoPageNotification.flags |= Notification.FLAG_AUTO_CANCEL;

        // Get an instance of the NotificationManager service
        NotificationManagerCompat notificationManager =
                NotificationManagerCompat.from(getContext());

        // Build the notification and issues it with notification manager.
        notificationManager.notify(notificationId, twoPageNotification);
    }

    private void init() {

        //Create a CardHeader
        CardHeader header = new CardHeader(_context);

        //Set the header title
        header.setTitle(this.mTitleHeader);

        addCardHeader(header);

        setSwipeable(true);

        setOnSwipeListener(new Card.OnSwipeListener() {
            @Override
            public void onSwipe(Card card) {
                ParseQuery<ParseObject> query = ParseQuery.getQuery("Card");
                query.getInBackground(objectId, new GetCallback<ParseObject>() {
                    public void done(ParseObject object, ParseException e) {
                        if (e == null) {
                            object.deleteInBackground();
                        } else {
                            ParseQuery<ParseObject> query = ParseQuery.getQuery("Card");
                            query.fromLocalDatastore();
                            query.getInBackground(objectId, new GetCallback<ParseObject>() {
                                public void done(ParseObject object, ParseException e) {
                                    if (e == null) {
                                        object.deleteInBackground();
                                    } else {
                                        // something went wrong
                                    }
                                }
                            });
                        }
                    }
                });

                as.cancel();
                cards.remove(card);
            }
        });

        setOnClickListener(new OnCardClickListener() {
            @Override
            public void onClick(Card card, View view) {
                gotoMemorizationPage();
            }
        });

        setClickable(false);

        //Set the card inner text
        setTitle(this.mTitleMain);
    }

    public void updateTime(Time currentTime) {
        int updateTime;
        switch (this.currentLevel){
            case 1:
                updateTime = this.nextTimer.minute - currentTime.minute;
                if (updateTime == 0){
                    this.mTitleMain = "Time for memorization!";
                    this.setClickable(true);
                }
                else if (updateTime == 1){
                    this.mTitleMain = "  Time until next memorization: 1 minute";
                }else
                    this.mTitleMain = "  Time until next memorization: " + Integer.toString(updateTime) + " minutes";
                break;
            case 2:
                updateTime = this.nextTimer.minute - currentTime.minute;
                if (updateTime == 0){
                    this.mTitleMain = "Time for memorization!";
                    this.setClickable(true);
                }
                else if (updateTime == 1){
                    this.mTitleMain = "  Time until next memorization: 1 minute";
                }else
                    this.mTitleMain = "  Time until next memorization: " + Integer.toString(updateTime) + " minutes";
                break;
            case 3:
                updateTime = this.nextTimer.hour - currentTime.hour;
                if (updateTime == 0){
                    this.mTitleMain = "Time for memorization!";
                    this.setClickable(true);
                }
                else if (updateTime == 1){
                    this.mTitleMain = "  Time until next memorization: 1 hour";
                }else
                    this.mTitleMain = "  Time until next memorization: " + Integer.toString(updateTime) + " hours";
                break;
            case 4:
                updateTime = this.nextTimer.hour - currentTime.hour;
                if (updateTime == 0){
                    this.mTitleMain = "Time for memorization!";
                    this.setClickable(true);
                }
                else if (updateTime == 1){
                    this.mTitleMain = "  Time until next memorization: 1 hour";
                }else
                    this.mTitleMain = "  Time until next memorization: " + Integer.toString(updateTime) + " hours";
                break;
            case 5:
                updateTime = this.nextTimer.hour - currentTime.hour;
                if (updateTime == 0){
                    this.mTitleMain = "Time for memorization!";
                    this.setClickable(true);
                }
                else if (updateTime == 1){
                    this.mTitleMain = "  Time until next memorization: 1 hour";
                }else
                    this.mTitleMain = "  Time until next memorization: " + Integer.toString(updateTime) + " hours";
                break;
            case 6:
                updateTime = this.nextTimer.monthDay - currentTime.monthDay;
                if (updateTime == 0){
                    this.mTitleMain = "Time for memorization!";
                    this.setClickable(true);
                }
                else if (updateTime == 1){
                    this.mTitleMain = "  Time until next memorization: 1 day";
                }else
                    this.mTitleMain = "  Time until next memorization: " + Integer.toString(updateTime) + " days";
                break;
            case 7:
                updateTime = this.nextTimer.monthDay - currentTime.monthDay;
                if (updateTime == 0){
                    this.mTitleMain = "Time for memorization!";
                    this.setClickable(true);
                }
                else if (updateTime == 1){
                    this.mTitleMain = "  Time until next memorization: 1 day";
                }else
                    this.mTitleMain = "  Time until next memorization: " + Integer.toString(updateTime) + " days";
                break;
            case 8:
                updateTime = this.nextTimer.month - currentTime.month;
                if (updateTime == 0){
                    this.mTitleMain = "Time for memorization!";
                    this.setClickable(true);
                }
                else if (updateTime == 1){
                    this.mTitleMain = "  Time until next memorization: 1 month";
                }else
                    this.mTitleMain = "  Time until next memorization: " + Integer.toString(updateTime) + " months";
                break;
        }
    }

    private void gotoMemorizationPage() {
        ++this.currentLevel;

        Intent memorizeScreen = new Intent();
        memorizeScreen.setClassName("com.getmemorease.memorease", "com.getmemorease.memorease.MemorizeCardActivity");
        Bundle actionExtras = new Bundle();
        actionExtras.putString("item", mTitleHeader);
        actionExtras.putBoolean("dismiss", false);
        actionExtras.putString("objectId", objectId);
        memorizeScreen.putExtras(actionExtras);
        memorizeScreen.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getContext().startActivity(memorizeScreen);
    }

    private Time timeOfNextTimer(int level, Time currentTime){
        Time newTime = new Time();
        newTime.set(currentTime);
        String titleAddition = "";
        switch (level){
            case 1:
                newTime.minute += 2;
                titleAddition = "2 minutes";
                break;
            case 2:
                newTime.minute += 10;
                titleAddition = "10 minutes";
                break;
            case 3:
                newTime.hour += 1;
                titleAddition = "1 hour";
                break;
            case 4:
                newTime.hour += 5;
                titleAddition = "5 hours";
                break;
            case 5:
                newTime.hour += 24;
                titleAddition = "1 day";
                break;
            case 6:
                newTime.monthDay += 5;
                titleAddition = "5 days";
                break;
            case 7:
                newTime.monthDay += 25;
                titleAddition = "25 days";
                break;
            case 8:
                newTime.month += 4;
                titleAddition = "4 months";
                break;
        }
        this.mTitleMain = "  Time until next memorization: " + titleAddition;
        return newTime;
    }

    public Time getNextTimer() {
        return this.nextTimer;
    }

    public void setNextTimer(Time nextTimer) {
        this.nextTimer = nextTimer;
    }

    public String getmTitleHeader() {
        return this.mTitleHeader;
    }

    public int getCurrentLevel() {
        return this.currentLevel;
    }

    public String getObjectId() {
        return this.objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

}