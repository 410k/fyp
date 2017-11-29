package xyz.tomclarke.fyp.nlp.evaluation;

import java.util.ArrayList;
import java.util.List;

import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.util.CoreMap;
import xyz.tomclarke.fyp.nlp.paper.Paper;
import xyz.tomclarke.fyp.nlp.paper.extraction.Extraction;
import xyz.tomclarke.fyp.nlp.paper.extraction.KeyPhrase;

/**
 * Used to evaluate extractions
 * 
 * @author tbc452
 *
 */
public abstract class EvaluateExtractions {

    /**
     * Evaluates key phrases (for now we ignore ID and text position)
     * 
     * @param pred
     *            Predicted key phrases
     * @param paper
     *            The paper the key phrases are for
     * @param act
     *            Actual key phrases
     * @param strict
     *            Whether to match key phrases perfectly (actual.equals(predicted))
     *            or if we'll accept close matches
     * @param includeClazz
     *            Whether or not to consider classification of the key phrase
     * @return Statistics based off of a confusion matrix
     */
    public static ConfusionStatistic evaluateKeyPhrases(List<KeyPhrase> pred, Paper paper, List<Extraction> act,
            Strictness strictness, boolean includeClazz) {
        double tp = 0;
        double fp = 0;
        double tn = 0;
        double fn = 0;

        // Actual KPs not in this list are false negatives
        List<KeyPhrase> foundKP = new ArrayList<KeyPhrase>();

        // Check each predicted key phrase...
        for (KeyPhrase predKP : pred) {
            // ... against each actual key phrase
            boolean matched = false;
            for (Extraction actExt : act) {
                if (!(actExt instanceof KeyPhrase)) {
                    // Not a key phrase
                    continue;
                }

                KeyPhrase actKP = (KeyPhrase) actExt;

                boolean matchingPhrase = false;
                boolean matchingClazz = false;

                String predKPWord = predKP.getPhrase().toLowerCase();
                String actKPWord = actKP.getPhrase().toLowerCase();
                // Check key phrase
                switch (strictness) {
                case GENEROUS:
                    matchingPhrase = predKPWord.contains(actKPWord) || actKPWord.contains(predKPWord);
                    break;
                case INCLUSIVE:
                    matchingPhrase = predKPWord.contains(actKPWord);
                    break;
                case STRICT:
                    matchingPhrase = predKPWord.equals(actKPWord);
                    break;
                }

                // Check classification of key phrase
                if (includeClazz) {
                    matchingClazz = predKP.getClazz().equals(actKP.getClazz());
                } else {
                    // As we're not concerned with this
                    matchingClazz = true;
                }

                if (matchingPhrase && matchingClazz) {
                    // Result decided
                    tp++;
                    matched = true;
                    if (!foundKP.contains(actKP)) {
                        foundKP.add(actKP);
                    }
                    continue;
                }
            }

            if (!matched) {
                fp++;
            }
        }

        fn = act.size() - foundKP.size();

        // Get count of true negatives...
        for (CoreMap sentence : paper.getAnnotations()) {
            for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
                String word = token.get(TextAnnotation.class);
                // Don't recount positives
                boolean inPredKP = false;
                for (KeyPhrase phrase : pred) {
                    inPredKP = phrase.getPhrase().contains(word);
                    if (inPredKP) {
                        break;
                    }
                }
                // Don't count false negatives
                boolean inActKP = false;
                for (KeyPhrase phrase : pred) {
                    inActKP = phrase.getPhrase().contains(word);
                    if (inActKP) {
                        break;
                    }
                }

                // If it's not a positive and if it's not a false negative
                if (!inPredKP && !inActKP) {
                    tn++;
                }
            }
        }

        return ConfusionStatistic.calculateScore(tp, fp, tn, fn);
    }
}
