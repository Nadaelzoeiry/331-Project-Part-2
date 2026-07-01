import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class VotingController {
    private int lastValidAltitude;
    private int consecutiveFailures;
    private final FileWriter writer;

    public VotingController(int initialAltitude, FileWriter writer) {
        // I initialized the tracking states and passed the file logger reference
        this.lastValidAltitude = initialAltitude;
        this.consecutiveFailures = 0;
        this.writer = writer;
    }

    public int vote(List<Integer> outcomes, List<String> sensorIds) throws SystemReliabilityException {
        List<Integer> validValues = new ArrayList<Integer>();
        List<String> validIds = new ArrayList<String>();

        // I filtered out all null readings and out-of-bounds numbers first
        for (int i = 0; i < outcomes.size(); i++) {
            Integer reading = outcomes.get(i);
            if (reading != null && AltitudeSensor.isValid(reading)) {
                validValues.add(reading);
                validIds.add(sensorIds.get(i));
            }
        }

        // I triggered an immediate failure check if fewer than 2 sensors are healthy
        if (validValues.size() < 2) 
            return handleFailure("Reliability failure, fewer than 2 valid sensors in this Z");

        // I checked every pair of healthy sensors to look for matching values
        for (int i = 0; i < validValues.size(); i++) {
            for (int j = i + 1; j < validValues.size(); j++) {
                if (validValues.get(i).equals(validValues.get(j))) {
                    int agreedAltitude = validValues.get(i);
                    
                    // I extracted rogue/broken sensors to highlight them in the logs
                    List<String> outliers = findOutliers(validIds, validValues, agreedAltitude, sensorIds, outcomes);
                    String outlierNote;
                    if (outliers.isEmpty()) {
                        outlierNote = "no outliers";
                    } else {
                        outlierNote = "outlier(s): Sensor " + String.join(", Sensor ", outliers);
                    }
                    
                    // I reset the fault counter on success and returned the agreed value
                    String msg = "Altitude = " + agreedAltitude + " m  (agreed: Sensor " + validIds.get(i) + " & Sensor " + validIds.get(j) + "; " + outlierNote + ")";
                    log(msg);
                    lastValidAltitude = agreedAltitude;
                    consecutiveFailures = 0;
                    return agreedAltitude;
                }
            }
        }
        // I triggered fallback mechanics if sensors are healthy but don't agree
        return handleFallback();
    }

    public int getLastValidAltitude() { return lastValidAltitude; }
    public int getConsecutiveFailures() { return consecutiveFailures; }

    private int handleFailure(String reason) throws SystemReliabilityException {
        // I increased consecutive errors and evaluated safety-halt conditions
        consecutiveFailures++;
        String msg = "[RELIABILITY] " + reason + "  (consecutive failures: " + consecutiveFailures + ")";
        log(msg);

        if (consecutiveFailures >= 2) {
            String safeMsg = "SAFE MODE: Two consecutive reliability failures, system entering SAFE MODE and halting";
            System.out.println("  " + safeMsg);
            throw new SystemReliabilityException("Two consecutive voting failures detected, SAFE MODE activated");
        }
        return lastValidAltitude;
    }

    private int handleFallback() throws SystemReliabilityException {
        // I used old data when a majority vote couldn't be calculated
        consecutiveFailures++;
        String msg = "[FALLBACK] No majority found, using last valid altitude: " + lastValidAltitude + " m  (consecutive failures: " + consecutiveFailures + ")";
        log(msg);

        if (consecutiveFailures >= 2) {
            String safeMsg = "SAFE MODE: Two consecutive reliability failures, system entering SAFE MODE and halting";
            System.out.println("  " + safeMsg);
            throw new SystemReliabilityException("Two consecutive voting failures detected, SAFE MODE activated");
        }
        return lastValidAltitude;
    }

    private void log(String msg) {
        // I wrote to both the console stream and the raw text log file
        println(msg);
        try {
            writer.write(msg + "\n");
        } catch (IOException e) {
            println("Could not write to log file: " + e.getMessage());
        }
    }

    private void println(String msg) {
        System.out.println("  " + msg);
    }

    private List<String> findOutliers(List<String> validIds, List<Integer> validValues, int majority, List<String> allIds, List<Integer> allOutcomes) {
        List<String> outliers = new ArrayList<String>();
        
        // I checked for sensors that are valid but disagree with the majority value
        for (int i = 0; i < validIds.size(); i++) {
            if (!validValues.get(i).equals(majority)) outliers.add(validIds.get(i));
        }
        
        // I added sensors that skipped validation entirely due to corruption/nulls
        for (int i = 0; i < allIds.size(); i++) {
            Integer v = allOutcomes.get(i);
            if (v == null || !AltitudeSensor.isValid(v)) {
                String id = allIds.get(i);
                if (!outliers.contains(id)) outliers.add(id);
            }
        }
        return outliers;
    }
}
