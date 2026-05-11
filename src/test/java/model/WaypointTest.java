package model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Waypoint - точка маршрута")
class WaypointTest {

    @Test
    @DisplayName("Создание Waypoint с tolerance по умолчанию")
    void testDefaultTolerance() {
        Waypoint wp = new Waypoint(100, 200);
        assertEquals(100, wp.getX());
        assertEquals(200, wp.getY());
        assertEquals(5.0, wp.getTolerance());
    }

    @Test
    @DisplayName("Создание Waypoint с указанным tolerance")
    void testCustomTolerance() {
        Waypoint wp = new Waypoint(150, 250, 10.0);
        assertEquals(150, wp.getX());
        assertEquals(250, wp.getY());
        assertEquals(10.0, wp.getTolerance());
    }

    @Test
    @DisplayName("Waypoint с нулевыми координатами")
    void testZeroCoordinates() {
        Waypoint wp = new Waypoint(0, 0);
        assertEquals(0, wp.getX());
        assertEquals(0, wp.getY());
    }

    @Test
    @DisplayName("Waypoint с отрицательными координатами")
    void testNegativeCoordinates() {
        Waypoint wp = new Waypoint(-50, -100);
        assertEquals(-50, wp.getX());
        assertEquals(-100, wp.getY());
    }
}