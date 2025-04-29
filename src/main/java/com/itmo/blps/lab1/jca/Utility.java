package com.itmo.blps.lab1.jca;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionRequestInfo;
import jakarta.resource.spi.ManagedConnectionFactory;
import jakarta.resource.spi.security.PasswordCredential;
import javax.security.auth.Subject;
import java.util.Set;

public class Utility {

    /**
     * Get password credential from subject or connection request info
     */
    public static PasswordCredential getPasswordCredential(
            final ManagedConnectionFactory mcf,
            final Subject subject,
            final ConnectionRequestInfo cri) throws ResourceException {

        if (subject == null) {
            return null;
        }

        Set<PasswordCredential> credentials = subject.getPrivateCredentials(PasswordCredential.class);

        for (PasswordCredential pc : credentials) {
            if (pc.getManagedConnectionFactory().equals(mcf)) {
                return pc;
            }
        }

        return null;
    }
}
