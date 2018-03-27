/**
 * Residue.java
 *
 * Official Java client library for Residue logging server
 *
 * Copyright (C) 2017-present Muflihun Labs
 *
 * https://muflihun.com
 * https://muflihun.github.io/residue
 * https://github.com/muflihun/residue-java
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

package com.muflihun.residue;

import javax.crypto.Cipher;
import javax.crypto.EncryptedPrivateKeyInfo;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.ReadPendingException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayDeque;
import java.util.Calendar;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.zip.DeflaterOutputStream;

import java.util.logging.LogRecord;
import java.util.logging.Level;
import java.util.logging.Handler;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;

/**
 * Provides residue interface for interacting with residue server seamlessly and sending the log messages
 * <p>
 * For the functions that do not have documentation, please refer to C++ library's documentation,
 * all the concepts and naming conventions are same for all the official residue client libraries
 *
 * @see <a href='https://muflihun.github.io/residue/'>C++ API Documentation </a>
 */
public class Residue {

    private static final Integer TOUCH_THRESHOLD = 60; // should always be min(client_age)
    private static final Integer ALLOCATION_BUFFER_SIZE = 4098;

    private final ResidueClient connectionClient = new ResidueClient();
    private final ResidueClient loggingClient = new ResidueClient();

    private final Deque<JsonObject> backlog = new ArrayDeque<>();
    private final Map<String, Logger> loggers = new HashMap<>();

    private String host;
    private Integer port;
    private Integer loggingPort;
    private String applicationName;
    private Integer rsaKeySize = 2048;
    private Integer keySize = 128;
    private Boolean utcTime = false;
    private Integer timeOffset = 0;
    private Boolean useTimeOffsetIfNotUtc = false;
    private Integer dispatchDelay = 1;
    private Boolean autoBulkParams = true;
    private Boolean bulkDispatch = false;
    private Integer bulkSize = 0;
    private String defaultLoggerId = "default";

    private String privateKeySecret;
    private String privateKeyFilename;
    private String privateKeyPEM;
    private PrivateKey privateKey;

    private String serverKeyFilename;
    private String serverKeyPEM;

    private boolean connected = false;
    private boolean connecting = false;

    private String serverVersion;
    private String key;
    private String clientId;
    private Integer age;
    private Date dateCreated;
    private Integer serverFlags;
    private Integer maxBulkSize;
    private volatile String lastError;
    private PrintStream printStream = new ResiduePrintStream(System.out);

    private static Residue instance;

    private Boolean isConnecting() {
        return connecting;
    }

    public static Residue getInstance() {
        if (instance == null) {
            instance = new Residue();
        }
        return instance;
    }

    private Residue() {
    }

    private boolean hasProvidedClientKey() {
        return (privateKeyFilename != null && !privateKeyFilename.isEmpty())
                || (privateKeyPEM != null && !privateKeyPEM.isEmpty());
    }

    private boolean hasProvidedServerKey() {
        return (serverKeyFilename != null && !serverKeyFilename.isEmpty())
                || (serverKeyPEM != null && !serverKeyPEM.isEmpty());
    }

    public String getServerVersion() {
        return serverVersion;
    }

    public PrintStream getPrintStream() {
        return printStream;
    }

    public String getHost() {
        return host;
    }

    public Integer getPort() {
        return port;
    }

    public String getLastError() {
        return lastError;
    }

    public Boolean isConnected() {
        return connected;
    }

    public void setTimeOffset(final Integer timeOffset) {
        this.timeOffset = timeOffset;
    }

    /**
     * Enables automatic setting of bulk parameters depending on
     * what's most efficient. It essentially enables bulk if server supports
     * it and sets bulk size and dispatch delay accordingly.
     *
     * note: You need re-connect using <pre>connect()</pre> helper method
     * note: By default it is enabled
     */
    public void setAutoBulkParams(final Boolean autoBulkParams) {
        this.autoBulkParams = autoBulkParams;
    }

    public void setDispatchDelay(final Integer dispatchDelay) {
        this.dispatchDelay = dispatchDelay;
    }

    public void setDefaultLoggerId(final String defaultLoggerId) {
        this.defaultLoggerId = defaultLoggerId;
    }

    public void setClientId(final String clientId) {
        this.clientId = clientId;
    }

    /**
     * Applicable to unknown clients only. Known clients already have public key on the server
     * <p>
     * Accepted values are 1024, 2048, 4096
     *
     * @throws IllegalArgumentException if invalid key size is provided
     */
    public void setRsaKeySize(final Integer rsaKeySize) {
        if (rsaKeySize == 0 || rsaKeySize % 1024 != 0) {
            throw new IllegalArgumentException("Accepted RSA key sizes are 1024, 2048, 4096");
        }
        this.rsaKeySize = rsaKeySize;
    }

    /**
     * Only change this if you know the client can support any bigger keys
     * Accepted values are 128, 192 and 256
     *
     * @throws IllegalArgumentException if invalid key size is provided
     */
    public void setKeySize(final Integer keySize) throws IllegalArgumentException {
        if (keySize != 128 && keySize != 192 && keySize != 256) {
            throw new IllegalArgumentException("Accepted key sizes are 128, 192 and 256");
        }
        this.keySize = keySize;
    }

    public void setPrivateKeyFilename(final String privateKeyFilename) {
        this.privateKeyFilename = privateKeyFilename;
    }

    public void setPrivateKeyPEM(final String privateKeyPEM) {
        this.privateKeyPEM = privateKeyPEM;
    }

    public void setPrivateKeySecret(final String privateKeySecret) {
        this.privateKeySecret = privateKeySecret;
    }

    public void setServerKeyFilename(final String serverKeyFilename) {
        this.serverKeyFilename = serverKeyFilename;
    }

    public void setServerKeyPEM(final String serverKeyPEM) {
        this.serverKeyPEM = serverKeyPEM;
    }

    public void setApplicationName(final String applicationName) {
        this.applicationName = applicationName;
    }

    public void setUtcTime(Boolean utcTime) {
        this.utcTime = utcTime;
    }

    /**
     * If this is true the timeOffset will only take affect if the
     * current timezone is NOT UTC.
     *
     * You must enable utcTime with this
     */
    public void setUseTimeOffsetIfNotUtc(Boolean useTimeOffsetIfNotUtc) {
        this.useTimeOffsetIfNotUtc = useTimeOffsetIfNotUtc;
    }

    public synchronized void loadConfigurations(final String jsonFilename) throws Exception {
        byte[] encoded = Files.readAllBytes(Paths.get(jsonFilename));
        String json = new String(encoded, Charset.defaultCharset());
        loadConfigurationsFromJson(json);
    }

    public synchronized void loadConfigurationsFromJson(final String json) throws Exception {
        JsonObject jsonObject = new Gson().fromJson(json, JsonObject.class);
        if (jsonObject.has("url")) {
            String[] parts = jsonObject.get("url").getAsString().split(":");
            if (parts.length == 2) {
                Integer port = Integer.parseInt(parts[1]);
                setHost(parts[0], port);
            }
        } else {
            throw new Exception("URL should be in format of <host>:<port>");
        }

        if (jsonObject.has("application_id")) {
            setApplicationName(jsonObject.get("application_id").getAsString());
        }

        if (jsonObject.has("utc_time")) {
            setUtcTime(jsonObject.get("utc_time").getAsBoolean());
        }

        if (jsonObject.has("use_timeoffset_if_not_utc")) {
            setUseTimeOffsetIfNotUtc(jsonObject.get("use_timeoffset_if_not_utc").getAsBoolean());
        }

        if (jsonObject.has("time_offset")) {
            setTimeOffset(jsonObject.get("time_offset").getAsInt());
        }

        if (jsonObject.has("rsa_key_size")) {
            setRsaKeySize(jsonObject.get("rsa_key_size").getAsInt());
        }

        if (jsonObject.has("dispatch_delay")) {
            setDispatchDelay(jsonObject.get("dispatch_delay").getAsInt());
        }

        if (jsonObject.has("key_size")) {
            setKeySize(jsonObject.get("key_size").getAsInt());
        }

        if (jsonObject.has("main_thread_id")) {
            Thread.currentThread().setName(jsonObject.get("main_thread_id").getAsString());
        }

        if (jsonObject.has("server_public_key")) {
            setServerKeyFilename(jsonObject.get("server_public_key").getAsString());
        }

        if (jsonObject.has("server_public_key_pem")) {
            setServerKeyPEM(jsonObject.get("server_public_key_pem").getAsString());
        }

        if (jsonObject.has("client_id")) {
            setClientId(jsonObject.get("client_id").getAsString());
        }

        if (jsonObject.has("client_private_key")) {
            setPrivateKeyFilename(jsonObject.get("client_private_key").getAsString());
        }

        if (jsonObject.has("client_private_key_pem")) {
            setPrivateKeyPEM(jsonObject.get("client_private_key_pem").getAsString());
        }

        if (jsonObject.has("client_key_secret")) {
            setPrivateKeySecret(jsonObject.get("client_key_secret").getAsString());
        }
    }

    /**
     * Sets number of log messages to be bulked together
     * This depends on what server accepts. The configuration value on the server is
     * <a href='https://github.com/muflihun/residue/blob/master/docs/CONFIGURATION.md#max_items_in_bulk'>max_items_in_bulk</a>.
     *
     * @param bulkSize
     * @throws IllegalArgumentException If already connected and you try to set size more than
     * maximum allowed
     * @see #setAutoBulkParams(Boolean)
     */
    public void setBulkSize(Integer bulkSize) throws IllegalArgumentException {
        if (isConnected() && bulkSize > maxBulkSize) {
            throw new IllegalArgumentException("Invalid bulk dispatch size. Maximum allowed: " + maxBulkSize);
        }
        this.bulkSize = bulkSize;
    }

    /**
     * Extension for Handler from Java logging API for using in existing projects
     *
     * Here is how levels map:
     * SEVERE => ERROR
     * WARNING => WARNING
     * INFO => INFO
     * CONFIG => DEBUG
     * FINE => VERBOSE level 3
     * FINER => VERBOSE level 5
     * FINEST => VERBOSE level 9
     */
    public static class ResidueLogHandler extends Handler {
        @Override
        public void publish(LogRecord record) {
            Residue.getInstance().log(record);
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() throws SecurityException {
        }
    }

    /**
     * Logger class to send log messages to the server
     */
    public static class Logger {

        private String id;

        private Logger() {
            this(getInstance().defaultLoggerId);
        }

        private Logger(String id) {
            this.id = id;
        }

        public boolean isDebugEnabled() {
            return true;
        }

        public boolean isInfoEnabled() {
            return true;
        }

        public boolean isWarnEnabled() {
            return true;
        }

        public boolean isErrorEnabled() {
            return true;
        }

        public void debug(Object obj) {
            log(obj, LoggingLevels.DEBUG);
        }

        public void info(Object obj) {
            log(obj, LoggingLevels.INFO);
        }

        public void error(Object obj) {
            log(obj, LoggingLevels.ERROR);
        }

        public void warn(Object obj) {
            log(obj, LoggingLevels.WARNING);
        }

        public void fatal(Object obj) {
            log(obj, LoggingLevels.FATAL);
        }

        public void trace(Object obj) {
            log(obj, LoggingLevels.TRACE);
        }

        public void debug(String format, Object... args) {
            if (isDebugEnabled()) {
                String message = String.format(format, args);

                log(message, LoggingLevels.DEBUG);
            }
        }

        public void debug(Throwable t, String format, Object... args) {
            if (isDebugEnabled()) {
                String message = String.format(format, args);

                log(message, t, LoggingLevels.DEBUG);
            }
        }

        public void debug(String message, Throwable throwable) {
            if (isDebugEnabled()) {
                log(message, throwable, LoggingLevels.DEBUG);
            }

        }

        public void info(String format, Object... args) {
            if (isInfoEnabled()) {
                String message = String.format(format, args);

                log(message, LoggingLevels.INFO);
            }
        }

        public void info(Throwable t, String format, Object... args) {
            if (isInfoEnabled()) {
                String message = String.format(format, args);

                info(message, t);
            }
        }

        public void info(String message, Throwable throwable) {
            if (isInfoEnabled()) {
                log(message, throwable, LoggingLevels.INFO);
            }
        }

        public void warn(String format, Object... args) {
            if (isWarnEnabled()) {
                String message = String.format(format, args);

                log(message, LoggingLevels.WARNING);
            }
        }

        public void warn(Throwable t, String format, Object... args) {
            if (isWarnEnabled()) {
                String message = String.format(format, args);

                warn(message, t);
            }
        }

        public void warn(String message, Throwable throwable) {
            if (isWarnEnabled()) {
                log(message, throwable, LoggingLevels.WARNING);
            }
        }

        public void error(String format, Object... args) {
            if (isErrorEnabled()) {
                String message = String.format(format, args);

                log(message, LoggingLevels.ERROR);
            }
        }

        public void error(Throwable t, String format, Object... args) {
            if (isErrorEnabled()) {
                String message = String.format(format, args);

                error(message, t);
            }
        }

        public void error(String message, Throwable throwable) {
            if (isErrorEnabled()) {
                log(message, throwable, LoggingLevels.ERROR);
            }
        }

        public void trace(String format, Object... args) {
            if (isErrorEnabled()) {
                String message = String.format(format, args);

                log(message, LoggingLevels.TRACE);
            }
        }

        public void trace(Throwable t, String format, Object... args) {
            if (isErrorEnabled()) {
                String message = String.format(format, args);

                trace(message, t);
            }
        }

        public void trace(String message, Throwable throwable) {
            if (isErrorEnabled()) {
                log(message, throwable, LoggingLevels.TRACE);
            }
        }

        public void fatal(String format, Object... args) {
            if (isErrorEnabled()) {
                String message = String.format(format, args);

                log(message, LoggingLevels.FATAL);
            }
        }

        public void fatal(Throwable t, String format, Object... args) {
            if (isErrorEnabled()) {
                String message = String.format(format, args);

                fatal(message, t);
            }
        }

        public void fatal(String message, Throwable throwable) {
            if (isErrorEnabled()) {
                log(message, throwable, LoggingLevels.FATAL);
            }
        }

        public void verbose(Integer vlevel, String format, Object... args) {
            String message = String.format(format, args);

            log(message, LoggingLevels.VERBOSE, vlevel);
        }

        public void verbose(Integer vlevel, Throwable t, String format, Object... args) {
            String message = String.format(format, args);

            verbose(vlevel, message, t);
        }

        public void verbose(Integer vlevel, String message, Throwable throwable) {
            log(message, throwable, LoggingLevels.VERBOSE, vlevel);
        }

        public void log(Object msg, Throwable t, LoggingLevels level) {
            if (t != null) {
                t.printStackTrace(Residue.getInstance().printStream);
            }
            Residue.getInstance().log(id, msg, level);
        }

        public void log(Object msg, LoggingLevels level) {
            Residue.getInstance().log(id, msg, level);
        }

        public void log(Object msg, Throwable t, LoggingLevels level, Integer vlevel) {
            if (t != null) {
                t.printStackTrace(Residue.getInstance().printStream);
            }
            Residue.getInstance().log(id, msg, level, vlevel);
        }

        public void log(Object msg, LoggingLevels level, Integer vlevel) {
            Residue.getInstance().log(id, msg, level, vlevel);
        }
    }

    /**
     * Print stream to enable System.out family. Simply do following to enable
     * <code>
     *     Residue.getInstance().setDefaultLoggerId({default_logger});
     *     System.setOut(Residue.getInstance().getPrintStream());
     * </code>
     */
    public static class ResiduePrintStream extends PrintStream {

        private ResiduePrintStream(PrintStream org) {
            super(org);
        }

        @Override
        public void println(Object line) {
            if (line == null) {
                Residue.getInstance().getLogger().info("NULL");
            } else {
                Residue.getInstance().getLogger().info(line.toString());
            }
            super.println(line);
        }

        @Override
        public void println(String line) {
            Residue.getInstance().getLogger().info(line);
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
                Residue.getInstance().getLogger().info("NULL");
            } else {
                Residue.getInstance().getLogger().info(line.toString());
            }
            super.print(line);
        }

        @Override
        public void print(String line) {
            Residue.getInstance().getLogger().info(line);
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

    /**
     * Gets existing or new logger
     *
     * @see Logger
     */
    public synchronized Logger getLogger(String id) {
        if (loggers.containsKey(id)) {
            return loggers.get(id);
        }
        Logger newLogger = new Logger(id);
        loggers.put(id, newLogger);
        return newLogger;
    }

    /**
     * Gets default logger
     *
     * @see #setDefaultLoggerId(String)
     * @see Logger
     */
    public synchronized Logger getLogger() {
        return getLogger(getInstance().defaultLoggerId);
    }

    public static Logger getClassLogger() {
        StackTraceElement[] stacktrace = Thread.currentThread().getStackTrace();
        StackTraceElement element = stacktrace[2];
        String name = element.getClassName();
        return getInstance().getLogger(name);
    }

    /**
     * Sets host and port
     */
    public void setHost(final String host, final Integer port) {
        this.host = host;
        this.port = port;
    }

    /**
     * Connects to previously set host and port.
     * <p>
     * You will need to make sure host and port is already set
     *
     * @see #setHost(String, Integer)
     * @see #connect(String, Integer)
     */
    public static boolean reconnect() throws Exception {
        return connect(getInstance().host, getInstance().port);
    }

    /**
     * Connects to the residue server and waits until connected or throws exception
     *
     * @param host Server host
     * @param port Connection port
     * @return True if successfully connected, otherwise false
     * @throws Exception If any exception is thrown
     */
    public static boolean connect(final String host, final Integer port) throws Exception {
        ResidueUtils.debugLog("reconnect()");
        getInstance().host = host;
        getInstance().port = port;
        getInstance().connecting = true;
        getInstance().connected = false;
        getInstance().connectionClient.destroy();
        getInstance().loggingClient.destroy();

        final CountDownLatch latch = new CountDownLatch(3); // 3 connection sockets
        if (getInstance().clientId != null && !getInstance().clientId.isEmpty()
                && getInstance().hasProvidedClientKey()) {
            if (getInstance().privateKeyPEM != null && !getInstance().privateKeyPEM.isEmpty()) {
                getInstance().privateKey = ResidueUtils.getPrivateKeyFromPEM(getInstance().privateKeyPEM, getInstance().privateKeySecret);
            } else {
                getInstance().privateKey = ResidueUtils.getPrivateKeyFromFile(getInstance().privateKeyFilename, getInstance().privateKeySecret);
            }
        }

        getInstance().connectionClient.connect(getInstance().host, getInstance().port, new ResponseHandler("connectionClient.reconnect") {
            @Override
            public void handle(String data, boolean hasError) {
                logForDebugging();
                JsonObject j = new JsonObject();
                j.addProperty("_t", ResidueUtils.getTimestamp());
                j.addProperty("type", ConnectType.CONNECT.getValue());
                j.addProperty("key_size", getInstance().keySize);
                if (getInstance().clientId != null
                        && !getInstance().clientId.isEmpty()
                        && getInstance().hasProvidedClientKey()
                        && getInstance().rsaKeySize != null) {
                    j.addProperty("client_id", getInstance().clientId);
                } else {
                    ResidueUtils.log("Generating " + getInstance().rsaKeySize + "-bit key...");
                    KeyPair p = ResidueUtils.createNewKeyPair(getInstance().rsaKeySize);
                    getInstance().privateKey = p.getPrivate();
                    j.addProperty("rsa_public_key", ResidueUtils.keyToPem(p.getPublic()));
                }

                String request = new Gson().toJson(j);

                if (getInstance().hasProvidedServerKey()) {
                    try {
                        final PublicKey publicKey;
                        if (getInstance().serverKeyPEM != null && !getInstance().serverKeyPEM.isEmpty()) {
                            publicKey = ResidueUtils.getPublicKeyFromPEM(getInstance().serverKeyPEM);
                        } else {
                            publicKey = ResidueUtils.getPublicKeyFromFile(getInstance().serverKeyFilename);
                        }
                        request = ResidueUtils.base64Encode(ResidueUtils.encryptRSA(request, publicKey));
                    } catch (Exception e) {
                        ResidueUtils.log("Invalid server public key, ignoring and trying with plain connection! " + e.getMessage());
                    }
                }

                getInstance().connectionClient.send(request, new ResponseHandler("connectionClient.send") {

                    @Override
                    public void handle(String data, boolean hasError) {
                        logForDebugging();
                        if (hasError) {
                            ResidueUtils.log("Failed to connect, connection refused");
                            getInstance().connecting = false;
                            getInstance().connected = false;
                            latch.countDown();
                            latch.countDown();
                            latch.countDown();
                            return;
                        }
                        try {
                            if (!data.isEmpty() && "{".equals(data.substring(1, 1))) {
                                // error?

                                ResidueUtils.log("Failed to connect. Error response: " + data);
                                getInstance().connecting = false;
                                getInstance().connected = false;
                                latch.countDown();
                                latch.countDown();
                                latch.countDown();
                            }

                            byte[] decoded = ResidueUtils.base64Decode(data);
                            String s2 = ResidueUtils.decryptRSA(decoded, getInstance().privateKey);
                            if (s2 != null) {
                                int pos = s2.indexOf("{\""); // decryption issue on android
                                if (pos == -1) {
                                    ResidueUtils.log("Pos == -1");
                                    return;
                                }
                                s2 = s2.substring(pos);
                            }
                            JsonObject nonAckResponse = new Gson().fromJson(s2, JsonObject.class);

                            getInstance().key = nonAckResponse.get("key").getAsString();
                            getInstance().clientId = nonAckResponse.get("client_id").getAsString();

                            JsonObject j = new JsonObject();
                            j.addProperty("_t", ResidueUtils.getTimestamp());
                            j.addProperty("type", ConnectType.ACKNOWLEGEMENT.getValue());
                            j.addProperty("client_id", getInstance().clientId);
                            String request = new Gson().toJson(j);
                            String r = ResidueUtils.encrypt(request, getInstance().key);
                            getInstance().connectionClient.send(r, new ResponseHandler("connectionClient.send-2") {
                                @Override
                                public void handle(String data, boolean hasError) {
                                    logForDebugging();
                                    getInstance().connecting = false;
                                    String finalConnectionStr = ResidueUtils.decrypt(data, getInstance().key);
                                    JsonObject finalConnection = new Gson().fromJson(finalConnectionStr, JsonObject.class);
                                    if (finalConnection.get("status").getAsInt() == 0) {
                                        getInstance().age = finalConnection.get("age").getAsInt();
                                        getInstance().loggingPort = finalConnection.get("logging_port").getAsInt();
                                        getInstance().maxBulkSize = finalConnection.get("max_bulk_size").getAsInt();
                                        getInstance().serverFlags = finalConnection.get("flags").getAsInt();
                                        getInstance().serverVersion = finalConnection.get("server_info").getAsJsonObject().get("version").getAsString();
                                        getInstance().dateCreated = new Date(finalConnection.get("date_created").getAsLong() * 1000);
                                        if (Boolean.TRUE.equals(getInstance().autoBulkParams) && Flag.ALLOW_BULK_LOG_REQUEST.isSet()) {
                                            getInstance().bulkSize = Math.min(getInstance().maxBulkSize, 40);
                                            getInstance().bulkDispatch = true;
                                        }
                                        if (Boolean.TRUE.equals(getInstance().bulkDispatch) && Flag.ALLOW_BULK_LOG_REQUEST.isSet() && getInstance().bulkSize > getInstance().maxBulkSize) {
                                            getInstance().bulkSize = getInstance().maxBulkSize;
                                        } else if (Boolean.TRUE.equals(getInstance().bulkDispatch) && !Flag.ALLOW_BULK_LOG_REQUEST.isSet()) {
                                            getInstance().bulkDispatch = false;
                                        }
                                        getInstance().connected = true;
                                        try {
                                            getInstance().loggingClient.connect(getInstance().host, getInstance().loggingPort, new ResponseHandler("loggingClient.reconnect") {
                                                @Override
                                                public void handle(String data, boolean hasError) {
                                                    logForDebugging();
                                                    latch.countDown();
                                                }
                                            });
                                        } catch (IOException e) {
                                            latch.countDown();
                                        }
                                    } else {
                                        getInstance().lastError = finalConnection.get("error_text").getAsString();
                                    }
                                    latch.countDown();
                                }
                            });
                        } catch (Exception e) {
                            ResidueUtils.log(e.getMessage());
                            throw e;
                        }
                    }

                });
            }
        });

        ResidueUtils.debugLog("Waiting for residue connection...");
        latch.await(5L, TimeUnit.SECONDS);

        if (getInstance().connected) {
            try {
                if (!getInstance().dispatcher.isAlive()) {
                    getInstance().dispatcher.start();
                } else {
                    ResidueUtils.debugLog("Dispatcher resumed!");
                }
            } catch (Exception e) {
                ResidueUtils.log("ERROR: Unable to start dispatcher thread [" + e.getMessage() + "]");
            }
        } else {
            ResidueUtils.log("ERROR: Residue connection timeout [5s]");
        }
        return getInstance().connected;
    }

    private abstract static class ResponseHandler {

        private String id;

        private ResponseHandler(String id) {
            this.id = id;
        }

        public abstract void handle(String data, boolean hasError);

        /**
         * For residue-java developers only
         */
        public void logForDebugging() {
            //ResidueUtils.debugLog("ResponseHandler::handle " + this.id);
        }

        /**
         * For residue-java developers only
         */
        public void logForDebugging(final String data) {
            logForDebugging();
            //ResidueUtils.debugLog("ResponseHandler::handle::data = " + data);
        }

        @Override
        public String toString() {
            return id;
        }
    }

    private enum ConnectType {
        CONNECT(1),
        ACKNOWLEGEMENT(2),
        TOUCH(3);

        private Integer value;

        ConnectType(Integer value) {
            this.value = value;
        }

        public Integer getValue() {
            return value;
        }
    }

    private enum LoggingLevels {
        TRACE(2),
        DEBUG(4),
        FATAL(8),
        ERROR(16),
        WARNING(32),
        VERBOSE(64),
        INFO(128);

        private Integer value;

        LoggingLevels(Integer value) {
            this.value = value;
        }

        public Integer getValue() {
            return value;
        }
    }

    private enum Flag {
        NONE(0),
        ALLOW_UNKNOWN_LOGGERS(1),
        ALLOW_PLAIN_LOG_REQUEST(8),
        ALLOW_BULK_LOG_REQUEST(16),
        COMPRESSION(256);

        private Integer value;

        Flag(Integer value) {
            this.value = value;
        }

        public boolean isSet() {
            return (Residue.getInstance().serverFlags & this.value) != 0;
        }
    }

    /**
     * Residue network client
     */
    private static class ResidueClient {
        private static final String PACKET_DELIMITER = "\r\n\r\n";
        private AsynchronousSocketChannel socketChannel;
        private Boolean isConnected;

        private ResidueClient() {
            isConnected = false;
        }

        private void destroy() {
            try {
                if (isConnected && socketChannel.isOpen()) {
                    isConnected = false;
                    socketChannel.close();
                }
            } catch (IOException e) {
                // ignore
            }
        }

        private void connect(String host, Integer port, final ResponseHandler responseHandler) throws IOException {
            socketChannel = AsynchronousSocketChannel.open();
            socketChannel.connect(new InetSocketAddress(host, port), socketChannel,
                    new CompletionHandler<Void, AsynchronousSocketChannel>() {
                        @Override
                        public void completed(Void result, AsynchronousSocketChannel channel) {
                            isConnected = true;
                            responseHandler.handle("CONNECTED", false);
                        }

                        @Override
                        public void failed(Throwable exc, AsynchronousSocketChannel channel) {
                            ResidueUtils.log("Failed to reconnect to the server " + exc);
                            responseHandler.handle("FAILED", true);
                        }
                    });
        }

        private void read(final ResponseHandler responseHandler) {
            final ByteBuffer buf = ByteBuffer.allocate(ALLOCATION_BUFFER_SIZE);
            try {
                socketChannel.read(buf, socketChannel,
                        new CompletionHandler<Integer, AsynchronousSocketChannel>() {
                            @Override
                            public void completed(Integer result, AsynchronousSocketChannel channel) {
                                try {
                                    responseHandler.handle(new String(buf.array(), "UTF-8").substring(0, ALLOCATION_BUFFER_SIZE - buf.remaining()), false);
                                } catch (UnsupportedEncodingException e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void failed(Throwable exc, AsynchronousSocketChannel channel) {
                                ResidueUtils.log("Thrown exception while reading: " + exc.getMessage());
                                if ("Connection reset by peer".equals(exc.getMessage())) {
                                    isConnected = false;
                                    getInstance().connected = false;
                                }
                                responseHandler.handle(exc.getMessage(), true);
                            }

                        });
            } catch (ReadPendingException e) {
                // We ignore this as we may send multiple requests without really waiting for
                // response
            } catch (NotYetConnectedException e) {
                // we may be in the middle of connecting but still log it
                e.printStackTrace();
            }
        }

        private void send(final String message, final ResponseHandler responseHandler) {
            ByteBuffer buf = ByteBuffer.allocate(ALLOCATION_BUFFER_SIZE);
            buf.put((message + PACKET_DELIMITER).getBytes());
            buf.flip();
            socketChannel.write(buf, socketChannel,
                    new CompletionHandler<Integer, AsynchronousSocketChannel>() {
                        @Override
                        public void completed(Integer result, AsynchronousSocketChannel channel) {
                            read(responseHandler);
                        }

                        @Override
                        public void failed(Throwable exc, AsynchronousSocketChannel channel) {
                            exc.printStackTrace();
                            read(responseHandler);
                        }

                    });
        }
    }

    /**
     * Residue utility functions
     */
    private static class ResidueUtils {

        /**
         * Log for debugging residue-java
         */
        private static void log(Object msg) {
            /*synchronized (System.out) {
                System.out.println(msg);
            }*/
        }

        /**
         * Log for debugging residue-java
         */
        private static void debugLog(Object msg) {
            /*synchronized (System.out) {
                System.out.println(msg);
            }*/
        }

        private static String readFile(String filename) throws Exception {
            File f = new File(filename);
            FileInputStream fis = new FileInputStream(f);
            DataInputStream dis = new DataInputStream(fis);
            final ByteBuffer buf = ByteBuffer.allocate((int) f.length());
            dis.readFully(buf.array());
            dis.close();
            fis.close();

            return new String(buf.array());
        }

        private static PrivateKey getPrivateKeyFromFile(String filename, String secret) throws Exception {
            return getPrivateKeyFromPEM(readFile(filename), secret);
        }

        private static PrivateKey getPrivateKeyFromPEM(String pem, String secret) throws Exception {
            String privKeyPEM = pem.replace("-----BEGIN RSA PRIVATE KEY-----", "");
            privKeyPEM = privKeyPEM.replace("-----END RSA PRIVATE KEY-----", "");
            privKeyPEM = privKeyPEM.trim();

            byte[] decoded = ResidueUtils.base64Decode(privKeyPEM);

            ASN1EncodableVector v = new ASN1EncodableVector();
            v.add(new ASN1Integer(0));
            ASN1EncodableVector v2 = new ASN1EncodableVector();
            ASN1ObjectIdentifier objectIdentifier = new ASN1ObjectIdentifier(PKCSObjectIdentifiers.rsaEncryption.getId());
            v2.add(objectIdentifier);
            v2.add(DERNull.INSTANCE);
            v.add(new DERSequence(v2));
            v.add(new DEROctetString(decoded));
            ASN1Sequence seq = new DERSequence(v);
            byte[] privKey = seq.getEncoded("DER");

            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privKey);
            KeyFactory kf = KeyFactory.getInstance("RSA");

            if (secret != null && !secret.isEmpty()) {
                boolean f = keySpec.getEncoded() == privKey;
                // FIXME: Encrypted private keys not working
                PBEKeySpec pbeSpec = new PBEKeySpec(secret.toCharArray());
                EncryptedPrivateKeyInfo pkinfo = new EncryptedPrivateKeyInfo(keySpec.getEncoded());
                SecretKeyFactory skf = SecretKeyFactory.getInstance(pkinfo.getAlgName());
                Key secretKey = skf.generateSecret(pbeSpec);
                keySpec = pkinfo.getKeySpec(secretKey);
            }
            return kf.generatePrivate(keySpec);
        }


        private static PublicKey getPublicKeyFromFile(String filename) throws Exception {
            return getPublicKeyFromPEM(readFile(filename));
        }

        private static PublicKey getPublicKeyFromPEM(String pem) throws Exception {
            String publicKeyPEM = pem.replace("-----BEGIN PUBLIC KEY-----\n", "");
            publicKeyPEM = publicKeyPEM.replace("-----END PUBLIC KEY-----", "");

            byte[] decoded = ResidueUtils.base64Decode(publicKeyPEM);

            X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePublic(spec);
        }

        private static KeyPair createNewKeyPair(Integer size) {
            try {
                KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
                SecureRandom random = SecureRandom.getInstance("SHA1PRNG");//, "SUN"); // Android!
                keyGen.initialize(size, random);
                return keyGen.generateKeyPair();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        private static String keyToPem(PublicKey pubKey) {
            String s = "-----BEGIN RSA PUBLIC KEY-----\n" +
                    ResidueUtils.base64Encode(pubKey.getEncoded()) +
                    "\n-----END RSA PUBLIC KEY-----\n";
            return ResidueUtils.base64Encode(s.getBytes());
        }

        private static byte[] encryptRSA(String Buffer, PublicKey key) {
            try {

                Cipher rsa = Cipher.getInstance("RSA");
                rsa.init(Cipher.ENCRYPT_MODE, key);
                return rsa.doFinal(Buffer.getBytes());
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }


        private static String decryptRSA(byte[] buffer, PrivateKey key) {
            try {
                Cipher rsa = Cipher.getInstance("RSA");
                rsa.init(Cipher.DECRYPT_MODE, key);
                byte[] utf8 = rsa.doFinal(buffer);
                return new String(utf8, "UTF-8");
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        private static String encrypt(String request, String keyHex) {
            try {
                byte[] initVector = new byte[16];
                new Random().nextBytes(initVector);

                String randomIV = ResidueUtils.hexEncode(initVector);
                byte[] k = ResidueUtils.hexDecode(keyHex);
                IvParameterSpec ivSpec = new IvParameterSpec(initVector);
                SecretKeySpec keySpec = new SecretKeySpec(k, "AES");

                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
                cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

                byte[] encrypted = cipher.doFinal(request.getBytes());
                return randomIV + ":" + Residue.getInstance().clientId + ":" + ResidueUtils.base64Encode(encrypted);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        private static String decrypt(String response, String keyHex) {
            try {
                String[] parts = response.split(":");
                if (parts.length >= 2) {
                    String iv = parts[0];
                    byte[] data = ResidueUtils.base64Decode(parts[1]);
                    IvParameterSpec ivSpec = new IvParameterSpec(ResidueUtils.hexDecode(iv));
                    SecretKeySpec keySpec = new SecretKeySpec(ResidueUtils.hexDecode(keyHex), "AES");

                    Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5PADDING");
                    cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

                    byte[] decrypted = cipher.doFinal(data);
                    return new String(decrypted);
                }
                return null;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        private static String base64Encode(byte[] bytes) {
            return Base64.encodeToString(bytes, Base64.DEFAULT);
        }

        private static byte[] base64Decode(String str) {
            return Base64.decode(str.getBytes(), Base64.DEFAULT);
        }

        private final static char[] HEX_DIGITS = "0123456789ABCDEF".toCharArray();

        private static String hexEncode(byte[] bytes) {
            int l = bytes.length;

            char[] out = new char[l << 1];

            for (int i = 0, j = 0; i < l; i++) {
                out[j++] = HEX_DIGITS[(0xF0 & bytes[i]) >>> 4];
                out[j++] = HEX_DIGITS[0x0F & bytes[i]];
            }

            return new String(out);
        }

        private static byte[] hexDecode(String hex) throws RuntimeException {
            char[] data = hex.toCharArray();
            int len = data.length;

            if ((len & 0x01) != 0) {
                throw new RuntimeException("Odd number of characters.");
            }

            byte[] out = new byte[len >> 1];

            for (int i = 0, j = 0; j < len; i++) {
                int f = Character.digit(data[j++], 16) << 4;
                f = f | Character.digit(data[j++], 16);
                out[i] = (byte) (f & 0xFF);
            }

            return out;

        }

        private static long getTimestamp() {
            return System.currentTimeMillis() / 1000;
        }
    }

    private boolean isClientValid() {
        if (!connected || dateCreated == null) {
            return false;
        }
        if (age == 0) {
            return true;
        }
        return new Date(((dateCreated.getTime() / 1000) + age) * 1000).after(new Date());
    }

    private boolean shouldTouch() {
        if (!connected || connecting) {
            // Can't send touch
            return false;
        }
        if (age == 0) {
            // Always alive!
            return false;
        }
        if (dateCreated == null) {
            return true;
        }
        return age - ((new Date().getTime() / 1000) - (dateCreated.getTime() / 1000)) < TOUCH_THRESHOLD;
    }

    private void touch() {
        final CountDownLatch latch = new CountDownLatch(1);
        JsonObject j = new JsonObject();
        j.addProperty("_t", ResidueUtils.getTimestamp());
        j.addProperty("type", ConnectType.TOUCH.getValue());
        j.addProperty("client_id", clientId);
        String request = new Gson().toJson(j);

        String r = ResidueUtils.encrypt(request, key);

        connectionClient.send(r, new ResponseHandler("connectionClient.touch") {
            @Override
            public void handle(String data, boolean hasError) {
                logForDebugging(data);
                String touchResponseStr = ResidueUtils.decrypt(data, key);
                JsonObject touchResponse = new Gson().fromJson(touchResponseStr, JsonObject.class);
                if (touchResponse != null && touchResponse.get("status").getAsInt() == 0) {
                    ResidueUtils.log("Updating client age via touch!");
                    dateCreated = new Date(touchResponse.get("date_created").getAsLong() * 1000);
                }
                latch.countDown();
            }
        });
        try {
            latch.await(10L, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            // ignore
        }
    }

    private Thread dispatcher = new Thread(new Runnable() {
        public void run() {
            Integer reconnectingAttempts = 0; // don't use Timer as we want to schedule it once
            while (true) {
                if (!backlog.isEmpty()) {
                    if (isConnecting()) {
                        ResidueUtils.debugLog("Still connecting...");
                        if (reconnectingAttempts >= 20) { // 10 seconds
                            connecting = false;
                            connected = false;
                            reconnectingAttempts = 0; // reset
                            // fallthrough to next condition to ensure we start from scratch the re-connection
                        } else {
                            try {
                                Thread.sleep(500);
                                reconnectingAttempts++;
                            } catch (InterruptedException e) {
                                // Ignore
                            }
                        }
                        continue;
                    }

                    if (!isConnected()) {
                        try {
                            ResidueUtils.log("Trying to reconnect...");
                            connect(host, port);
                        } catch (Exception e) {
                            ResidueUtils.log("Unable to connect, " + e.getMessage() + "\nRetrying in 500ms");
                        }
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            // Ignore
                        }
                        continue;
                    }

                    if (!isClientValid()) {
                        try {
                            ResidueUtils.log("Client expired, reconnecting...");
                            connect(host, port);
                        } catch (Exception e) {
                            // Unable to reconnect
                            e.printStackTrace();
                        }
                    }

                    if (shouldTouch()) {
                        ResidueUtils.log("Touching...");
                        touch();
                    }

                    Integer totalRequests = Boolean.TRUE.equals(bulkDispatch) ? bulkSize : 1;
                    totalRequests = Math.min(totalRequests, backlog.size());

                    final JsonArray bulkJ = new JsonArray();
                    final Set<String> loggerIds = new HashSet<>();

                    // build up bulk request
                    synchronized (backlog) {
                        for (Integer i = 0; i < totalRequests; ++i) {
                            JsonObject j;
                            j = backlog.pop();
                            if (j != null) {
                                loggerIds.add(j.get("logger").getAsString());
                                bulkJ.add(j);
                            }
                        }
                    }

                    String request = new Gson().toJson(Boolean.TRUE.equals(bulkDispatch) ? bulkJ : bulkJ.get(0));
                    if (Flag.COMPRESSION.isSet()) {
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        DeflaterOutputStream dos = new DeflaterOutputStream(baos);
                        try {
                            dos.write(request.getBytes());
                            dos.flush();
                            dos.close();
                            request = ResidueUtils.base64Encode(baos.toByteArray());
                        } catch (IOException e) {
                            ResidueUtils.log("Failed to compress: " + e.getMessage());
                        }
                    }
                    String r = ResidueUtils.encrypt(request, key);
                    getInstance().loggingClient.send(r, new ResponseHandler("loggingClient.send") {
                        @Override
                        public void handle(String data, boolean hasError) {
                            if (data.isEmpty()) {
                                // Not connected
                                getInstance().connected = false;
                                getInstance().loggingClient.isConnected = false;
                            } else {
                                ResidueUtils.debugLog("loggingClient response: " + data);
                            }
                        }
                    });
                }
                try {
                    Thread.sleep(dispatchDelay);
                } catch (InterruptedException e) {
                    // Ignore
                }
            }
        }
    });

    private StackTraceElement getStackItem(int baseIdx, LoggingLevels level, Integer vlevel) {
        String sourceFilename = "";
        StackTraceElement stackItem = null;
        while (sourceFilename.isEmpty() || "Residue.java".equals(sourceFilename)) {
            final int stackItemIndex = level == LoggingLevels.VERBOSE && vlevel > 0 ? baseIdx : (baseIdx + 1);
            final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            if (stackTrace != null && stackTrace.length > stackItemIndex) {
                stackItem = Thread.currentThread().getStackTrace()[stackItemIndex];
                sourceFilename = stackItem == null ? "" : stackItem.getFileName();
            }
            baseIdx++;
            if (baseIdx >= 10) {
                // too much effort, leave it!
                // technically it should be resolved when baseIdx == 4 or 5 or max 6
                break;
            }
        }
        return stackItem;
    }

    private StackTraceElement getStackItem(LoggingLevels level, Integer vlevel) {
        return getStackItem(4, level, vlevel);
    }

    private Long getTime(Long baseTime) {
        Boolean isNonUTC = false;
        Calendar c = Calendar.getInstance();
        if (baseTime != null) {
            c.setTime(new Date(baseTime));
        }
        if (Boolean.TRUE.equals(utcTime)) {
            TimeZone timeZone = c.getTimeZone();
            int offset = timeZone.getRawOffset();

            if (timeZone.inDaylightTime(new Date())) {
                offset = offset + timeZone.getDSTSavings();
            }

            int offsetHrs = offset / 1000 / 60 / 60;
            int offsetMins = offset / 1000 / 60 % 60;

            if (offsetHrs != 0 || offsetMins != 0) { // already utc
                c.add(Calendar.HOUR_OF_DAY, -offsetHrs);
                c.add(Calendar.MINUTE, -offsetMins);
                isNonUTC = true;
            }
        }
        if (timeOffset != null) {
            if (useTimeOffsetIfNotUtc && isNonUTC) {
                c.add(Calendar.SECOND, timeOffset);
            } else if (!useTimeOffsetIfNotUtc) {
                c.add(Calendar.SECOND, timeOffset);
            }
        }
        return c.getTime().getTime();
    }

    private void log(String loggerId, String msg, LoggingLevels level, Integer vlevel) {
        int baseIdx = 5;
        StackTraceElement stackItem = getStackItem(baseIdx, level, vlevel);
        String sourceFilename = stackItem == null ? "" : stackItem.getFileName();

        log(getTime(null), loggerId, msg, applicationName, level,
                sourceFilename, stackItem == null ? 0 : stackItem.getLineNumber(),
                stackItem == null ? "" : stackItem.getMethodName(),
                Thread.currentThread().getName(),
                vlevel);
    }

    public void log(Long datetime, String loggerId, String msg,
                     String applicationName, LoggingLevels level, String sourceFilename,
                     Integer sourceLineNumber, String sourceMethodName, String threadName,
                     Integer vlevel) {
        JsonObject j = new JsonObject();
        j.addProperty("_t", ResidueUtils.getTimestamp());
        j.addProperty("datetime", datetime);
        j.addProperty("logger", loggerId);
        j.addProperty("msg", msg);
        j.addProperty("file", sourceFilename);
        j.addProperty("line", sourceLineNumber);
        j.addProperty("func", sourceMethodName);
        j.addProperty("app", applicationName);
        j.addProperty("level", level.getValue());
        j.addProperty("thread", threadName);
        j.addProperty("vlevel", vlevel);

        synchronized (backlog) {
            backlog.add(j);
        }
    }

    private void log(String loggerId, Object msg, LoggingLevels level) {
        log(loggerId, msg == null ? "NULL" : msg.toString(), level, 0);
    }

    private void log(String loggerId, Object msg, LoggingLevels level, Integer vlevel) {
        log(loggerId, msg == null ? "NULL" : msg.toString(), level, vlevel);
    }

    /**
     *
     * Logs using java logging API's LogRecord.
     *
     * Level map:
     *
     * SEVERE => ERROR
     * WARNING => WARNING
     * INFO => INFO
     * CONFIG => DEBUG
     * FINE => VERBOSE level 3
     * FINER => VERBOSE level 5
     * FINEST => VERBOSE level 9
     */
    private void log(LogRecord record) {
        String loggerName = record.getLoggerName();

        if (loggerName == null || loggerName.trim().isEmpty()) {
            loggerName = getInstance().defaultLoggerId;
        }

        Integer vlevel = 0;
        LoggingLevels level;

        if (record.getLevel() == Level.SEVERE) {
            level = LoggingLevels.ERROR;
        } else if (record.getLevel() == Level.WARNING) {
            level = LoggingLevels.WARNING;
        } else if (record.getLevel() == Level.INFO) {
            level = LoggingLevels.INFO;
        } else if (record.getLevel() == Level.CONFIG) {
            level = LoggingLevels.DEBUG;
        } else if (record.getLevel() == Level.FINE) {
            level = LoggingLevels.VERBOSE;
            vlevel = 3;
        } else if (record.getLevel() == Level.FINER) {
            level = LoggingLevels.VERBOSE;
            vlevel = 5;
        } else if (record.getLevel() == Level.FINEST) {
            level = LoggingLevels.VERBOSE;
            vlevel = 9;
        } else {
            level = LoggingLevels.INFO;
        }

        StackTraceElement si = getStackItem(8, level, vlevel);
        Integer lineNumber = si == null ? 0 : si.getLineNumber();

        log(getTime(record.getMillis()),
                loggerName,
                record.getMessage(),
                applicationName,
                level,
                record.getSourceClassName(),
                lineNumber,
                record.getSourceMethodName(),
                Thread.currentThread().getName(),
                vlevel);
    }
}
