package server;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Reduce {
    private double getDistance(double[] a, double[] b) {
        double diffSquareSum = 0;
        for (int i = 0; i < a.length; i++) {
            diffSquareSum += (a[i] - b[i]) * (a[i] - b[i]);
        }
        return Math.sqrt(diffSquareSum);
    }

    public List<double[]> findClosestVectors(double[] targetVector, double[][][] threeDArray, int n) {
        List<double[]> flattenedList = flattenThreeDArray(threeDArray);

        flattenedList.sort(Comparator.comparingDouble(v -> getDistance(targetVector, v)));

        return flattenedList.subList(0, Math.min(n, flattenedList.size()));
    }

    private List<double[]> flattenThreeDArray(double[][][] threeDArray) {
        List<double[]> flattenedList = new ArrayList<>();

        for (double[][] matrix : threeDArray) {
            for (double[] vector : matrix) {
                flattenedList.add(vector);
            }
        }

        return flattenedList;
    }

    public static void main(String[] args) {
        Reduce Reduce = new Reduce();

        double[] targetVector = {1.0, 2.0, 3.0};
        double[][][] threeDArray = {
                {{1.0, 2.0, 3.0}, {4.0, 5.0, 6.0}},
                {{7.0, 8.0, 9.0}, {10.0, 11.0, 12.0}},
                {{2.0, 3.0, 4.0}, {5.0, 6.0, 7.0}},
                {{8.0, 9.0, 10.0}, {11.0, 12.0, 13.0}},
                {{3.0, 4.0, 5.0}, {6.0, 7.0, 8.0}},
                {{9.0, 10.0, 11.0}, {12.0, 13.0, 14.0}},
                {{4.0, 5.0, 6.0}, {7.0, 8.0, 9.0}},
                {{10.0, 11.0, 12.0}, {13.0, 14.0, 15.0}},
                {{5.0, 6.0, 7.0}, {8.0, 9.0, 10.0}},
                {{11.0, 12.0, 13.0}, {14.0, 15.0, 16.0}}
        };

        int n = 2;

        List<double[]> closestVectors = Reduce.findClosestVectors(targetVector, threeDArray, n);

        System.out.println("Closest Vectors:");
        for (double[] vector : closestVectors) {
            for (double value : vector) {
                System.out.print(value + " ");
            }
            System.out.println();
        }
    }
}

