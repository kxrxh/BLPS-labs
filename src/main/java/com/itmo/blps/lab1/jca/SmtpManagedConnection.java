package com.itmo.blps.lab1.jca;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.*;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.security.auth.Subject;
import javax.transaction.xa.XAResource;

public class SmtpManagedConnection implements ManagedConnection {

    private final SmtpManagedConnectionFactory factory;
    private final String username;
    private final String password;
    private final Properties mailProperties;
    private final Session mailSession;
    private PrintWriter logWriter;
    private final List<ConnectionEventListener> listeners;
    private final List<SmtpConnection> connections;

    public SmtpManagedConnection(SmtpManagedConnectionFactory factory, String username, String password,
            Properties mailProperties) {

        this.factory = factory;
        this.username = username;
        this.password = password;
        this.mailProperties = mailProperties;
        this.listeners = new ArrayList<>();
        this.connections = new ArrayList<>();

        this.mailSession = Session.getInstance(this.mailProperties, new jakarta.mail.Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
    }

    @Override
    public Object getConnection(Subject subject, ConnectionRequestInfo cri) throws ResourceException {
        SmtpConnection connection = new SmtpConnection(this, mailSession, factory.getSenderEmail(), username);
        connections.add(connection);
        return connection;
    }

    @Override
    public void destroy() throws ResourceException {
        for (SmtpConnection connection : connections) {
            connection.invalidate();
        }
        connections.clear();
    }

    @Override
    public void cleanup() throws ResourceException {
        for (SmtpConnection connection : connections) {
            connection.invalidate();
        }
        connections.clear();
    }

    @Override
    public void associateConnection(Object connection) throws ResourceException {
        if (connection instanceof SmtpConnection smtpConn) {
            connections.add(smtpConn);
        } else {
            throw new ResourceException("Invalid connection type");
        }
    }

    @Override
    public void addConnectionEventListener(ConnectionEventListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeConnectionEventListener(ConnectionEventListener listener) {
        listeners.remove(listener);
    }

    @Override
    public XAResource getXAResource() throws ResourceException {
        return null;
    }

    @Override
    public LocalTransaction getLocalTransaction() throws ResourceException {
        return null;
    }

    @Override
    public ManagedConnectionMetaData getMetaData() throws ResourceException {
        return new ManagedConnectionMetaData() {
            @Override
            public String getEISProductName() throws ResourceException {
                return "SMTP Mail Server";
            }

            @Override
            public String getEISProductVersion() throws ResourceException {
                return "1.0";
            }

            @Override
            public int getMaxConnections() throws ResourceException {
                return 10;
            }

            @Override
            public String getUserName() throws ResourceException {
                return username;
            }
        };
    }

    @Override
    public void setLogWriter(PrintWriter out) throws ResourceException {
        this.logWriter = out;
    }

    @Override
    public PrintWriter getLogWriter() throws ResourceException {
        return logWriter;
    }

    void fireConnectionEvent(int eventType, Object connection, Exception exception) {
        ConnectionEvent event = new ConnectionEvent(this, eventType, exception);
        event.setConnectionHandle(connection);

        for (ConnectionEventListener listener : listeners) {
            switch (eventType) {
                case ConnectionEvent.CONNECTION_CLOSED -> listener.connectionClosed(event);
                case ConnectionEvent.CONNECTION_ERROR_OCCURRED -> listener.connectionErrorOccurred(event);
                case ConnectionEvent.LOCAL_TRANSACTION_COMMITTED -> listener.localTransactionCommitted(event);
                case ConnectionEvent.LOCAL_TRANSACTION_ROLLEDBACK -> listener.localTransactionRolledback(event);
                case ConnectionEvent.LOCAL_TRANSACTION_STARTED -> listener.localTransactionStarted(event);
                default -> {
                }
            }
        }
    }

    public SmtpManagedConnectionFactory getFactory() {
        return factory;
    }

    boolean passwordMatches(String username, String password) {
        return this.username.equals(username) && this.password.equals(password);
    }
}
