package com.mmo.infrastructure.map.packet;

import com.mmo.infrastructure.map.MapServerException;

public class PacketHandlerNotBindedException extends MapServerException {

    private static final long serialVersionUID = -7522948217848722758L;

    public PacketHandlerNotBindedException(String messageFormat, Object... arguments) {
        super(messageFormat, arguments);
    }
}
