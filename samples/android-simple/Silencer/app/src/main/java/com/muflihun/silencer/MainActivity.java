package com.muflihun.silencer;

import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;

import com.muflihun.residue.Residue;


public class MainActivity extends AppCompatActivity {

    public void showMessageIfNotConnected(View view) {

        if (!Residue.getInstance().isConnected()) {
            Snackbar.make(view, "Not connected!", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        }
    }

    public Residue.Logger getLogger() {

        final RadioButton defaultLoggerOption = (RadioButton) findViewById(R.id.radioButton);
        final RadioButton sampleAppLoggerOption = (RadioButton) findViewById(R.id.radioButton2);

        if (defaultLoggerOption.isChecked()) {
            return Loggers.defaultLogger;
        }
        if (sampleAppLoggerOption.isChecked()) {
            return Loggers.sampleAppLogger;
        }

        return Loggers.nonexistentLogger;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final EditText editText = (EditText) findViewById(R.id.logMsg);

        Button btnInfo = (Button) findViewById(R.id.logI);
        btnInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showMessageIfNotConnected(view);
                getLogger().info(editText.getText().toString());
            }
        });

        Button btnDebug = (Button) findViewById(R.id.logD);
        btnDebug.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                showMessageIfNotConnected(view);
                getLogger().debug(editText.getText().toString());
            }
        });

        Button btnVerbose3 = (Button) findViewById(R.id.logV3);
        btnVerbose3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                showMessageIfNotConnected(view);
                getLogger().verbose(editText.getText().toString(), 3);
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
            setContentView(R.layout.activity_connect);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
