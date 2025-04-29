package com.itmo.blps.lab1.jca;

import jakarta.resource.spi.ConnectionRequestInfo;
import java.util.Objects;

public class SmtpConnectionRequestInfo implements ConnectionRequestInfo {
    private final String username;
    private final String password;

    public SmtpConnectionRequestInfo(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        SmtpConnectionRequestInfo that = (SmtpConnectionRequestInfo) o;
        return Objects.equals(username, that.username) &&
                Objects.equals(password, that.password);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, password);
    }
}
