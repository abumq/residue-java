package com.muflihun.silencer;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.muflihun.residue.Residue;
import com.muflihun.residue.ResidueConnectTask;

import java.util.HashMap;

public class ConnectActivity extends AppCompatActivity {

    public void connect() {
        final EditText portText = (EditText) findViewById(R.id.textPort);
        final EditText hostText = (EditText) findViewById(R.id.textHost);
        Residue r = Residue.getInstance();
        System.out.println("reconnect()...");

        r.setAccessCodeMap(new HashMap<String, String>() {{
            put("sample-app", "eif89");
        }});

        r.setHost(hostText.getText().toString(), Integer.valueOf(portText.getText().toString()));

        try {
            r.loadConfigurationsFromJson("{ \"url\": \"localhost:8777\", \"access_codes\": [ { \"logger_id\": \"sample-app\", \"code\": \"a2dcb\" },{ \"logger_id\": \"default\", \"code\": \"blah\" } ], \"application_id\": \"com.muflihun.residue.sampleapp\", \"rsa_key_size\": 2048, \"utc_time\": false, \"time_offset\": 0, \"dispatch_delay\": 1, \"main_thread_id\": \"Main Thread\", \"server_public_key\": \"samples/clients/netcat/server-1024-public.pem\", \"client_id\": \"muflihun00102030\", \"client_private_key\": \"samples/clients/netcat/client-256-private.pem\" }");
        } catch (Exception e) {
            e.printStackTrace();
        }
        new ResidueConnectTask().execute();
    }

    public void gotoMain() {
        Intent Intent = new Intent(this, MainActivity.class);
        startActivity(Intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        Button btnReconnect = (Button) findViewById(R.id.btnReconnect);
        btnReconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connect();
                gotoMain();
            }
        });

        Button btnCancel = (Button) findViewById(R.id.button3);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                gotoMain();
            }
        });
    }

}
