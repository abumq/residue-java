package com.muflihun.silencer;

import org.zuhd.residue.Residue;

public class Loggers {

    public static final Residue.Logger defaultLogger = Residue.getInstance().getLogger("default");
    public static final Residue.Logger sampleAppLogger = Residue.getInstance().getLogger("sample-app");
    public static final Residue.Logger nonexistentLogger = Residue.getInstance().getLogger("nonexistent");
}
