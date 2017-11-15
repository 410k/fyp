package xyz.tomclarke.fyp.nlp.evaluation;

/**
 * A class to hold information about F scoring (and accuracy)
 * 
 * @author tbc452
 *
 */
public class ConfusionStatistics {

    private final double tp;
    private final double fp;
    private final double tn;
    private final double fn;
    private final double accuracy;
    private final double precision;
    private final double recall;
    private final double fone;

    public ConfusionStatistics(double tp, double fp, double tn, double fn, double accuracy, double precision,
            double recall, double fone) {
        this.tp = tp;
        this.fp = fp;
        this.tn = tn;
        this.fn = fn;
        this.accuracy = accuracy;
        this.precision = precision;
        this.recall = recall;
        this.fone = fone;
    }

    public double getTp() {
        return tp;
    }

    public double getFp() {
        return fp;
    }

    public double getTn() {
        return tn;
    }

    public double getFn() {
        return fn;
    }

    public double getAccuracy() {
        return accuracy;
    }

    public double getPrecision() {
        return precision;
    }

    public double getRecall() {
        return recall;
    }

    public double getFone() {
        return fone;
    }

    @Override
    public String toString() {
        return String.format("Accuracy: %.8f Precision: %.8f Recall: %.8f F1: %.8f", accuracy, precision, recall, fone);
    }

    /**
     * Utility to construct confusion statistics information based off of a
     * confusion matrix
     * 
     * @param tp
     *            True positives
     * @param fp
     *            False positives
     * @param tn
     *            True negatives
     * @param fn
     *            False negatives
     * @return The calculated confusion statistics
     */
    public static ConfusionStatistics calculateScore(double tp, double fp, double tn, double fn) {
        double accuracy = (tp + tn) / (tp + fp + fn + tn);
        double precision = tp / (tp + fp);
        double recall = tp / (tp + fn);
        double fone = 2.0 * precision * recall / (precision + recall);

        return new ConfusionStatistics(tp, fp, tn, fn, accuracy, precision, recall, fone);
    }

}
