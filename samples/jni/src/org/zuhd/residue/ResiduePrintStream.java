/**
 * ResiduePrintStream.java
 *
 * Official Java client library for Residue logging server
 *
 * Copyright (C) 2017-present Amrayn Web Services
 *
 * https://muflihun.com
 * https://amrayn.com
 * https://github.com/amrayn/residue-java
 *
 * Author: @abumusamq
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.amrayn.residue;

import java.io.PrintStream;

/**
 * Print stream that sends to the residue server
 */
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
