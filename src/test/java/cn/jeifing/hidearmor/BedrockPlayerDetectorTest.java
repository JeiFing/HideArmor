package cn.jeifing.hidearmor;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BedrockPlayerDetectorTest {
    @Test
    void cachesFloodgatePlayersAndRemovesThemOnQuit() {
        UUID bedrockId = UUID.fromString("12345678-1234-5678-9234-567812345678");
        BedrockPlayerDetector detector = new BedrockPlayerDetector(bedrockId::equals);

        detector.track(bedrockId);
        assertTrue(detector.isBedrock(bedrockId));

        detector.untrack(bedrockId);
        assertFalse(detector.isBedrock(bedrockId));
    }

    @Test
    void recognizesFloodgateUuidShapeBeforeJoinCacheIsReady() {
        UUID floodgateShape = UUID.fromString("00000000-0000-0000-1234-567812345678");
        BedrockPlayerDetector detector = new BedrockPlayerDetector(ignored -> false);

        assertTrue(BedrockPlayerDetector.hasFloodgateUuidShape(floodgateShape));
        assertTrue(detector.isBedrock(floodgateShape));
    }

    @Test
    void ordinaryJavaUuidDoesNotMatchFallback() {
        UUID javaId = UUID.fromString("12345678-1234-5678-9234-567812345678");
        BedrockPlayerDetector detector = new BedrockPlayerDetector(ignored -> false);

        detector.track(javaId);
        assertFalse(BedrockPlayerDetector.hasFloodgateUuidShape(javaId));
        assertFalse(detector.isBedrock(javaId));
    }
}
