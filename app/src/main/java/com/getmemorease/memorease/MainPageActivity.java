package com.getmemorease.memorease;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.format.Time;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.Parse;
import com.parse.ParseACL;
import com.parse.ParseAnonymousUtils;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.parse.SignUpCallback;
import com.parse.ui.ParseLoginBuilder;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import it.gmariotti.cardslib.library.internal.Card;
import it.gmariotti.cardslib.library.internal.CardArrayAdapter;
import it.gmariotti.cardslib.library.view.CardListView;


public class MainPageActivity extends Activity {

    private SingletonCardList cards;
    private CardArrayAdapter mCardArrayAdapter;

    public Activity getActivity(){
        return MainPageActivity.this;
    }

    public static Context getContext(){
        return MainPageActivity.getContext();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_page);
        this.cards = SingletonCardList.getInstance();
        this.cards.clear();

        this.mCardArrayAdapter = new CardArrayAdapter(getActivity(), cards.getCardList());
        this.mCardArrayAdapter.setEnableUndo(true);
        this.mCardArrayAdapter.setInnerViewTypeCount(1);

        CardListView listView = (CardListView) getActivity().findViewById(R.id.myList);
        if (listView != null) {
            listView.setAdapter(this.mCardArrayAdapter);
        }

        if (ParseAnonymousUtils.isLinked(ParseUser.getCurrentUser())) {
            ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Card");
            query.whereEqualTo("user", ParseUser.getCurrentUser());
            query.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> parseObjects, ParseException e) {
                    if (e == null) {
                        if (parseObjects != null) {
                            try {
                                ParseObject.pinAll(parseObjects);
                            } catch (ParseException e1) {
                                e1.printStackTrace();
                            }
                        }
                    }

                    ParseQuery<ParseObject> localQuery = new ParseQuery<ParseObject>("Card");
                    localQuery.fromLocalDatastore();
                    localQuery.findInBackground(new FindCallback<ParseObject>() {
                        @Override
                        public void done(List<ParseObject> parseObjects, ParseException e) {
                            if (parseObjects != null) {
                                for (ParseObject parseObject : parseObjects) {
                                    Time cardTime = new Time();
                                    cardTime.set(Long.valueOf(parseObject.getString("time")));
                                    MemorizeCard card = new MemorizeCard(getApplicationContext(), parseObject.getString("info"), parseObject.getInt("level"), cardTime, false);
                                    card.setObjectId(parseObject.getObjectId());
                                    parseObject.put("user", ParseUser.getCurrentUser().toString());
                                    parseObject.saveEventually();
                                    //setNotification(card.getNextTimer());
                                }
                            }

                            mCardArrayAdapter.notifyDataSetChanged();
                        }
                    });
                }
            });
        }else {
            ParseQuery<ParseObject> localQuery = new ParseQuery<ParseObject>("Card");
            localQuery.fromLocalDatastore();
            localQuery.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> parseObjects, ParseException e) {
                    if (parseObjects != null) {
                        for (ParseObject parseObject : parseObjects) {
                            Time cardTime = new Time();
                            cardTime.set(Long.valueOf(parseObject.getString("time")));
                            MemorizeCard card = new MemorizeCard(getApplicationContext(), parseObject.getString("info"), parseObject.getInt("level"), cardTime, false);
                            card.setObjectId(parseObject.getObjectId());
                            parseObject.put("user", ParseUser.getCurrentUser().toString());
                            parseObject.saveEventually();
                            //setNotification(card.getNextTimer());
                        }
                    }

                    mCardArrayAdapter.notifyDataSetChanged();
                }
            });
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        this.mCardArrayAdapter = new CardArrayAdapter(getActivity(), cards.getCardList());
        this.mCardArrayAdapter.setEnableUndo(true);
        this.mCardArrayAdapter.setInnerViewTypeCount(1);

        CardListView listView = (CardListView) getActivity().findViewById(R.id.myList);
        if (listView != null) {
            listView.setAdapter(this.mCardArrayAdapter);
        }

        this.cards.updateTimes();
        this.mCardArrayAdapter.notifyDataSetChanged();
        setTitle(ParseUser.getCurrentUser().getObjectId());
    }

    private void deleteAll() throws ParseException {
        ParseQuery<ParseObject> localQuery = new ParseQuery<ParseObject>("Card");
        localQuery.fromLocalDatastore();
        List<ParseObject> parseObjects = localQuery.find();
        ParseObject.deleteAll(parseObjects);
    }

    /*private void setNotification(Time time) {
        Calendar calendar = Calendar.getInstance();

        calendar.set(Calendar.MONTH, time.month);
        calendar.set(Calendar.YEAR, time.year);
        calendar.set(Calendar.DAY_OF_MONTH, time.monthDay);

        calendar.set(Calendar.HOUR_OF_DAY, time.hour);
        calendar.set(Calendar.MINUTE, time.minute);
        calendar.set(Calendar.SECOND, time.second);

        Intent myIntent = new Intent(getActivity(), AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getActivity(), 0, myIntent, 0);

        AlarmManager alarmManager = (AlarmManager)getSystemService(ALARM_SERVICE);
        alarmManager.set(AlarmManager.RTC, calendar.getTimeInMillis(), pendingIntent);
    }*/

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.my, menu);
        if (ParseUser.getCurrentUser() != null) {
            if (ParseAnonymousUtils.isLinked(ParseUser.getCurrentUser()))
                menu.findItem(R.id.action_login).setTitle("Login");
            else
                menu.findItem(R.id.action_login).setTitle("Logout");
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        } else if(id == R.id.action_login){
            if (ParseUser.getCurrentUser() != null) {
                if (ParseAnonymousUtils.isLinked(ParseUser.getCurrentUser())) {
                    ParseLoginBuilder builder = new ParseLoginBuilder(getActivity());
                    startActivity(builder.build());
                    invalidateOptionsMenu();
                }
                else {
                    ParseUser.logOut();
                    invalidateOptionsMenu();
                }
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == 0) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                ParseQuery<ParseObject> localQuery = new ParseQuery<ParseObject>("Card");
                localQuery.fromLocalDatastore();

                localQuery.findInBackground(new FindCallback<ParseObject>() {
                    @Override
                    public void done(List<ParseObject> parseObjects, ParseException e) {
                        if (parseObjects != null) {
                            for (ParseObject parseObject : parseObjects) {
                                parseObject.put("user", ParseUser.getCurrentUser().toString());
                                parseObject.saveEventually();
                            }
                        }
                        mCardArrayAdapter.notifyDataSetChanged();
                    }
                });

                setTitle(ParseUser.getCurrentUser().toString());
            }
        }
    }

    public void addOnClick(View view) {
        final EditText input = new EditText(this);
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.popupAddItem);
        builder.setView(input);
        builder.setPositiveButton(R.string.popupOkayButton, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                if (!input.getText().toString().isEmpty()) {
                    Time now = new Time();
                    now.setToNow();

                    final MemorizeCard card = new MemorizeCard(getActivity(), input.getText().toString(), 1, now, true);
                    //setNotification(card.getNextTimer());

                    ParseObject parseCard = new ParseObject("Card");
                    parseCard.put("user", ParseUser.getCurrentUser());
                    parseCard.put("info", card.getmTitleHeader());
                    parseCard.put("level", card.getCurrentLevel());
                    parseCard.put("time", Long.toString(card.getNextTimer().normalize(false)));

                    parseCard.saveEventually();

                    card.setObjectId(parseCard.getObjectId());

                    mCardArrayAdapter.notifyDataSetChanged();
                }
                 else {
                    Toast.makeText(getApplicationContext(), "Please enter an item", Toast.LENGTH_SHORT).show();
                }
            }
        }).setNegativeButton(R.string.popupCancelButton, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Do nothing.
            }
        });
        AlertDialog dialog = builder.show();
        TextView messageText = (TextView)dialog.findViewById(android.R.id.message);
        messageText.setGravity(Gravity.CENTER);
        dialog.show();
        dialog.getWindow().setLayout(450, 350);
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    }
}