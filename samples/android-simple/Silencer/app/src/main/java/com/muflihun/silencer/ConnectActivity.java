package com.muflihun.silencer;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.amrayn.residue.Residue;
import com.amrayn.residue.ResidueConnectTask;

import java.util.HashMap;

public class ConnectActivity extends AppCompatActivity {

    public void connect() {
        final EditText portText = (EditText) findViewById(R.id.textPort);
        final EditText hostText = (EditText) findViewById(R.id.textHost);
        Residue r = Residue.getInstance();
        System.out.println("reconnect()...");

        if (true) {
            // unencrypted private key
            r.setClientId("muflihun00102030");
            r.setPrivateKeyPEM("-----BEGIN RSA PRIVATE KEY-----\n" +
                    "MIIEpAIBAAKCAQEA7FRIrSdEHB0sLds0sHzdZtSeN6RUHtvPRhjk9wWThhq7b0OV\n" +
                    "BToWLHip9Jrwg69sVn8MtYaYbd9KtZSA9rHdYOEpplunphNwzq9BEQMsqs2ELFr6\n" +
                    "Eh1dwIPH2UcOeyd2W0OFYYjLdDXOUrgBz8LEliP1c0IMKc8gU4Z1welgDn60I4r6\n" +
                    "nVoeMBRR95xkcUyFuJ1Rw3Gg6z7cFYYqseJNGF5fguL0gqoBM+ZaZUieINx+NieW\n" +
                    "hzTdICcxXEIGb4m81edAo2HSif2q6777LUoYWefuZudbHyM5NtUZzBXwETXLArvK\n" +
                    "lOcdayIRKVfb/Fz7a/BRo0yG/rl/rjhPWzcTqQIBAwKCAQEAnY2FyMTYEr4dc+d4\n" +
                    "daiTmeMUJRg4FJKKLrtDT1kNBBHSSi0OA3wOyFBxTbygV8pIOaoIeQRlnpTceQ2r\n" +
                    "TyE+QJYbxD0abregicorYKzIcd5YHZH8DBOT1a0v5i9e/MT5ki0DllsyTXk0NyVW\n" +
                    "ioHYZBf494FdcTTAN675K/DqtFMwEGG5gk+ycReZeXsxCw8rAJedkE8I1KTs9wuH\n" +
                    "w10XK179PVc8JZqAH0elPRjfJ5Cq6bUl4Dd3rjbCd/OHw6pbb0Fac2Gsv4x3D/e0\n" +
                    "1dEdqgDLhJltnj0sZWCsQAm5uE3DuKCIgqkNJn5LPgSgZkuvxY/wtKkZzJJov4HH\n" +
                    "PAWagwKBgQD7JU1/Ib2aI0GwaB8kEvfSk65ZJ4HHnmGBiBM0cMqhvC74x4pTuddh\n" +
                    "wZyjtWMoumxD5pL+o874OyRLtwk3ArT1EJsrhRhXgWNyt3F9rXDO5mNMgRgRel5h\n" +
                    "El0dBPXzAIIstvhwL8raeC2WA9TCZSIstIHWDnu1jOsU41EeN5FFgwKBgQDw5arl\n" +
                    "OCT4Yy87WUR2zb3ypAuMQ3lMDeX3GuGqnAwImyDq31XGkGrfQ14+1EAmqxc58QTf\n" +
                    "ExKxX7DB8UuDx1U+nmoNcTb36UeFnFnuPx+c9INwnuklN2kVjGb6ZxFmfD74ttKN\n" +
                    "oR6vOTcKSHwo/clHDxaShdMqvvLNq6SGSZ1mYwKBgQCnbjOqFn5mwivK8BTCt0/h\n" +
                    "t8mQxQEvvuursAzNoIcWfXSl2lw30TpBK73CeOzF0Z2CmbdUbTSlfMLdJLDPVyNO\n" +
                    "CxIdA2WPq5ehz6D+c6CJ7uzdq2Vg/D7rYZNorflMqwFzJKWgH9yRpXO5V+MsQ2wd\n" +
                    "zavkCafOXfIN7OC+z7YuVwKBgQCgmRyY0Bill3TSO4L53n6hwrJdglDdXplPZ0Ec\n" +
                    "aAgFvMCclOPZtZyU15Qp4tVvHLomoK3qDLcg6nXWoN0ChON/FEazoM9P8NpZEuae\n" +
                    "1L+9+FegafDDekYOXZn8RLZEUtSlzzcJFhR00M9cMFLF/oYvX2RhrozHKfczx8ME\n" +
                    "MROZlwKBgQCmPgUCediRMlRrtYiLhsHhLJ11fSPDMcDLAn1CyNRqbUv2vm77rqf8\n" +
                    "SpzrO1MV7+Myv7mmnLwleG3jROKaB8zUHPqLQaeIV9M3dQs/iaPsu5NQ63TRzDK8\n" +
                    "Azh5HkbYBzmoDATt2QoQmenwcW0sBpaEgKsnUe5WozZJeizQ8d8igA==\n" +
                    "-----END RSA PRIVATE KEY-----\n");
        }

        if (false) {

            // encrypted private key
            r.setClientId("muflihun00102031");
            r.setPrivateKeySecret("8583fFir");
            r.setPrivateKeyPEM("-----BEGIN RSA PRIVATE KEY-----\n" +
                    "Proc-Type: 4,ENCRYPTED\n" +
                    "DEK-Info: AES-256-CBC,4F3AD18C72BB21B75726984FCD5538FE\n" +
                    "\n" +
                    "Qq/e3549YWPrunQ7XwSWpzxR+V2jEwDtoxgFinbHrI7WQEtbmYh68GN+VGzarJMv\n" +
                    "Et1M9KEKrAQbmPUSaYuZVwhnLAi/No0SNRKaA8LzuetJBqXbCZJlaAXwT85ecu5L\n" +
                    "GGAdfVr1ONGLjje8cawythvwhryhGxXbQ+RP9E6OG6kRsK1k/xsetq113Q8b9A6L\n" +
                    "IOa4ZbQMSSkmIkBxgJYd5QaIQbKj3pXZ92kSaMu0XLi5OnGzel8wsofFj+xtMGrR\n" +
                    "KYfR8wPstL2YjpeIioR2JUEtIoqRMaqdYHusBvOdHIRn5YVcQuYCkA0HUAb/kROr\n" +
                    "ZJHxZ8lURDRbHkDBxS6Tp/lXs6lSCibtXkjCMr0DscAzYeMAeheYJgPnKuEZtxoX\n" +
                    "Yp7PBE/0ywrNrjPgCpyoYl1GiGOsvYX1Sp+ksDFE1olKWxJyfvZu/ssI6W/KrXiJ\n" +
                    "BgzXcFGqmwv9NSoFCix2MvEwUJOlfPVfoZ0pCOgEEumLyhLjvwm6P83si2hGQnlj\n" +
                    "qJI0/T9fU5hxqqntBs5Z9cgvDFt8QAy5/a8NA2KQnn35rzjPupGHCPPi37NAjfeX\n" +
                    "zLj6nc6bfdtYxGyKSJkcGxs3J+2DHIES5igMmIiwpPiFY+rm+1jl2zx/GlVNovRC\n" +
                    "x3JVBgUIGXW3R/lGLAwowRVsfLZcM7hm+RGORkT2EjLqfQt0WuBOlgXV+5QIkLKF\n" +
                    "Nu2jgF+u09qs1724gO7KmnUTBdSxrDgNj8iE8OMaQ7YarVsjbj84+219xBo7WlX2\n" +
                    "05Hcy7zEkExrMadbQUXvKoi9JVYmpFGp8sC29wjTMyX7pBVTtQ8NE+oBe2Od0kbA\n" +
                    "bFsZ8MSpLEeUtYVgKdT6ArwTlOJ158sZ8F52USD0oK6TXwG+DtNw3vOZiSUib0h5\n" +
                    "S2KsAhxOn0MMSnILwN9VzgpXIbgkvn0qF/dGExeN6ny0ma7lUmaZCT/NlDHHQds5\n" +
                    "GGHLVpsqjEGAoajapsUtw6f4DViV14pk2y89bAIy9IkEpsvnpCD/OoHP+D0IKrNN\n" +
                    "I0NmDyyEMAC8Ypx7tzVT7SAj85QKcfDwTl16f6Ln2k1YYzmBfd7GMjFVrpw7vv8N\n" +
                    "9NaOZFswfmZCJdo5vREK6EN+0/cYJ9c/xeZqyywZvCCgr89XeC5qbUlnysVSnPAh\n" +
                    "FBpZ7uo+eb4Nhp1pUyALCAN4fwghBLnDSRl7HdghckkD54dp4z0ypHPgiK3Oml06\n" +
                    "PJ2HZB7MrPaggFfYxl3QyyA5x7pAglifzWpO1Mrq7Qe2mM6199v3lcPj0R37ZK/i\n" +
                    "YXm0y8527VlI7Lv3oTvk3inJgwR5F0vNVXQG3NKbdvQ1/nGpXmtwcIR2QJQXzS+e\n" +
                    "oKYcRtPL3m2/bEnrmAcXce5YsnQWhDwvZQDt63E/LviCw5V+kikAGZOeCCX1ctTY\n" +
                    "1wquJS6Fgw1c9z1b0r+0ttgnHRpIhW3Io826MoObk0qkSTwan4cNe8l/OHPbB1Iq\n" +
                    "4jOx9U2cYhuE+qL8oeOcJQAY/YK4rXReadzjCb7PZgySrXn3dcrZNtSOjUpACG7g\n" +
                    "hR0CS7SeVo6EhIQhzFzAfiX2TGxZ1YksSCogAjx2aavWXF+AAzb/BamtXyQ+beJ/\n" +
                    "-----END RSA PRIVATE KEY-----");
        }

        r.setHost(hostText.getText().toString(), Integer.valueOf(portText.getText().toString()));
        r.setUtcTime(true);

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

        final EditText portText = (EditText) findViewById(R.id.textPort);
        final EditText hostText = (EditText) findViewById(R.id.textHost);

        if (Residue.getInstance().getHost() != null) {
            hostText.setText(Residue.getInstance().getHost());
        }
        if (Residue.getInstance().getPort() != null) {
            portText.setText(Residue.getInstance().getPort().toString());
        }

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
