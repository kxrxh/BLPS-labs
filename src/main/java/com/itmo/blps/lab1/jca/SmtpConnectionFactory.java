package com.itmo.blps.lab1.jca;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionManager;

public class SmtpConnectionFactory {

    private final SmtpManagedConnectionFactory mcf;
    private final ConnectionManager cm;

    public SmtpConnectionFactory(SmtpManagedConnectionFactory mcf, ConnectionManager cm) {
        this.mcf = mcf;
        this.cm = cm;
    }

    public SmtpConnection getConnection() throws ResourceException {
        if (cm != null) {
            return (SmtpConnection) cm.allocateConnection(mcf, null);
        } else {
            SmtpManagedConnection managedConnection = (SmtpManagedConnection) mcf.createManagedConnection(null, null);
            return (SmtpConnection) managedConnection.getConnection(null, null);
        }
    }

    public SmtpConnection getConnection(String username, String password) throws ResourceException {
        SmtpConnectionRequestInfo cri = new SmtpConnectionRequestInfo(username, password);
        if (cm != null) {
            return (SmtpConnection) cm.allocateConnection(mcf, cri);
        } else {
            SmtpManagedConnection managedConnection = (SmtpManagedConnection) mcf.createManagedConnection(null, cri);
            return (SmtpConnection) managedConnection.getConnection(null, cri);
        }
    }
}
