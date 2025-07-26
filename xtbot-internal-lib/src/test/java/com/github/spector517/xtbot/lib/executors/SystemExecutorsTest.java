package com.github.spector517.xtbot.lib.executors;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SystemExecutorsTest {

    @ParameterizedTest
    @CsvSource({
        "millis,200,190,500",
        "seconds,1,900,2000",
        "minutes,0,0,100",
        "hours,0,0,100"
    })
    void pause_shouldSleepForApproximateTime(String unit, int time, int minMs, int maxMs) {
        long start = System.currentTimeMillis();
        SystemExecutors.pause(time, unit);
        long elapsed = System.currentTimeMillis() - start;
        assertTrue(elapsed >= minMs && elapsed < maxMs);
    }

    @Test
    void pause_shouldThrowOnUnsupportedUnit() {
        var ex = assertThrows(
                IllegalArgumentException.class,
                () -> SystemExecutors.pause(1, "days")
        );
        assertTrue(ex.getMessage().contains("Unsupported time unit"));
    }

    @Test
    void pause_shouldHandleInterruptedException() {
        Thread testThread = new Thread(() -> SystemExecutors.pause(1000, "millis"));
        testThread.start();
        try {
            Thread.sleep(100);
            testThread.interrupt();
            testThread.join(500);
            assertFalse(testThread.isAlive());
        } catch (InterruptedException e) {
            fail("Test thread was interrupted");
        }
    }
}