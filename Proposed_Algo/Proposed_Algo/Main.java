
package Proposed_Algo;

import java.util.*;

class Main {

    // ---------------------------------------------------------
    // DATA CLASSES
    // ---------------------------------------------------------

    static class fogNode {
        int Id;
        double computational_power_of_fogNode;
        double power_of_fogNode;
        double deadline;
        double computational_demand;
        double energy;
        double inputSize;
        double weightage;
        double quota;
    }

    static class iotDevice {
        int Id;
        double computational_demand;
        double inputSize;
        double energy;
        double latency;
        double weightage;
        double dependency;
    }

    static class Binarycases {
        double energy;
        double total_time;
    }

    static class criteria_Weight {
        double CW_of_Energy_of_iot;
        double CW_of_Latency_of_iot;
        double CW_of_Computational_demand_of_iot;
        double CW_of_Energy_of_fog;
        double CW_of_deadline_of_fog;
    }

    static class ModeResult {
        String mode;
        String fogWeights;
        String iotWeights;

        int[] fogOrder;
        int[] iotOrder;

        double totalEnergy;
        double totalLatency;

        double matchedEnergy;
        double matchedLatency;

        int outages;
        int matchedTasks;
        int deadlineMet;
        int totalTasks;
    }

    static Scanner SC = new Scanner(System.in);

    static final String[] MODES = {
        "EQUAL", "AHP", "ENTROPY"
    };

    // true = cost criterion (smaller is better)
    // false = benefit criterion (larger is better)
    static final boolean[] FOG_IS_COST = {
        true, true
    };

    static final boolean[] IOT_IS_COST = {
        true, true, true
    };

    // ---------------------------------------------------------
    // BASIC UTILITY METHODS
    // ---------------------------------------------------------

    public static double log2(double x) {
        return Math.log(x) / Math.log(2);
    }

    public static double log10(int x) {
        return Math.log(x) / Math.log(10);
    }

    public static String fmt(double[] w) {
        StringBuilder sb = new StringBuilder("[");

        for (int j = 0; j < w.length; j++) {
            sb.append(j > 0 ? ", " : "");
            sb.append(String.format(Locale.US, "%.4f", w[j]));
        }

        return sb.append("]").toString();
    }

    public static String orderString(int[] ids, int max) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < ids.length && i < max; i++) {
            sb.append(i > 0 ? " " : "");
            sb.append(ids[i]);
        }

        if (ids.length > max) {
            sb.append(" ...");
        }

        return sb.toString();
    }

    private static void validateMatrix(double[][] m) {
        if (m == null || m.length == 0 || m[0] == null
                || m[0].length == 0) {
            throw new IllegalArgumentException(
                "Decision matrix cannot be empty."
            );
        }

        int cols = m[0].length;

        for (int i = 0; i < m.length; i++) {
            if (m[i] == null || m[i].length != cols) {
                throw new IllegalArgumentException(
                    "Decision matrix must be rectangular."
                );
            }

            for (int j = 0; j < cols; j++) {
                if (!Double.isFinite(m[i][j]) || m[i][j] < 0) {
                    throw new IllegalArgumentException(
                        "Decision matrix values must be finite and non-negative."
                    );
                }
            }
        }
    }

    private static void validateWeights(
            double[] weights, int cols) {

        if (weights == null || weights.length != cols) {
            throw new IllegalArgumentException(
                "Weight count does not match decision matrix."
            );
        }

        double sum = 0;

        for (double weight : weights) {
            if (!Double.isFinite(weight) || weight < 0) {
                throw new IllegalArgumentException(
                    "Weights must be finite and non-negative."
                );
            }

            sum += weight;
        }

        if (sum <= 0) {
            throw new IllegalArgumentException(
                "At least one criterion weight must be positive."
            );
        }
    }

    // ---------------------------------------------------------
    // COPY METHODS
    // ---------------------------------------------------------

    public static fogNode copyFog(fogNode f) {
        fogNode c = new fogNode();

        c.Id = f.Id;
        c.computational_power_of_fogNode =
            f.computational_power_of_fogNode;
        c.power_of_fogNode = f.power_of_fogNode;
        c.deadline = f.deadline;
        c.computational_demand = f.computational_demand;
        c.energy = f.energy;
        c.inputSize = f.inputSize;
        c.weightage = f.weightage;
        c.quota = f.quota;

        return c;
    }

    public static iotDevice copyIot(iotDevice t) {
        iotDevice c = new iotDevice();

        c.Id = t.Id;
        c.computational_demand = t.computational_demand;
        c.inputSize = t.inputSize;
        c.energy = t.energy;
        c.latency = t.latency;
        c.weightage = t.weightage;
        c.dependency = t.dependency;

        return c;
    }

    // ---------------------------------------------------------
    // AHP WEIGHT CALCULATION
    // ---------------------------------------------------------

    /*
     * Calculates AHP weights using column normalization
     * and the arithmetic mean of normalized rows.
     *
     * Also calculates the AHP consistency ratio correctly:
     *
     * Aw = A * W
     * lambda max = average(Aw[i] / W[i])
     * CI = (lambda max - n) / (n - 1)
     * CR = CI / RI
     */

    public static double[] calculateAHPWeights(
            double[][] matrix, String matrixName) {

        int n = matrix.length;

        if (n == 0) {
            throw new IllegalArgumentException(
                "AHP matrix cannot be empty."
            );
        }

        for (int i = 0; i < n; i++) {
            if (matrix[i].length != n) {
                throw new IllegalArgumentException(
                    "AHP matrix must be square."
                );
            }

            if (matrix[i][i] <= 0) {
                throw new IllegalArgumentException(
                    "AHP diagonal values must be positive."
                );
            }

            for (int j = 0; j < n; j++) {
                if (matrix[i][j] <= 0
                        || !Double.isFinite(matrix[i][j])) {
                    throw new IllegalArgumentException(
                        "AHP values must be positive and finite."
                    );
                }
            }
        }

        double[] columnSum = new double[n];

        for (int j = 0; j < n; j++) {
            for (int i = 0; i < n; i++) {
                columnSum[j] += matrix[i][j];
            }
        }

        double[][] normalized = new double[n][n];

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                normalized[i][j] =
                    matrix[i][j] / columnSum[j];
            }
        }

        double[] weights = new double[n];

        for (int i = 0; i < n; i++) {
            double rowSum = 0;

            for (int j = 0; j < n; j++) {
                rowSum += normalized[i][j];
            }

            weights[i] = rowSum / n;
        }

        // Normalize weights to ensure their sum is 1.
        double weightSum = 0;

        for (double weight : weights) {
            weightSum += weight;
        }

        for (int i = 0; i < n; i++) {
            weights[i] /= weightSum;
        }

        // Calculate consistency ratio.
        double lambdaMax = 0;

        for (int i = 0; i < n; i++) {
            double aw = 0;

            for (int j = 0; j < n; j++) {
                aw += matrix[i][j] * weights[j];
            }

            lambdaMax += aw / weights[i];
        }

        lambdaMax /= n;

        double CI = 0;
        double CR = 0;

        if (n > 1) {
            CI = (lambdaMax - n) / (n - 1);
        }

        double RI = 0;

        if (n == 3) RI = 0.58;
        else if (n == 4) RI = 0.90;
        else if (n == 5) RI = 1.12;
        else if (n == 6) RI = 1.24;
        else if (n == 7) RI = 1.32;
        else if (n == 8) RI = 1.41;
        else if (n == 9) RI = 1.45;
        else if (n == 10) RI = 1.49;

        if (n <= 2 || RI == 0) {
            CR = 0;
        } else {
            CR = CI / RI;
        }

        System.out.println("\nAHP consistency check: " + matrixName);
        System.out.println(
            "Lambda max = " +
            String.format(Locale.US, "%.4f", lambdaMax)
        );
        System.out.println(
            "Consistency Index (CI) = " +
            String.format(Locale.US, "%.4f", CI)
        );
        System.out.println(
            "Consistency Ratio (CR) = " +
            String.format(Locale.US, "%.4f", CR)
        );

        if (CR < 0.10) {
            System.out.println("Matrix is consistent.");
        } else {
            System.out.println(
                "WARNING: Matrix is inconsistent. Review pairwise comparisons."
            );
        }

        return weights;
    }

    public static criteria_Weight AHP() {

        criteria_Weight cw = new criteria_Weight();

        /*
         * IoT criteria order:
         * 0 = Energy
         * 1 = Latency
         * 2 = Computational demand
         */

        double[][] iotMatrix = {
            {1.0, 4.0, 5.0},
            {0.25, 1.0, 3.0},
            {0.2, 0.33, 1.0}
        };

        double[] iotWeights =
            calculateAHPWeights(iotMatrix, "IoT criteria");

        cw.CW_of_Energy_of_iot = iotWeights[0];
        cw.CW_of_Latency_of_iot = iotWeights[1];
        cw.CW_of_Computational_demand_of_iot =
            iotWeights[2];

        /*
         * Fog criteria order:
         * 0 = Energy
         * 1 = Deadline
         */

        double[][] fogMatrix = {
            {1.0, 5.0},
            {0.2, 1.0}
        };

        double[] fogWeights =
            calculateAHPWeights(fogMatrix, "Fog criteria");

        cw.CW_of_Energy_of_fog = fogWeights[0];
        cw.CW_of_deadline_of_fog = fogWeights[1];

        return cw;
    }

    // ---------------------------------------------------------
    // EQUAL WEIGHT METHOD
    // ---------------------------------------------------------

    public static double[] equalWeights(int cols) {

        if (cols <= 0) {
            throw new IllegalArgumentException(
                "Number of criteria must be positive."
            );
        }

        double[] w = new double[cols];

        for (int j = 0; j < cols; j++) {
            w[j] = 1.0 / cols;
        }

        return w;
    }

    // ---------------------------------------------------------
    // ENTROPY WEIGHT METHOD
    // ---------------------------------------------------------

    /*
     * Entropy weighting:
     *
     * 1. Normalize each criterion using its column sum.
     * 2. Calculate entropy for each criterion.
     * 3. Calculate diversification degree.
     * 4. Normalize diversification degrees into weights.
     *
     * A criterion with more variation receives more weight.
     */

    public static double[] entropyWeights(double[][] m) {

        validateMatrix(m);

        int rows = m.length;
        int cols = m[0].length;

        double[] weights = new double[cols];

        if (rows < 2) {
            return equalWeights(cols);
        }

        double k = 1.0 / Math.log(rows);

        double[] diversification = new double[cols];

        double diversificationSum = 0;

        for (int j = 0; j < cols; j++) {

            double columnSum = 0;

            for (int i = 0; i < rows; i++) {
                columnSum += m[i][j];
            }

            double entropy = 1.0;

            if (columnSum > 0) {

                entropy = 0;

                for (int i = 0; i < rows; i++) {

                    double p = m[i][j] / columnSum;

                    if (p > 0) {
                        entropy -= p * Math.log(p);
                    }
                }

                entropy *= k;

                // Avoid small floating-point errors.
                entropy = Math.max(0, Math.min(1, entropy));
            }

            diversification[j] = 1.0 - entropy;

            diversificationSum += diversification[j];
        }

        if (diversificationSum <= 1e-12) {
            return equalWeights(cols);
        }

        for (int j = 0; j < cols; j++) {
            weights[j] =
                diversification[j] / diversificationSum;
        }

        return weights;
    }

    // ---------------------------------------------------------
    // SELECT WEIGHTING METHOD
    // ---------------------------------------------------------

    public static double[] criteriaWeights(
            String mode,
            double[][] m,
            double[] ahpWeights) {

        if (mode.equals("EQUAL")) {
            return equalWeights(m[0].length);
        }

        if (mode.equals("AHP")) {

            validateWeights(ahpWeights, m[0].length);

            double[] normalized = ahpWeights.clone();

            double sum = 0;

            for (double x : normalized) {
                sum += x;
            }

            for (int i = 0; i < normalized.length; i++) {
                normalized[i] /= sum;
            }

            return normalized;
        }

        if (mode.equals("ENTROPY")) {
            return entropyWeights(m);
        }

        throw new IllegalArgumentException(
            "Unknown weighting method: " + mode
        );
    }

    // ---------------------------------------------------------
    // TOPSIS RANKING
    // ---------------------------------------------------------

    /*
     * TOPSIS steps:
     *
     * 1. Construct decision matrix.
     * 2. Normalize the decision matrix.
     * 3. Apply criterion weights.
     * 4. Determine ideal best and ideal worst.
     * 5. Calculate distances from ideal solutions.
     * 6. Calculate closeness coefficient.
     *
     * Higher closeness coefficient = better alternative.
     */

    public static double[] topsisScores(
            double[][] m,
            double[] w,
            boolean[] isCost) {

        validateMatrix(m);

        int rows = m.length;
        int cols = m[0].length;

        validateWeights(w, cols);

        if (isCost == null || isCost.length != cols) {
            throw new IllegalArgumentException(
                "TOPSIS criterion types do not match matrix."
            );
        }

        double[][] v = new double[rows][cols];

        // Step 1: Vector normalization and weighting.
        for (int j = 0; j < cols; j++) {

            double squareSum = 0;

            for (int i = 0; i < rows; i++) {
                squareSum += m[i][j] * m[i][j];
            }

            double norm = Math.sqrt(squareSum);

            for (int i = 0; i < rows; i++) {

                double normalized =
                    norm > 0 ? m[i][j] / norm : 0;

                v[i][j] = normalized * w[j];
            }
        }

        // Step 2: Determine ideal best and ideal worst.
        double[] best = new double[cols];
        double[] worst = new double[cols];

        for (int j = 0; j < cols; j++) {

            double max = v[0][j];
            double min = v[0][j];

            for (int i = 1; i < rows; i++) {
                max = Math.max(max, v[i][j]);
                min = Math.min(min, v[i][j]);
            }

            if (isCost[j]) {
                best[j] = min;
                worst[j] = max;
            } else {
                best[j] = max;
                worst[j] = min;
            }
        }

        // Step 3: Calculate distances and closeness scores.
        double[] scores = new double[rows];

        for (int i = 0; i < rows; i++) {

            double dPlus = 0;
            double dMinus = 0;

            for (int j = 0; j < cols; j++) {

                double diffBest = v[i][j] - best[j];
                double diffWorst = v[i][j] - worst[j];

                dPlus += diffBest * diffBest;
                dMinus += diffWorst * diffWorst;
            }

            dPlus = Math.sqrt(dPlus);
            dMinus = Math.sqrt(dMinus);

            double denominator = dPlus + dMinus;

            scores[i] =
                denominator > 1e-12
                ? dMinus / denominator
                : 0.5;
        }

        return scores;
    }

    // ---------------------------------------------------------
    // MATCHING ALGORITHM
    // ---------------------------------------------------------

    /*
     * Greedy one-to-many matching:
     *
     * Each IoT task is considered in its ranking order.
     * It is assigned to the first fog node with sufficient quota.
     * If no fog node has sufficient quota, it becomes an outage.
     *
     * Matching stores:
     * IoT task ID -> Fog node ID
     * -1 means no assignment.
     */

    public static int MatchingAlgo(
            iotDevice[] iot,
            fogNode[] fog,
            int count) {

        return MatchingAlgo(
            iot, fog, count,
            new HashMap<Integer, Integer>()
        );
    }

    public static int MatchingAlgo(
            iotDevice[] iot,
            fogNode[] fog,
            int count,
            HashMap<Integer, Integer> matching) {

        for (int i = 0; i < iot.length; i++) {

            boolean assigned = false;

            for (int j = 0; j < fog.length; j++) {

                if (fog[j].quota >= iot[i].dependency) {

                    fog[j].quota -= iot[i].dependency;

                    matching.put(
                        iot[i].Id,
                        fog[j].Id
                    );

                    assigned = true;

                    break;
                }
            }

            if (!assigned) {

                matching.put(iot[i].Id, -1);

                count++;
            }
        }

        return count;
    }

    // ---------------------------------------------------------
    // READ FOG NODES
    // ---------------------------------------------------------

    public static fogNode[] readFogNodes(
            double downlink_data_rate,
            double uplink_data_rate,
            double output_size,
            double capacity_of_VRU) {

        System.out.println("\nEnter the number of fogNodes:");

        int n = SC.nextInt();

        if (n <= 0) {
            throw new IllegalArgumentException(
                "Number of fog nodes must be greater than zero."
            );
        }

        fogNode[] fog = new fogNode[n];

        for (int i = 0; i < n; i++) {

            fog[i] = new fogNode();

            fog[i].Id = i + 1;

            System.out.println(
                "\nEnter the following details for fog node "
                + (i + 1)
            );

            System.out.println("Power of fog node (Watts):");
            fog[i].power_of_fogNode = SC.nextDouble();

            System.out.println(
                "Computational power of fog node:"
            );
            fog[i].computational_power_of_fogNode =
                SC.nextDouble();

            System.out.println("Deadline of fog node:");
            fog[i].deadline = SC.nextDouble();

            System.out.println("Input size of fog node in Kb:");
            fog[i].inputSize = SC.nextDouble();

            System.out.println("Computational demand of fog node:");
            fog[i].computational_demand = SC.nextDouble();

            System.out.println("Enter quota of fog node:");
            fog[i].quota = SC.nextDouble();

            if (fog[i].quota < 0) {
                throw new IllegalArgumentException(
                    "Fog node quota cannot be negative."
                );
            }

            double upload_latency =
                fog[i].inputSize /
                (uplink_data_rate * 1000);

            double download_latency =
                output_size /
                (downlink_data_rate * 1000);

            double computational_latency =
                fog[i].computational_demand /
                capacity_of_VRU;

            fog[i].energy =
                fog[i].power_of_fogNode *
                (upload_latency + download_latency)
                +
                computational_latency *
                fog[i].computational_power_of_fogNode;
        }

        return fog;
    }

    // ---------------------------------------------------------
    // BINARY CASE 1
    // BOTH TASKS EXECUTED LOCALLY
    // ---------------------------------------------------------

    public static Binarycases both_locally_executed(
            double local_execution_time,
            double uplink_time,
            double downlink_time,
            double power_of_iotd1,
            double power_of_iotd2,
            double computational_power_of_fognode,
            double computational_delay,
            double X,
            double S,
            double N,
            double power_of_fogDevice) {

        double total_time =
            2 * local_execution_time
            + uplink_time
            + downlink_time;

        double total_energy =
            power_of_iotd1 * local_execution_time
            +
            power_of_iotd2 * local_execution_time
            +
            (uplink_time + downlink_time) *
            power_of_fogDevice;

        double computation_time_for_iotd1 =
            (uplink_time + computational_delay) * (X - 1);

        double computation_time_for_iotd2 =
            (uplink_time + computational_delay) * (S - 1);

        double remaining_time =
            (uplink_time + computational_delay) * (N - S);

        double waiting_time =
            Math.max(
                computation_time_for_iotd1,
                computation_time_for_iotd2
            );

        total_energy +=
            (X - 2 + N) *
            (
                computational_power_of_fognode *
                computational_delay
                +
                power_of_fogDevice *
                (uplink_time + downlink_time)
            );

        total_time += remaining_time + waiting_time;

        Binarycases bn = new Binarycases();

        bn.total_time = total_time;
        bn.energy = total_energy;

        System.out.println(
            "CASE 1: Both tasks are executed locally."
        );

        return bn;
    }

    // ---------------------------------------------------------
    // BINARY CASE 2
    // FIRST TASK ON FOG, SECOND TASK LOCALLY
    // ---------------------------------------------------------

    public static Binarycases first_onfog_second_locally(
            double local_execution_time,
            double uplink_time,
            double downlink_time,
            double power_of_iotd1,
            double power_of_iotd2,
            double computational_power_of_fognode,
            double computational_delay,
            double X,
            double S,
            double N,
            double power_of_fogDevice) {

        double total_time =
            local_execution_time
            + uplink_time
            + downlink_time
            + computational_delay;

        double total_energy =
            power_of_fogDevice *
            (uplink_time + downlink_time)
            +
            computational_power_of_fognode *
            computational_delay
            +
            power_of_iotd2 * local_execution_time;

        double computation_time_for_iotd1 =
            (uplink_time + computational_delay) * (X - 1);

        double computation_time_for_iotd2 =
            (uplink_time + computational_delay) * (S - 1);

        double remaining_time =
            (uplink_time + computational_delay) * (N - S);

        double waiting_time =
            Math.max(
                computation_time_for_iotd1,
                computation_time_for_iotd2
            );

        total_energy +=
            (X - 2 + N) *
            (
                computational_power_of_fognode *
                computational_delay
                +
                power_of_fogDevice *
                (uplink_time + downlink_time)
            );

        total_time += remaining_time + waiting_time;

        Binarycases bn = new Binarycases();

        bn.total_time = total_time;
        bn.energy = total_energy;

        System.out.println(
            "CASE 2: IoT device 1 executes at fog and device 2 locally."
        );

        return bn;
    }

    // ---------------------------------------------------------
    // BINARY CASE 3
    // FIRST TASK LOCALLY, SECOND TASK ON FOG
    // ---------------------------------------------------------

    public static Binarycases first_locally_second_onfog(
            double local_execution_time,
            double uplink_time,
            double downlink_time,
            double power_of_iotd1,
            double power_of_iotd2,
            double computational_power_of_fognode,
            double computational_delay,
            double X,
            double S,
            double N,
            double power_of_fogDevice) {

        double total_time =
            local_execution_time
            + 2 * uplink_time
            + computational_delay;

        double total_energy =
            2 * uplink_time * power_of_fogDevice
            +
            computational_delay *
            computational_power_of_fognode
            +
            power_of_iotd1 * local_execution_time;

        double computation_time_for_iotd1 =
            (uplink_time + computational_delay) * (X - 1);

        double computation_time_for_iotd2 =
            (uplink_time + computational_delay) * (S - 1);

        double remaining_time =
            (uplink_time + computational_delay) * (N - S);

        double waiting_time =
            Math.max(
                computation_time_for_iotd1,
                computation_time_for_iotd2
            );

        total_energy +=
            (X - 2 + N) *
            (
                computational_power_of_fognode *
                computational_delay
                +
                power_of_fogDevice *
                (uplink_time + downlink_time)
            );

        total_time += remaining_time + waiting_time;

        Binarycases bn = new Binarycases();

        bn.total_time = total_time;
        bn.energy = total_energy;

        System.out.println(
            "CASE 3: IoT device 1 executes locally and device 2 at fog."
        );

        return bn;
    }

    // ---------------------------------------------------------
    // BINARY CASE 4
    // BOTH TASKS EXECUTED ON FOG
    // ---------------------------------------------------------

    public static Binarycases both_onfog(
            double local_execution_time,
            double uplink_time,
            double downlink_time,
            double power_of_iotd1,
            double power_of_iotd2,
            double computational_power_of_fognode,
            double computational_delay,
            double X,
            double S,
            double N,
            double power_of_fogDevice) {

        double total_time =
            2 * uplink_time
            + 2 * computational_delay;

        double total_energy =
            computational_power_of_fognode *
            (2 * uplink_time + 2 * computational_delay);

        double computation_time_for_iotd1 =
            (uplink_time + computational_delay) * (X - 1);

        double computation_time_for_iotd2 =
            (uplink_time + computational_delay) * (S - 1);

        double remaining_time =
            (uplink_time + computational_delay) * (N - S);

        double waiting_time =
            Math.max(
                computation_time_for_iotd1,
                computation_time_for_iotd2
            );

        total_energy +=
            (X - 2 + N) *
            (
                computational_power_of_fognode *
                computational_delay
                +
                power_of_fogDevice *
                (uplink_time + downlink_time)
            );

        total_time += remaining_time + waiting_time;

        Binarycases bn = new Binarycases();

        bn.total_time = total_time;
        bn.energy = total_energy;

        System.out.println(
            "CASE 4: Both IoT devices execute at fog."
        );

        return bn;
    }

    // ---------------------------------------------------------
    // READ IOT TASKS
    // ---------------------------------------------------------

    public static iotDevice[] readIotTasks(
            double local_execution_time,
            double uplink_time,
            double downlink_time,
            double power_of_iotd1,
            double power_of_iotd2,
            double computational_power_of_fognode,
            double computational_delay,
            double X,
            double S,
            double N,
            double power_of_fogDevice,
            double downlink_data_rate,
            double uplink_data_rate,
            double output_size,
            double capacity_of_VRU) {

        System.out.println(
            "\nEnter the number of independent tasks:"
        );

        int number_of_independent_tasks = SC.nextInt();

        if (number_of_independent_tasks < 0) {
            throw new IllegalArgumentException(
                "Number of independent tasks cannot be negative."
            );
        }

        int n = number_of_independent_tasks + 4;

        iotDevice[] iot = new iotDevice[n];

        Binarycases bn;

        // First four tasks represent the four binary cases.
        for (int i = 0; i < 4; i++) {

            if (i == 0) {

                bn = both_locally_executed(
                    local_execution_time,
                    uplink_time,
                    downlink_time,
                    power_of_iotd1,
                    power_of_iotd2,
                    computational_power_of_fognode,
                    computational_delay,
                    X, S, N,
                    power_of_fogDevice
                );

            } else if (i == 1) {

                bn = first_onfog_second_locally(
                    local_execution_time,
                    uplink_time,
                    downlink_time,
                    power_of_iotd1,
                    power_of_iotd2,
                    computational_power_of_fognode,
                    computational_delay,
                    X, S, N,
                    power_of_fogDevice
                );

            } else if (i == 2) {

                bn = first_locally_second_onfog(
                    local_execution_time,
                    uplink_time,
                    downlink_time,
                    power_of_iotd1,
                    power_of_iotd2,
                    computational_power_of_fognode,
                    computational_delay,
                    X, S, N,
                    power_of_fogDevice
                );

            } else {

                bn = both_onfog(
                    local_execution_time,
                    uplink_time,
                    downlink_time,
                    power_of_iotd1,
                    power_of_iotd2,
                    computational_power_of_fognode,
                    computational_delay,
                    X, S, N,
                    power_of_fogDevice
                );
            }

            iot[i] = new iotDevice();

            iot[i].Id = i + 1;
            iot[i].energy = bn.energy;
            iot[i].dependency = 2;
            iot[i].latency = bn.total_time;

            iot[i].computational_demand = 210;
            iot[i].inputSize = 400;
        }

        // Remaining tasks are independent tasks.
        for (int i = 4; i < n; i++) {

            iot[i] = new iotDevice();

            iot[i].Id = i + 1;
            iot[i].dependency = 1;

            System.out.println(
                "\nEnter computational demand of task "
                + (i + 1) + ":"
            );

            iot[i].computational_demand = SC.nextDouble();

            System.out.println(
                "Enter input size of task " + (i + 1) + ":"
            );

            iot[i].inputSize = SC.nextDouble();

            if (iot[i].computational_demand < 300) {

                // Local execution.
                iot[i].latency =
                    iot[i].computational_demand / 16.0;

                iot[i].energy =
                    power_of_iotd1 * iot[i].latency;

            } else {

                // Fog execution estimate.
                double upload =
                    iot[i].inputSize /
                    (uplink_data_rate * 1000);

                double computation =
                    iot[i].computational_demand /
                    capacity_of_VRU;

                double download =
                    output_size /
                    (downlink_data_rate * 1000);

                iot[i].latency =
                    upload + computation + download;

                iot[i].energy =
                    power_of_fogDevice * (upload + download)
                    +
                    computational_power_of_fognode *
                    computation;
            }
        }

        return iot;
    }

    // ---------------------------------------------------------
    // FOG NODE RANKING
    // ---------------------------------------------------------

    public static fogNode[] rankFog(
            fogNode[] base,
            String mode,
            double[] ahpWeights,
            ModeResult res) {

        int n = base.length;

        fogNode[] fog = new fogNode[n];

        for (int i = 0; i < n; i++) {
            fog[i] = copyFog(base[i]);
        }

        // Fog criteria: energy and deadline.
        double[][] decision = new double[n][2];

        for (int i = 0; i < n; i++) {
            decision[i][0] = fog[i].energy;
            decision[i][1] = fog[i].deadline;
        }

        double[] weights =
            criteriaWeights(mode, decision, ahpWeights);

        double[] scores =
            topsisScores(decision, weights, FOG_IS_COST);

        for (int i = 0; i < n; i++) {
            fog[i].weightage = scores[i];
        }

        res.fogWeights = fmt(weights);

        // Sort by descending TOPSIS score.
        for (int i = 0; i < n; i++) {

            for (int j = i + 1; j < n; j++) {

                if (fog[i].weightage < fog[j].weightage) {

                    fogNode temp = fog[i];

                    fog[i] = fog[j];
                    fog[j] = temp;
                }
            }
        }

        res.fogOrder = new int[n];

        for (int i = 0; i < n; i++) {
            res.fogOrder[i] = fog[i].Id;
        }

        return fog;
    }

    // ---------------------------------------------------------
    // IOT TASK RANKING
    // ---------------------------------------------------------

    public static iotDevice[] rankIot(
            iotDevice[] base,
            String mode,
            double[] ahpWeights,
            ModeResult res) {

        int n = base.length;

        iotDevice[] iot = new iotDevice[n];

        for (int i = 0; i < n; i++) {
            iot[i] = copyIot(base[i]);
        }

        /*
         * Decision matrix order:
         * 0 = Energy
         * 1 = Computational demand
         * 2 = Latency
         */

        double[][] decision = new double[n][3];

        for (int i = 0; i < n; i++) {

            decision[i][0] = iot[i].energy;
            decision[i][1] = iot[i].computational_demand;
            decision[i][2] = iot[i].latency;
        }

        double[] weights =
            criteriaWeights(mode, decision, ahpWeights);

        double[] scores =
            topsisScores(decision, weights, IOT_IS_COST);

        for (int i = 0; i < n; i++) {
            iot[i].weightage = scores[i];
        }

        res.iotWeights = fmt(weights);

        // Sort tasks by descending TOPSIS score.
        for (int i = 0; i < n; i++) {

            for (int j = i + 1; j < n; j++) {

                if (iot[i].weightage < iot[j].weightage) {

                    iotDevice temp = iot[i];

                    iot[i] = iot[j];
                    iot[j] = temp;
                }
            }
        }

        res.iotOrder = new int[n];

        for (int i = 0; i < n; i++) {
            res.iotOrder[i] = iot[i].Id;
        }

        return iot;
    }

    // ---------------------------------------------------------
    // RUN ONE WEIGHTING METHOD
    // ---------------------------------------------------------

    public static ModeResult runMode(
            String mode,
            fogNode[] fogBase,
            iotDevice[] iotBase,
            double[] fogAhp,
            double[] iotAhp) {

        System.out.println(
            "\n\n===================================================="
        );

        System.out.println(
            "WEIGHTING METHOD: " + mode + " + TOPSIS"
        );

        System.out.println(
            "===================================================="
        );

        ModeResult res = new ModeResult();

        res.mode = mode;

        // Rank fog nodes.
        fogNode[] fog =
            rankFog(fogBase, mode, fogAhp, res);

        // Rank IoT tasks.
        iotDevice[] iot =
            rankIot(iotBase, mode, iotAhp, res);

        System.out.println(
            "Fog weights [energy, deadline] = "
            + res.fogWeights
        );

        System.out.println(
            "IoT weights [energy, demand, latency] = "
            + res.iotWeights
        );

        System.out.println(
            "Fog node ranking: "
            + orderString(res.fogOrder, 20)
        );

        System.out.println(
            "IoT task ranking: "
            + orderString(res.iotOrder, 20)
        );

        // Perform matching.
        HashMap<Integer, Integer> matching =
            new HashMap<Integer, Integer>();

        res.outages =
            MatchingAlgo(iot, fog, 0, matching);

        // Store fog nodes by their original IDs.
        HashMap<Integer, fogNode> fogById =
            new HashMap<Integer, fogNode>();

        for (int i = 0; i < fog.length; i++) {
            fogById.put(fog[i].Id, fog[i]);
        }

        /*
         * These are baseline estimates calculated from
         * the original task and fog-node input formulas.
         *
         * They are independent of which fog node is assigned.
         * They are therefore not expected to change between
         * weighting methods.
         */

        double totalEnergy = 0;

        for (int i = 0; i < fogBase.length; i++) {
            totalEnergy += fogBase[i].energy;
        }

        for (int i = 0; i < iotBase.length; i++) {
            totalEnergy += iotBase[i].energy;
        }

        double totalLatency = 0;

        for (int i = 0; i < iotBase.length; i++) {
            totalLatency += iotBase[i].latency;
        }

        // Calculate metrics for successfully matched tasks.
        for (int i = 0; i < iot.length; i++) {

            Integer fogId = matching.get(iot[i].Id);

            if (fogId != null && fogId != -1) {

                res.matchedTasks++;

                res.matchedEnergy += iot[i].energy;
                res.matchedLatency += iot[i].latency;

                fogNode assignedFog = fogById.get(fogId);

                if (assignedFog != null
                        && iot[i].latency <= assignedFog.deadline) {

                    res.deadlineMet++;
                }
            }
        }

        res.totalEnergy = totalEnergy;
        res.totalLatency = totalLatency;
        res.totalTasks = iot.length;

        System.out.println("\nPerformance results:");

        System.out.println(
            "Number of IoT tasks: " + res.totalTasks
        );

        System.out.println(
            "Total estimated system energy: " + totalEnergy
        );

        System.out.println(
            "Total estimated task latency: " + totalLatency
        );

        System.out.println(
            "Successfully matched tasks: " + res.matchedTasks
        );

        System.out.println(
            "Number of outages: " + res.outages
        );

        System.out.println(
            "Tasks meeting assigned fog deadline: "
            + res.deadlineMet
        );

        return res;
    }

    // ---------------------------------------------------------
    // COMPARISON TABLE
    // ---------------------------------------------------------

    public static void row(
            String label,
            ArrayList<String> values) {

        StringBuilder sb =
            new StringBuilder(String.format("%-42s", label));

        for (int i = 0; i < values.size(); i++) {

            sb.append(
                String.format("%-27s", values.get(i))
            );
        }

        System.out.println(sb);
    }

    public static void printComparison(
            ArrayList<ModeResult> results) {

        int len = 42 + 27 * results.size();

        StringBuilder line = new StringBuilder();

        for (int i = 0; i < len; i++) {
            line.append("-");
        }

        System.out.println("\n\n" + line);

        System.out.println(
            "COMPARISON: EQUAL vs AHP vs ENTROPY + TOPSIS"
        );

        System.out.println(line);

        ArrayList<String> values = new ArrayList<String>();

        for (ModeResult x : results) {
            values.add(x.mode + " + TOPSIS");
        }

        row("Weighting method", values);

        System.out.println(line);

        values = new ArrayList<String>();

        for (ModeResult x : results) {
            values.add(x.fogWeights);
        }

        row("Fog weights [energy, deadline]", values);

        values = new ArrayList<String>();

        for (ModeResult x : results) {
            values.add(x.iotWeights);
        }

        row("Task weights [energy, demand, latency]", values);

        values = new ArrayList<String>();

        for (ModeResult x : results) {
            values.add("Fog node " + x.fogOrder[0]);
        }

        row("Highest-ranked fog node", values);

        values = new ArrayList<String>();

        for (ModeResult x : results) {
            values.add(orderString(x.fogOrder, 10));
        }

        row("Fog ranking", values);

        values = new ArrayList<String>();

        for (ModeResult x : results) {
            values.add("Task " + x.iotOrder[0]);
        }

        row("Highest-ranked IoT task", values);

        values = new ArrayList<String>();

        for (ModeResult x : results) {
            values.add(orderString(x.iotOrder, 8));
        }

        row("IoT ranking (first 8)", values);

        values = new ArrayList<String>();

        for (ModeResult x : results) {

            boolean same =
                Arrays.equals(
                    x.iotOrder,
                    results.get(0).iotOrder
                )
                &&
                Arrays.equals(
                    x.fogOrder,
                    results.get(0).fogOrder
                );

            values.add(same ? "Same" : "Different");
        }

        row("Ranking compared with first method", values);

        System.out.println(line);

        values = new ArrayList<String>();

        for (ModeResult x : results) {
            values.add(
                String.format(Locale.US, "%.4f", x.totalEnergy)
            );
        }

        row("Baseline estimated system energy", values);

        values = new ArrayList<String>();

        for (ModeResult x : results) {
            values.add(
                String.format(Locale.US, "%.4f", x.totalLatency)
            );
        }

        row("Baseline estimated task latency", values);

        values = new ArrayList<String>();

        for (ModeResult x : results) {
            values.add(
                String.format(Locale.US, "%.4f", x.matchedEnergy)
            );
        }

        row("Energy estimate of matched tasks", values);

        values = new ArrayList<String>();

        for (ModeResult x : results) {
            values.add(
                String.format(Locale.US, "%.4f", x.matchedLatency)
            );
        }

        row("Latency estimate of matched tasks", values);

        values = new ArrayList<String>();

        for (ModeResult x : results) {
            values.add(String.valueOf(x.matchedTasks));
        }

        row("Successfully matched tasks", values);

        values = new ArrayList<String>();

        for (ModeResult x : results) {
            values.add(String.valueOf(x.outages));
        }

        row("Outages", values);

        values = new ArrayList<String>();

        for (ModeResult x : results) {
            values.add(
                x.deadlineMet + " of " + x.matchedTasks
            );
        }

        row("Tasks meeting assigned fog deadline", values);

        values = new ArrayList<String>();

        for (ModeResult x : results) {

            double completionRate =
                x.totalTasks > 0
                ? 100.0 * x.matchedTasks / x.totalTasks
                : 0;

            values.add(
                String.format(
                    Locale.US, "%.2f%%", completionRate
                )
            );
        }

        row("Task assignment rate", values);

        values = new ArrayList<String>();

        for (ModeResult x : results) {

            double deadlineRate =
                x.matchedTasks > 0
                ? 100.0 * x.deadlineMet / x.matchedTasks
                : 0;

            values.add(
                String.format(
                    Locale.US, "%.2f%%", deadlineRate
                )
            );
        }

        row("Deadline satisfaction rate (matched tasks)", values);

        System.out.println(line);
    }

    // ---------------------------------------------------------
    // MAIN METHOD
    // ---------------------------------------------------------

    public static void main(String[] args) {

        Scanner sc = SC;

        try {

            System.out.println(
                "\n=============================================="
            );

            System.out.println(
                "IOT - FOG COMPUTING TASK OFFLOADING"
            );

            System.out.println(
                "TOPSIS WITH EQUAL, AHP AND ENTROPY WEIGHTS"
            );

            System.out.println(
                "==============================================\n"
            );

            // -------------------------------------------------
            // SYSTEM INPUTS
            // -------------------------------------------------

            System.out.println(
                "Enter computational demand of the task "
                + "in million cycles:"
            );

            double computational_demand = sc.nextDouble();

            System.out.println(
                "Enter IoT device frequency in MHz:"
            );

            double frequency = sc.nextDouble();

            System.out.println(
                "Enter channel bandwidth in MHz:"
            );

            double bandwidth = sc.nextDouble();

            System.out.println(
                "Enter distance from IoT device to fog node in meters:"
            );

            int distance_to_fog_node = sc.nextInt();

            System.out.println(
                "Enter power of IoT device in Watts:"
            );

            double power_of_iot_device = sc.nextDouble();

            System.out.println(
                "Enter power of fog node in Watts:"
            );

            double power_of_fogDevice = sc.nextDouble();

            System.out.println(
                "Enter output size in Kb:"
            );

            double output_size = sc.nextDouble();

            System.out.println(
                "Enter computational power of fog node:"
            );

            double computational_power_of_fognode =
                sc.nextDouble();

            System.out.println("Enter value of X:");
            double X = sc.nextDouble();

            System.out.println("Enter value of S:");
            double S = sc.nextDouble();

            System.out.println("Enter value of N:");
            double N = sc.nextDouble();

            if (computational_demand <= 0
                    || frequency <= 0
                    || bandwidth <= 0
                    || distance_to_fog_node <= 0
                    || power_of_iot_device <= 0
                    || power_of_fogDevice <= 0
                    || output_size < 0
                    || computational_power_of_fognode < 0) {

                throw new IllegalArgumentException(
                    "Please enter valid positive system parameters."
                );
            }

            // -------------------------------------------------
            // LOCAL EXECUTION TIME
            // -------------------------------------------------

            double local_execution_time =
                computational_demand / frequency;

            System.out.println(
                "\nLocal execution time: "
                + local_execution_time
            );

            // -------------------------------------------------
            // CHANNEL GAIN AND DATA RATE
            // -------------------------------------------------

            double log10distance_to_fog_node =
                log10(distance_to_fog_node);

            double PLIJ =
                38.02 + 20 * log10distance_to_fog_node;

            double power_to_calculate =
                -(PLIJ / 10);

            double channel_gain =
                Math.pow(10, power_to_calculate);

            double noise_power = Math.pow(10, -10);

            double in_the_log =
                1 +
                (power_of_iot_device * channel_gain)
                / noise_power;

            double log2in_the_log = log2(in_the_log);

            double uplink_data_rate =
                bandwidth * log2in_the_log;

            System.out.println(
                "\nUplink data rate: "
                + uplink_data_rate + " Mbps"
            );

            double downlink_data_rate =
                bandwidth *
                log2(
                    1 +
                    (power_of_fogDevice * channel_gain)
                    / noise_power
                );

            System.out.println(
                "Downlink data rate: "
                + downlink_data_rate + " Mbps"
            );

            // -------------------------------------------------
            // IOT ENERGY
            // -------------------------------------------------

            double Energy_of_IOT_device =
                power_of_iot_device * local_execution_time;

            System.out.println(
                "Energy of IoT device: "
                + Energy_of_IOT_device + " Joules\n"
            );

            // -------------------------------------------------
            // FOG COMPUTATIONAL CAPACITY
            // -------------------------------------------------

            double fog_node_capacity =
                6 * Math.pow(10, 9);

            double temp_quota = 50.0;

            double capacity_of_VRU =
                fog_node_capacity / temp_quota;

            double computational_delay =
                computational_demand / capacity_of_VRU;

            double uplink_data_rate_time =
                output_size / (uplink_data_rate * 1000);

            double downlink_data_rate_time =
                output_size / (downlink_data_rate * 1000);

            // -------------------------------------------------
            // READ FOG NODES
            // -------------------------------------------------

            fogNode[] fogBase =
                readFogNodes(
                    downlink_data_rate,
                    uplink_data_rate,
                    output_size,
                    capacity_of_VRU
                );

            // -------------------------------------------------
            // READ IOT TASKS
            // -------------------------------------------------

            iotDevice[] iotBase =
                readIotTasks(
                    local_execution_time,
                    uplink_data_rate_time,
                    downlink_data_rate_time,
                    power_of_iot_device,
                    power_of_iot_device,
                    computational_power_of_fognode,
                    computational_delay,
                    X, S, N,
                    power_of_fogDevice,
                    downlink_data_rate,
                    uplink_data_rate,
                    output_size,
                    capacity_of_VRU
                );

            // -------------------------------------------------
            // AHP WEIGHTS
            // -------------------------------------------------

            System.out.println(
                "\n\nCalculating AHP criteria weights..."
            );

            criteria_Weight cw = AHP();

            /*
             * Fog order:
             * Energy, Deadline
             */

            double[] fogAhp = {
                cw.CW_of_Energy_of_fog,
                cw.CW_of_deadline_of_fog
            };

            /*
             * AHP IoT matrix order:
             * Energy, Latency, Computational demand
             *
             * TOPSIS IoT decision matrix order:
             * Energy, Computational demand, Latency
             *
             * Therefore the weights are reordered here.
             */

            double[] iotAhp = {
                cw.CW_of_Energy_of_iot,
                cw.CW_of_Computational_demand_of_iot,
                cw.CW_of_Latency_of_iot
            };

            System.out.println(
                "\nAHP fog weights: " + fmt(fogAhp)
            );

            System.out.println(
                "AHP IoT weights: " + fmt(iotAhp)
            );

            // -------------------------------------------------
            // SELECT EXECUTION MODE
            // -------------------------------------------------

            String[] modes = MODES;

            if (args.length > 0) {

                String selectedMode =
                    args[0].toUpperCase(Locale.ROOT);

                if (selectedMode.equals("EQUAL")
                        || selectedMode.equals("AHP")
                        || selectedMode.equals("ENTROPY")) {

                    modes = new String[] { selectedMode };

                } else {

                    System.out.println(
                        "\nUnknown method: " + args[0]
                    );

                    System.out.println(
                        "Running all three methods instead."
                    );
                }
            }

            // -------------------------------------------------
            // RUN COMPARISON
            // -------------------------------------------------

            ArrayList<ModeResult> results =
                new ArrayList<ModeResult>();

            for (int k = 0; k < modes.length; k++) {

                results.add(
                    runMode(
                        modes[k],
                        fogBase,
                        iotBase,
                        fogAhp,
                        iotAhp
                    )
                );
            }

            // -------------------------------------------------
            // FINAL COMPARISON
            // -------------------------------------------------

            printComparison(results);

            System.out.println(
                "\nExecution completed successfully."
            );

        } catch (InputMismatchException e) {

            System.out.println(
                "\nERROR: Invalid input. Please enter numeric values."
            );

        } catch (IllegalArgumentException e) {

            System.out.println(
                "\nERROR: " + e.getMessage()
            );

        } catch (Exception e) {

            System.out.println(
                "\nUnexpected error: " + e.getMessage()
            );

            e.printStackTrace();

        } finally {

            // Do not close the shared Scanner here.
        }
    }
}
