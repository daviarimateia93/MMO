package com.mmo.core.map;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;

@Getter
@EqualsAndHashCode
@ToString
public class Position {

    private Long x;
    private Long y;

    @Builder
    private Position(@NonNull Long x, @NonNull Long y) {
        this.x = x;
        this.y = y;
    }

    public void incrementX(long x) {
        this.x += x;
    }

    public void incrementY(long y) {
        this.y += y;
    }

    public void decrementX(long x) {
        this.x -= x;
    }

    public void decrementY(long y) {
        this.y -= y;
    }

    public boolean isNearby(Position position, int ratio) {
        long minX = x - ratio;
        long maxX = x + ratio;
        long minY = y - ratio;
        long maxY = y + ratio;

        return position.getX() >= minX && position.getX() <= maxX
                && position.getY() >= minY && position.getY() <= maxY;
    }
}
