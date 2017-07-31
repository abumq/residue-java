package com.muflihun.silencer;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import com.muflihun.Residue;

import java.util.HashMap;


public class MainActivity extends AppCompatActivity {

    private class ResidueConnectTask extends AsyncTask<Object, Object, Void> {
        protected Void doInBackground(Object... urls) {

            Residue r = Residue.getInstance();

            try {
                if (r.connect("192.168.1.103", 8777)) {

                } else {

                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        new ResidueConnectTask().execute();

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Residue r = Residue.getInstance();

                if (r.isConnected()) {

                    Snackbar.make(view, "Sending...", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                } else {

                    Snackbar.make(view, "Not connected!", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                }

                final Residue.Logger l = r.getLogger("default");
                l.info("Info message");
                l.debug("Debug message");
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_reconnect) {
            new ResidueConnectTask().execute();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
