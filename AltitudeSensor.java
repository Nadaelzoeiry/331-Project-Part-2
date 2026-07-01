import java.util.Random;

public class AltitudeSensor {

    // Altitude boundaries
    public static final int MIN_ALTITUDE = 0;
    public static final int MAX_ALTITUDE = 200;
    private static final int VALID_RANGE = MAX_ALTITUDE;
    
    // Bounds for generating corrupted data
    private static final int CORRUPTED_OFFSET = 201;
    private static final int CORRUPTED_RANGE = 100;

    private final String sensorId;
    private final Random random;

    public AltitudeSensor(String sensorId) {
        this.sensorId = sensorId;
        this.random = new Random();
    }

    public int readSensor() throws SensorReadException {
        int chance = random.nextInt(100);

        if (chance < 15) {
            // 15% chance: Hardware failure
            throw new SensorReadException(
                    "Sensor " + sensorId + " failed (hardware fault)");
        } else if (chance < 30) {
            // 15% chance: Out-of-bounds corrupted data (201 to 300)
            return CORRUPTED_OFFSET + random.nextInt(CORRUPTED_RANGE);
        } else {
            // 70% chance: Valid altitude data (0 to 199)
            return MIN_ALTITUDE + random.nextInt(VALID_RANGE);
        }
    }

    public String getSensorId() {
        return sensorId;
    }

    // Helper to verify if a reading falls within safety limits
    public static boolean isValid(int value) {
        return value >= MIN_ALTITUDE && value <= MAX_ALTITUDE;
    }
}
