package xyz.tomclarke.fyp.nlp.svm;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.junit.Ignore;
import org.junit.Test;

import xyz.tomclarke.fyp.nlp.TestOnPapers;
import xyz.tomclarke.fyp.nlp.evaluation.ConfusionStatistic;
import xyz.tomclarke.fyp.nlp.evaluation.EvaluateExtractions;
import xyz.tomclarke.fyp.nlp.paper.extraction.RelationType;
import xyz.tomclarke.fyp.nlp.paper.extraction.Relationship;
import xyz.tomclarke.fyp.nlp.word2vec.Word2VecPretrained;
import xyz.tomclarke.fyp.nlp.word2vec.Word2VecProcessor;

/**
 * Tests the relation extraction SVM which uses Word2Vec
 * 
 * @author tbc452
 *
 */
public class TestRelationshipSVM2 extends TestOnPapers {

    private static final Logger log = LogManager.getLogger(TestRelationshipSVM2.class);

    @Ignore
    @Test
    public void testRelationSvmGN() throws Exception {
        testRelationSvm(Word2VecPretrained.GOOGLE_NEWS);
    }

    @Ignore
    @Test
    public void testRelationSvmW2V() throws Exception {
        // Wiki2Vec doesn't work right now
        testRelationSvm(Word2VecPretrained.WIKI2VEC);
    }

    @Ignore
    @Test
    public void testRelationSvmFI() throws Exception {
        testRelationSvm(Word2VecPretrained.FREEBASE_IDS);
    }

    @Test
    public void testRelationSvmFN() throws Exception {
        testRelationSvm(Word2VecPretrained.FREEBASE_NAMES);
    }

    /**
     * Test an SVM with given config
     * 
     * @param set
     *            The W2V set to load
     * @throws Exception
     */
    private void testRelationSvm(Word2VecPretrained set) throws Exception {
        Word2Vec vec = Word2VecProcessor.loadPreTrainedData(set);
        // Get all relationships and evaluate
        List<List<Relationship>> allRels = new ArrayList<List<Relationship>>();
        log.info("RUNNING HYP SVM...");
        // Do hyp
        allRels.addAll(trainAndUseSvm(RelationType.HYPONYM_OF, vec));
        System.gc();
        log.info("RUNNING SYN SVM...");
        // Do syn
        allRels.addAll(trainAndUseSvm(RelationType.SYNONYM_OF, vec));

        log.info("CALCULATING...");
        // Evaluate
        List<ConfusionStatistic> overallStats = new ArrayList<ConfusionStatistic>();
        for (int i = 0; i < testPapers.size(); i++) {
            overallStats.add(EvaluateExtractions.evaluateRelationships(allRels.get(i),
                    testPapers.get(i).getRelationships(), testPapers.get(i).getKeyPhrases()));
        }

        ConfusionStatistic stats = ConfusionStatistic.calculateScoreSum(overallStats);

        log.info("Overall statistics: " + stats);
        log.info("Specific results were: tp: " + stats.getTp() + " fp: " + stats.getFp() + " tn: " + stats.getTn()
                + " fn: " + stats.getFn());
    }

    /**
     * Loads resources and runs an SVM
     * 
     * @param type
     *            The type of SVM
     * @param vec
     *            The Word2Vec data set to use
     * @return Calculated relationships (for all test papers, in same order as test
     *         papers)
     * @throws Exception
     */
    private List<List<Relationship>> trainAndUseSvm(RelationType type, Word2Vec vec) throws Exception {
        List<List<Relationship>> allRels = new ArrayList<List<Relationship>>();
        RelationshipSVM2 svm = new RelationshipSVM2();

        // Generate the SVM training data
        svm.generateTrainingData(trainingPapers, type, vec);

        // Train the SVM
        svm.train();

        // Use the pre-calculated test SVs to get relations
        for (int i = 0; i < testPapers.size(); i++) {
            allRels.add(svm.predictRelationships(testPapers.get(i), vec));
        }

        return allRels;
    }

}
