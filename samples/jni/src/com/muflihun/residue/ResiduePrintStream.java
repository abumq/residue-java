package com.muflihun.residue;

import java.io.PrintStream;

public class ResiduePrintStream extends PrintStream {
    
    public ResiduePrintStream(PrintStream org) {
        super(org);
    }
    
    @Override
    public void println(Object line) {
        if (line == null) {
            Residue.getInstance().getDefaultLogger().info("NULL");
        } else {
            Residue.getInstance().getDefaultLogger().info(line.toString());
        }
        super.println(line);
    }
    
    @Override
    public void println(String line) {
        Residue.getInstance().getDefaultLogger().info(line);
        super.println(line);
    }
    
    @Override
    public void println(int line) {
        this.println(String.valueOf(line));
    }
    
    @Override
    public void println(double line) {
        this.println(String.valueOf(line));
    }
    
    @Override
    public void println(boolean line) {
        this.println(String.valueOf(line));
    }
    
    @Override
    public void println(char line) {
        this.println(String.valueOf(line));
    }
    
    @Override
    public void println(char[] line) {
        this.println(String.valueOf(line));
    }
    
    @Override
    public void println(float line) {
        this.println(String.valueOf(line));
    }
    
    @Override
    public void println(long line) {
        this.println(String.valueOf(line));
    }
    
    @Override
    public void print(Object line) {
        if (line == null) {
            Residue.getInstance().getDefaultLogger().info("NULL");
        } else {
            Residue.getInstance().getDefaultLogger().info(line.toString());
        }
        super.print(line);
    }
    
    @Override
    public void print(String line) {
        Residue.getInstance().getDefaultLogger().info(line);
        super.print(line);
    }
    
    @Override
    public void print(int line) {
        this.print(String.valueOf(line));
    }
    
    @Override
    public void print(double line) {
        this.print(String.valueOf(line));
    }
    
    @Override
    public void print(boolean line) {
        this.print(String.valueOf(line));
    }
    
    @Override
    public void print(char line) {
        this.print(String.valueOf(line));
    }
    
    @Override
    public void print(char[] line) {
        this.print(String.valueOf(line));
    }
    
    @Override
    public void print(float line) {
        this.print(String.valueOf(line));
    }
    
    @Override
    public void print(long line) {
        this.print(String.valueOf(line));
    }
}
