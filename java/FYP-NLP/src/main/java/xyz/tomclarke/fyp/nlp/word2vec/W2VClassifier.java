package xyz.tomclarke.fyp.nlp.word2vec;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.deeplearning4j.models.word2vec.Word2Vec;

import xyz.tomclarke.fyp.nlp.paper.extraction.Classification;
import xyz.tomclarke.fyp.nlp.util.NlpUtil;

/**
 * Looks at different ways Word2Vec can be used to classify key phrases
 * 
 * @author tbc452
 *
 */
public final class W2VClassifier {

    private static final Logger log = LogManager.getLogger(W2VClassifier.class);

    /**
     * Classifies a key phrase based on average distance to a class
     * 
     * @param kp
     *            The key phrase
     * @param vec
     *            The Word2Vec data to use
     * @param autoClazz
     *            The default classification to assign if none can be selected
     * @param removeStopWords
     *            Whether to remove stop words from classification
     * @return The classification decided on
     */
    public static Classification getClazzBasedOnAvgDistance(String kp, Word2Vec vec, Classification autoClazz,
            boolean removeStopWords) {
        String[] tokens = kp.split(" ");
        double distTask = 0;
        double distProc = 0;
        double distMatl = 0;
        int foundWords = 0;

        for (String token : tokens) {
            boolean removeDueToStop = false;
            if (removeStopWords) {
                removeDueToStop = NlpUtil.isTokenToIgnore(token);
            }

            if (vec.hasWord(token) && !removeDueToStop) {
                foundWords++;
                distTask += vec.similarity(token, "task");
                distProc += vec.similarity(token, "process");
                distMatl += vec.similarity(token, "material");
            }
        }

        distTask /= (double) foundWords;
        distProc /= (double) foundWords;
        distMatl /= (double) foundWords;

        if (foundWords == 0) {
            return autoClazz;
        }

        // Largest number means closest
        double max = Math.max(distTask, Math.max(distProc, distMatl));
        if (max == distTask) {
            return Classification.TASK;
        } else if (max == distProc) {
            return Classification.PROCESS;
        } else if (max == distMatl) {
            return Classification.MATERIAL;
        } else {
            // Shouldn't get here
            log.error("Found max which wasn't in valid numbers");
            return autoClazz;
        }
    }

    /**
     * Classifies a key phrase based on the closest tokens different to a class
     * 
     * @param kp
     *            The key phrase
     * @param vec
     *            The Word2Vec data to use
     * @param autoClazz
     *            The default classification to assign if none can be selected
     * @param removeStopWords
     *            Whether to remove stop words from classification
     * @return The classification decided on
     */
    public static Classification getClazzBasedOnClosestDistance(String kp, Word2Vec vec, Classification autoClazz,
            boolean removeStopWords) {
        String[] tokens = kp.split(" ");
        double distTask = 0;
        double distProc = 0;
        double distMatl = 0;
        boolean foundOneWord = false;

        for (String token : tokens) {
            boolean removeDueToStop = false;
            if (removeStopWords) {
                removeDueToStop = !NlpUtil.isTokenToIgnore(token);
            }

            if (vec.hasWord(token) && removeDueToStop) {
                foundOneWord = true;
                // Larger number = closer
                distTask = Math.max(distTask, vec.similarity(token, "task"));
                distProc = Math.max(distProc, vec.similarity(token, "process"));
                distMatl = Math.max(distMatl, vec.similarity(token, "material"));
            }
        }

        // Handle if it can't be classified
        if (!foundOneWord) {
            return autoClazz;
        }

        double max = Math.max(distTask, Math.max(distProc, distMatl));
        if (max == distTask) {
            return Classification.TASK;
        } else if (max == distProc) {
            return Classification.PROCESS;
        } else if (max == distMatl) {
            return Classification.MATERIAL;
        } else {
            // Shouldn't get here
            log.error("Found max which wasn't in valid numbers");
            return autoClazz;
        }
    }

}
