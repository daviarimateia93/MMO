package com.mmo.infrastructure.server;

import com.mmo.core.RuntimeException;

public abstract class ServerException extends RuntimeException {

    private static final long serialVersionUID = -7685496233211820023L;

    public ServerException(String messageFormat, Object... arguments) {
        super(messageFormat, arguments);
    }

    public ServerException(Throwable throwable, String messageFormat, Object... arguments) {
        super(throwable, messageFormat, arguments);
    }
}
