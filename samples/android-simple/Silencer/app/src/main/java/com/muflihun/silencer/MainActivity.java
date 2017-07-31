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
import android.widget.Button;
import android.widget.EditText;

import com.muflihun.Residue;

import java.util.HashMap;


public class MainActivity extends AppCompatActivity {

    private class ResidueConnectTask extends AsyncTask<Object, Object, Void> {
        protected Void doInBackground(Object... urls) {

            try {
                Residue.getInstance().connect();

            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    public void connect() {
        final EditText portText = (EditText) findViewById(R.id.textPort);
        final EditText hostText = (EditText) findViewById(R.id.textHost);
        Residue r = Residue.getInstance();

        r.setAccessCodeMap(new HashMap<String, String>() {{
            put("sample-app", "eif89");
        }});

        r.setHost(hostText.getText().toString(), Integer.valueOf(portText.getText().toString()));
        new ResidueConnectTask().execute();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        connect();

        Button btnReconnect = (Button) findViewById(R.id.btnReconnect);
        btnReconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connect();
            }
        });

        final EditText editText = (EditText) findViewById(R.id.logMsg);

        Button btnInfo = (Button) findViewById(R.id.logI);
        btnInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (Residue.getInstance().isConnected()) {

                    Snackbar.make(view, "Sending...", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                } else {

                    Snackbar.make(view, "Not connected!", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                }

                final Residue.Logger logger = Residue.getInstance().getLogger("default");
                logger.info(editText.getText().toString());
            }
        });

        Button btnDebug = (Button) findViewById(R.id.logD);
        btnDebug.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (Residue.getInstance().isConnected()) {

                    Snackbar.make(view, "Sending...", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                } else {

                    Snackbar.make(view, "Not connected!", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                }

                final Residue.Logger logger = Residue.getInstance().getLogger("default");
                logger.debug(editText.getText().toString());
            }
        });

        Button btnVerbose3 = (Button) findViewById(R.id.logV3);
        btnVerbose3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (Residue.getInstance().isConnected()) {

                    Snackbar.make(view, "Sending...", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                } else {

                    Snackbar.make(view, "Not connected!", Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                }

                final Residue.Logger logger = Residue.getInstance().getLogger("default");
                logger.verbose(editText.getText().toString(), 3);
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
