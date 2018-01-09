package xyz.tomclarke.fyp.nlp.svm;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.junit.Ignore;
import org.junit.Test;

import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.util.CoreMap;
import libsvm.svm_node;
import libsvm.svm_problem;
import xyz.tomclarke.fyp.nlp.TestOnPapers;
import xyz.tomclarke.fyp.nlp.evaluation.ConfusionStatistic;
import xyz.tomclarke.fyp.nlp.evaluation.EvaluateExtractions;
import xyz.tomclarke.fyp.nlp.evaluation.Strictness;
import xyz.tomclarke.fyp.nlp.paper.Paper;
import xyz.tomclarke.fyp.nlp.paper.extraction.Classification;
import xyz.tomclarke.fyp.nlp.paper.extraction.KeyPhrase;
import xyz.tomclarke.fyp.nlp.word2vec.Word2VecPretrained;
import xyz.tomclarke.fyp.nlp.word2vec.Word2VecProcessor;

/**
 * Test to evaluate the SVM Processor
 * 
 * @author tbc452
 *
 */
public class TestKeyPhraseSVM extends TestOnPapers {

    private static final Logger log = LogManager.getLogger(TestKeyPhraseSVM.class);

    private static KeyPhraseSVM svmGeneral;
    private static Word2Vec vec;

    /**
     * Builds the general purpose key phrase extractor SVM
     * 
     * @throws Exception
     */
    private static void loadGeneralTools() throws Exception {
        if (svmGeneral == null) {
            // Load Word2Vec
            vec = Word2VecProcessor.loadPreTrainedData(Word2VecPretrained.GOOGLE_NEWS);

            // Setup the SVM
            log.info("Building general key phrase SVM...");
            svmGeneral = new KeyPhraseSVM();
            svmGeneral.generateTrainingData(trainingPapers, null, vec);
            svmGeneral.train();

            log.info("SVM trained, now to test...");
        }
    }

    @Ignore
    @Test
    public void testSvmProcessorSameData() throws Exception {
        loadGeneralTools();
        log.info("Testing with training data");
        // Test on input data...
        double tp = 0;
        double fp = 0;
        double tn = 0;
        double fn = 0;
        svm_problem problem = svmGeneral.getProblem();
        for (int i = 0; i < problem.l; i++) {
            boolean isPredictedKeyPhrase = svmGeneral.predict(problem.x[i]);
            if (problem.y[i] == 0.0 && isPredictedKeyPhrase) {
                fp++;
            } else if (problem.y[i] == 0.0 && !isPredictedKeyPhrase) {
                tn++;
            } else if (problem.y[i] == 1.0 && isPredictedKeyPhrase) {
                tp++;
            } else if (problem.y[i] == 1.0 && !isPredictedKeyPhrase) {
                fn++;
            } else {
                throw new Exception("Problem label not understood: " + problem.y[i]);
            }
        }

        log.info(String.format("tp: %.8f fp: %.8f tn: %.8f fn: %.8f", tp, fp, tn, fn));
        log.info(ConfusionStatistic.calculateScore(tp, fp, tn, fn));
    }

    @Ignore
    @Test
    public void testSvmProcessorTestData() throws Exception {
        loadGeneralTools();
        log.info("Testing with test data");
        double tp = 0;
        double fp = 0;
        double tn = 0;
        double fn = 0;
        for (Paper paper : testPapers) {
            boolean previousWordKeyPhrase = false;
            for (CoreMap sentence : paper.getAnnotations()) {
                for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
                    // Key phrase (answer)
                    double keyPhrase = paper.isTokenPartOfKeyPhrase(token) ? 1.0 : 0.0;

                    // SV Nodes (question)
                    svm_node[] nodes = svmGeneral.generateSupportVectors(token, paper, previousWordKeyPhrase, vec);

                    // Ask the question and compare the answer to the expected answer
                    boolean isPredictedKeyPhrase = svmGeneral.predict(nodes);
                    if (keyPhrase == 0.0 && isPredictedKeyPhrase) {
                        fp++;
                    } else if (keyPhrase == 0.0 && !isPredictedKeyPhrase) {
                        tn++;
                    } else if (keyPhrase == 1.0 && isPredictedKeyPhrase) {
                        tp++;
                    } else if (keyPhrase == 1.0 && !isPredictedKeyPhrase) {
                        fn++;
                    }

                    // Save previous word information (for next word in same paper only)
                    previousWordKeyPhrase = isPredictedKeyPhrase;
                }
            }
        }

        log.info(String.format("tp: %.8f fp: %.8f tn: %.8f fn: %.8f", tp, fp, tn, fn));
        log.info(ConfusionStatistic.calculateScore(tp, fp, tn, fn));
    }

    @Test
    public void testSvmPredictKeyPhrasesGN() throws Exception {
        testSvmPredictKeyPhrases(Word2VecPretrained.GOOGLE_NEWS);
    }

    @Ignore
    @Test
    public void testSvmPredictKeyPhrasesW2V() throws Exception {
        // Wiki2Vec doesn't seem to work right now
        testSvmPredictKeyPhrases(Word2VecPretrained.WIKI2VEC);
    }

    @Ignore
    @Test
    public void testSvmPredictKeyPhrasesFN() throws Exception {
        testSvmPredictKeyPhrases(Word2VecPretrained.FREEBASE_NAMES);
    }

    @Ignore
    @Test
    public void testSvmPredictKeyPhrasesFI() throws Exception {
        testSvmPredictKeyPhrases(Word2VecPretrained.FREEBASE_IDS);
    }

    /**
     * Runs the SVM with a set W2V
     * 
     * @param set
     *            The W2V set to load
     * @throws Exception
     */
    private void testSvmPredictKeyPhrases(Word2VecPretrained set) throws Exception {
        // Load Word2Vec
        Word2Vec vecForSvm = Word2VecProcessor.loadPreTrainedData(set);

        // Setup the SVM
        log.info("Building general key phrase SVM...");
        KeyPhraseSVM svm = new KeyPhraseSVM();
        svm.generateTrainingData(trainingPapers, null, vecForSvm);
        svm.train();

        log.info("SVM trained, now to test...");

        List<ConfusionStatistic> overallStatsGen = new ArrayList<ConfusionStatistic>();
        List<ConfusionStatistic> overallStatsInc = new ArrayList<ConfusionStatistic>();
        List<ConfusionStatistic> overallStatsStr = new ArrayList<ConfusionStatistic>();
        for (Paper paper : testPapers) {
            List<KeyPhrase> phrases = svm.predictKeyPhrases(paper, vecForSvm);

            // Log them
            printKP(paper, phrases);

            // Save statistics
            overallStatsGen.add(EvaluateExtractions.evaluateKeyPhrases(phrases, paper, paper.getKeyPhrases(),
                    Strictness.GENEROUS, false));
            overallStatsInc.add(EvaluateExtractions.evaluateKeyPhrases(phrases, paper, paper.getKeyPhrases(),
                    Strictness.INCLUSIVE, false));
            overallStatsStr.add(EvaluateExtractions.evaluateKeyPhrases(phrases, paper, paper.getKeyPhrases(),
                    Strictness.STRICT, false));
        }

        ConfusionStatistic gen = ConfusionStatistic.calculateScoreSum(overallStatsGen);
        ConfusionStatistic inc = ConfusionStatistic.calculateScoreSum(overallStatsInc);
        ConfusionStatistic str = ConfusionStatistic.calculateScoreSum(overallStatsStr);

        log.info("Overall statistics (gen): " + gen);
        log.debug("Specific results were: tp: " + gen.getTp() + " fp: " + gen.getFp() + " tn: " + gen.getTn() + " fn: "
                + gen.getFn());
        log.info("Overall statistics (inc): " + inc);
        log.debug("Specific results were: tp: " + inc.getTp() + " fp: " + inc.getFp() + " tn: " + inc.getTn() + " fn: "
                + inc.getFn());
        log.info("Overall statistics (str): " + str);
        log.debug("Specific results were: tp: " + str.getTp() + " fp: " + str.getFp() + " tn: " + str.getTn() + " fn: "
                + str.getFn());
    }

    @Ignore
    @Test
    public void testSvmKeyPhraseClassifications() throws Exception {
        if (vec == null) {
            vec = Word2VecProcessor.loadPreTrainedData(Word2VecPretrained.GOOGLE_NEWS);
        }

        log.info("Building task key phrase SVM...");
        KeyPhraseSVM svmTask = new KeyPhraseSVM();
        svmTask.generateTrainingData(trainingPapers, Classification.TASK, vec);
        svmTask.train();
        log.info("Building process key phrase SVM...");
        KeyPhraseSVM svmProcess = new KeyPhraseSVM();
        svmProcess.generateTrainingData(trainingPapers, Classification.PROCESS, vec);
        svmProcess.train();
        log.info("Building material key phrase SVM...");
        KeyPhraseSVM svmMaterial = new KeyPhraseSVM();
        svmMaterial.generateTrainingData(trainingPapers, Classification.MATERIAL, vec);
        svmMaterial.train();
        log.info("SVMs trained, now to test key phrase extraction and classification...");

        List<ConfusionStatistic> overallStatsGen = new ArrayList<ConfusionStatistic>();
        List<ConfusionStatistic> overallStatsInc = new ArrayList<ConfusionStatistic>();
        List<ConfusionStatistic> overallStatsStr = new ArrayList<ConfusionStatistic>();
        for (Paper paper : testPapers) {
            List<KeyPhrase> tasks = svmTask.predictKeyPhrases(paper, vec);
            List<KeyPhrase> processes = svmProcess.predictKeyPhrases(paper, vec);
            List<KeyPhrase> materials = svmMaterial.predictKeyPhrases(paper, vec);
            List<KeyPhrase> phrases = new ArrayList<KeyPhrase>();

            // Log them
            printKP(paper, phrases);

            phrases.addAll(tasks);
            phrases.addAll(processes);
            phrases.addAll(materials);

            // Save statistics
            overallStatsGen.add(EvaluateExtractions.evaluateKeyPhrases(phrases, paper, paper.getKeyPhrases(),
                    Strictness.GENEROUS, true));
            overallStatsInc.add(EvaluateExtractions.evaluateKeyPhrases(phrases, paper, paper.getKeyPhrases(),
                    Strictness.INCLUSIVE, true));
            overallStatsStr.add(EvaluateExtractions.evaluateKeyPhrases(phrases, paper, paper.getKeyPhrases(),
                    Strictness.STRICT, true));
        }

        ConfusionStatistic gen = ConfusionStatistic.calculateScoreSum(overallStatsGen);
        ConfusionStatistic inc = ConfusionStatistic.calculateScoreSum(overallStatsInc);
        ConfusionStatistic str = ConfusionStatistic.calculateScoreSum(overallStatsStr);

        log.info("Overall statistics (gen): " + gen);
        log.debug("Specific results were: tp: " + gen.getTp() + " fp: " + gen.getFp() + " tn: " + gen.getTn() + " fn: "
                + gen.getFn());
        log.info("Overall statistics (inc): " + inc);
        log.debug("Specific results were: tp: " + inc.getTp() + " fp: " + inc.getFp() + " tn: " + inc.getTn() + " fn: "
                + inc.getFn());
        log.info("Overall statistics (str): " + str);
        log.debug("Specific results were: tp: " + str.getTp() + " fp: " + str.getFp() + " tn: " + str.getTn() + " fn: "
                + str.getFn());
    }

    /**
     * Debug log extractions
     * 
     * @param paper
     *            The associated paper
     * @param phrases
     *            The phrases found
     */
    private void printKP(Paper paper, List<KeyPhrase> phrases) {
        log.debug("EXTRACTIONS FOUND");
        log.debug(paper.toString());
        for (KeyPhrase phrase : phrases) {
            log.debug(phrase.toString());
        }

    }

}
