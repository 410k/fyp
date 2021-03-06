package xyz.tomclarke.fyp.nlp.evaluation;

import xyz.tomclarke.fyp.nlp.paper.Paper;
import xyz.tomclarke.fyp.nlp.paper.extraction.Extraction;
import xyz.tomclarke.fyp.nlp.paper.extraction.KeyPhrase;
import xyz.tomclarke.fyp.nlp.util.NlpUtil;

/**
 * Analyse a paper.
 * 
 * @author tbc452
 *
 */
public class PaperAnalysis {

    private static final String SPACE = " ";
    private final Paper paper;
    private int numberOfKeyPhrases;
    private int numberOfRelations;
    private int minKPLength;
    private int maxKPLength;
    private int minKPWords;
    private int maxKPWords;
    private double avgKPLength;
    private double avgKPWords;
    private int words;

    public PaperAnalysis(Paper paper) {
        this.paper = paper;
    }

    @Override
    public String toString() {
        return paper.getLocation() + String.format(", %d, %d, %d, %d, %d, %d, %f, %f, %d", numberOfKeyPhrases,
                numberOfRelations, minKPLength, maxKPLength, minKPWords, maxKPWords, avgKPLength, avgKPWords, words);
    }

    /**
     * Calculates some interesting metrics
     */
    public void calculate() {
        // Reset all
        numberOfKeyPhrases = 0;
        numberOfRelations = 0;
        minKPLength = 0;
        maxKPLength = 0;
        avgKPLength = 0;
        minKPWords = 0;
        maxKPWords = 0;
        avgKPWords = 0;

        for (Extraction ext : paper.getExtractions()) {
            if (ext instanceof KeyPhrase) {
                numberOfKeyPhrases++;
            } else {
                numberOfRelations++;
            }
        }

        // Only continue if there are actually key phrases
        if (numberOfKeyPhrases == 0) {
            return;
        }

        minKPLength = Integer.MAX_VALUE;
        minKPWords = Integer.MAX_VALUE;
        for (KeyPhrase kp : paper.getKeyPhrases()) {
            if (minKPLength > kp.getPhrase().length()) {
                minKPLength = kp.getPhrase().length();
            }
            if (minKPWords > kp.getPhrase().split(SPACE).length) {
                minKPWords = kp.getPhrase().split(SPACE).length;
            }
            if (maxKPLength < kp.getPhrase().length()) {
                maxKPLength = kp.getPhrase().length();
            }
            if (maxKPWords < kp.getPhrase().split(SPACE).length) {
                maxKPWords = kp.getPhrase().split(SPACE).length;
            }

            avgKPLength += kp.getPhrase().length();
            avgKPWords += kp.getPhrase().split(SPACE).length;
        }

        avgKPLength /= numberOfKeyPhrases;
        avgKPWords /= numberOfKeyPhrases;

        words = NlpUtil.getAllTokens(paper.getText()).length;
    }

}
