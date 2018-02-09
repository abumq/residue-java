/**
 * ResidueConnectTask.java
 * <p>
 * Connect activity for android apps
 * Part of Residue Java client library
 * <p>
 * Copyright (C) 2017-present Muflihun Labs
 * <p>
 * https://muflihun.com
 * https://muflihun.github.io/residue
 * https://github.com/muflihun/residue-java
 * <p>
 * See https://github.com/muflihun/residue-java/blob/master/LICENSE
 * for licensing information
 * <p>
 * Author: @abumusamq
 */

package com.muflihun.residue;

import android.os.AsyncTask;

public class ResidueConnectTask extends AsyncTask<Object, Object, Void> {
    protected Void doInBackground(Object... urls) {

        try {
            Residue.getInstance().reconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
