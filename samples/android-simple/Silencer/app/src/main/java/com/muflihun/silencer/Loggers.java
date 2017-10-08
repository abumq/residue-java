package com.muflihun.silencer;

import com.muflihun.residue.Residue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Loggers {

    public static final Residue.Logger defaultLogger = Residue.getInstance().getLogger("default");
    public static final Residue.Logger sampleAppLogger = Residue.getInstance().getLogger("sample-app");
    public static final Residue.Logger nonexistentLogger = Residue.getInstance().getLogger("nonexistent");

    public static final Logger log4jSampleAppLogger = LogManager.getLogger("sample-app");
}
