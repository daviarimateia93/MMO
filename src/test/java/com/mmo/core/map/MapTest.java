package com.mmo.core.map;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.mmo.core.looper.LooperContext;

public class MapTest {

    @Test
    public void addEntity() {
        Entity entityA = new Entity(Position.builder()
                .x(10L)
                .y(15L)
                .build());

        Entity entityB = new Entity(Position.builder()
                .x(11L)
                .y(13L)
                .build());

        Entity entityC = new Entity(Position.builder()
                .x(9L)
                .y(17L)
                .build());

        Map map = Map.builder()
                .name("name")
                .description("description")
                .nearbyRatio(5)
                .build();

        map.addEntity(entityA);
        map.addEntity(entityB);
        map.addEntity(entityC);

        MapEntity[] expected = { entityA, entityB, entityC };
        Set<MapEntity> result = map.getEntities();

        assertThat(result, containsInAnyOrder(expected));
        assertThat(result.size(), equalTo(expected.length));
    }

    @Test
    public void removeEntity() {
        Entity entityA = new Entity(Position.builder()
                .x(10L)
                .y(15L)
                .build());

        Entity entityB = new Entity(Position.builder()
                .x(11L)
                .y(13L)
                .build());

        Map map = Map.builder()
                .name("name")
                .description("description")
                .nearbyRatio(5)
                .build();

        map.addEntity(entityA);
        map.addEntity(entityB);
        map.removeEntity(entityA);

        MapEntity[] expected = { entityB };
        Set<MapEntity> result = map.getEntities();

        assertThat(result, containsInAnyOrder(expected));
        assertThat(result.size(), equalTo(expected.length));
    }

    @Test
    public void getNearbyEntities() {
        Entity entityA = new Entity(Position.builder()
                .x(10L)
                .y(15L)
                .build());

        Entity entityB = new Entity(Position.builder()
                .x(11L)
                .y(13L)
                .build());

        Entity entityC = new Entity(Position.builder()
                .x(19L)
                .y(27L)
                .build());

        Map map = Map.builder()
                .name("name")
                .description("description")
                .nearbyRatio(5)
                .build();

        map.addEntity(entityA);
        map.addEntity(entityB);
        map.addEntity(entityC);

        MapEntity[] expected = { entityA, entityB };
        Set<MapEntity> result = map.getNearbyEntities(entityA);

        assertThat(result, containsInAnyOrder(expected));
        assertThat(result.size(), equalTo(expected.length));
    }

    @Test
    public void getTypedNearbyEntities() {
        Entity entityA = new Entity(Position.builder()
                .x(10L)
                .y(15L)
                .build());

        Entity entityB = new Entity(Position.builder()
                .x(11L)
                .y(13L)
                .build());

        Entity entityC = new Entity(Position.builder()
                .x(19L)
                .y(27L)
                .build());

        SubEntity entityD = new SubEntity(Position.builder()
                .x(11L)
                .y(13L)
                .build());

        Map map = Map.builder()
                .name("name")
                .description("description")
                .nearbyRatio(5)
                .build();

        map.addEntity(entityA);
        map.addEntity(entityB);
        map.addEntity(entityC);
        map.addEntity(entityD);

        MapEntity[] expected = { entityD };
        Set<SubEntity> result = map.getNearbyEntities(entityA, SubEntity.class);

        assertThat(result, containsInAnyOrder(expected));
        assertThat(result.size(), equalTo(expected.length));
    }

    private class Entity implements MapEntity {

        UUID instanceId = UUID.randomUUID();
        String name = UUID.randomUUID().toString();
        Position position;

        public Entity(Position position) {
            this.position = position;
        }

        @Override
        public UUID getInstanceId() {
            return instanceId;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Position getPosition() {
            return position;
        }

        @Override
        public void update(LooperContext context) {

        }
    }

    private class SubEntity extends Entity {

        public SubEntity(Position position) {
            super(position);
        }
    }
}