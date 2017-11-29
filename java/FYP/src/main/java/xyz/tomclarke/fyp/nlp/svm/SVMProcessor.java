package xyz.tomclarke.fyp.nlp.svm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.deeplearning4j.models.word2vec.Word2Vec;

import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.util.CoreMap;
import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import libsvm.svm_parameter;
import libsvm.svm_print_interface;
import libsvm.svm_problem;
import xyz.tomclarke.fyp.nlp.paper.Paper;
import xyz.tomclarke.fyp.nlp.paper.extraction.Classification;
import xyz.tomclarke.fyp.nlp.paper.extraction.KeyPhrase;
import xyz.tomclarke.fyp.nlp.util.NlpUtil;

/**
 * Uses libsvm to analyse text
 * 
 * @author tbc452
 *
 */
public class SVMProcessor {

    private static final Logger log = LogManager.getLogger(SVMProcessor.class);

    private static final int numOfSVs = 11;

    private svm_parameter param;
    private svm_model model;
    private svm_problem problem;
    private Word2Vec vec;

    // Used when constructing SVs
    private List<Paper> trainingPapers;
    private int maxWordLength;
    private Classification clazz;

    public SVMProcessor(Word2Vec vec) {
        // Construct the (default) parameter object
        param = new svm_parameter();
        param.svm_type = svm_parameter.C_SVC;
        param.kernel_type = svm_parameter.RBF;
        // 1 / number of features
        param.gamma = 1 / 2.0;
        param.cache_size = 1024;
        param.eps = 0.001;
        param.C = 100.0;
        param.nr_weight = 0;
        param.weight_label = new int[0];
        param.weight = new double[0];
        param.shrinking = 0;
        param.probability = 0;

        // Sort out printing
        class SvmPrinter implements svm_print_interface {
            @Override
            public void print(String message) {
                message = message.trim();
                String formatedMessage = "(libsvm) " + message;
                if (message.isEmpty()) {
                    // An empty message... hmm
                    return;
                } else if (message.length() < 2) {
                    // Message is a character long, so we probably don't care about it
                    log.debug(message);
                } else if (message.contains("WARNING")) {
                    // It's a warning
                    log.warn(formatedMessage);
                } else {
                    // No other special cases seen yet
                    // log everything else
                    log.info(formatedMessage);
                }
            }
        }

        svm.svm_set_print_string_function(new SvmPrinter());

        this.vec = vec;
        maxWordLength = 0;
    }

    /**
     * Generates the data (svm_problem) for use in the SVM
     * 
     * @param papers
     *            The papers that shall be used as training data
     * @param clazz
     *            The classification this SVM is being built for
     */
    public void generateTrainingData(List<Paper> papers, Classification clazz) throws IOException {
        // Save for later use
        trainingPapers = papers;
        this.clazz = clazz;

        // Generate totals of counts first
        Map<String, Integer> totalCounts = new HashMap<String, Integer>();
        for (Paper paper : papers) {
            for (String word : paper.getTokenCounts().keySet()) {
                Integer wordCount = paper.getTokenCounts().get(word);

                // Total word counts
                if (!totalCounts.containsKey(word)) {
                    totalCounts.put(word, wordCount);
                } else {
                    totalCounts.put(word, totalCounts.get(word) + wordCount);
                }

                // Find the longest word
                if (maxWordLength <= word.length()) {
                    maxWordLength = word.length();
                }
            }
        }

        // The problem to return
        problem = new svm_problem();
        // Will have 1 entry for each token/word
        problem.l = totalCounts.values().stream().reduce(0, Integer::sum);
        problem.y = new double[problem.l];
        problem.x = new svm_node[problem.l][numOfSVs];

        // Go through each word.
        int index = 0;
        for (Paper paper : papers) {
            for (CoreMap sentence : paper.getAnnotations()) {
                boolean previousWordKeyPhrase = false;
                for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
                    // Key phrase (label)
                    // TODO what if a token is repeated in another key phrase but is a different
                    // type
                    double keyPhrase = paper.isTokenPartOfKeyPhrase(token, clazz) ? 1.0 : 0.0;

                    svm_node[] nodes = generateSupportVectors(token, paper, previousWordKeyPhrase);

                    log.debug(String.format("%f 1:%.8f 2:%f 3:%.8f 4:%.8f 5:%f 6:%f 7:%f 8:%.8f", keyPhrase,
                            nodes[0].value, nodes[1].value, nodes[2].value, nodes[3].value, nodes[4].value,
                            nodes[5].value, nodes[6].value, nodes[7].value));

                    // Add the information
                    problem.y[index] = keyPhrase;
                    problem.x[index] = nodes;

                    index++;

                    // Save previous word information (for next word in same paper only)
                    previousWordKeyPhrase = keyPhrase > 0.0;
                }
            }
        }

        // // Do cross validation to find the best parameters
        // log.info("C: " + param.C + " gamma: " + param.gamma);
        // double[] target = new double[problem.l];
        // svm.svm_cross_validation(problem, param, 5, target);
        // log.info("C: " + param.C + " gamma: " + param.gamma);
        // int total_correct = 0;
        // for (int i = 0; i < problem.l; i++) {
        // if (target[i] == problem.y[i]) {
        // ++total_correct;
        // }
        // }
        // // Currently 69.67793310918421%
        // log.info("Cross Validation Accuracy = " + 100.0 * total_correct / problem.l +
        // "%\n");
    }

    /**
     * Train the SVM on loaded data
     * 
     * @throws Exception
     *             If the training data isn't suitable for use
     */
    public void train() throws Exception {
        log.info("Training SVM (classification " + clazz + ")");
        String paramCheck = svm.svm_check_parameter(problem, param);
        if (paramCheck == null) {
            // Fine to train on
            model = svm.svm_train(problem, param);
            log.info("Finished training");
        } else {
            // Something is wrong...
            throw new Exception(paramCheck);
        }
    }

    /**
     * Predict if a word (translated to a set of nodes) is a keyword.
     * 
     * @param nodes
     *            The processed token information
     * @return Whether the token is a key phrase
     */
    public boolean predictIsKeyword(svm_node[] nodes) {
        double prediction = svm.svm_predict(model, nodes);
        log.debug("Prediction: " + prediction);
        return prediction > 0.0;
    }

    /**
     * Generates support vectors for a given token for a given paper
     * 
     * @param token
     *            The token to generate SVs for
     * @param paper
     *            The paper (the token is from)
     * @param previousWordKeyPhrase
     *            Whether the previous word was a key phrase
     * @return The array of generated SVs
     */
    public svm_node[] generateSupportVectors(CoreLabel token, Paper paper, boolean previousWordKeyPhrase) {
        String word = token.get(TextAnnotation.class).toLowerCase();

        // Length (1)
        double len = (double) word.length() / (double) maxWordLength;
        svm_node svLen = makeNewNode(1, len);
        // POS (2)
        int pos = token.get(PartOfSpeechAnnotation.class).toLowerCase().contains("n") ? 1 : 0;
        svm_node svPos = makeNewNode(2, pos);
        // TF-IDF (3)
        svm_node svTfIdf = makeNewNode(3, NlpUtil.calculateTfIdf(word, paper, trainingPapers));
        // Depth (4)
        boolean foundWord = false;
        int wordDepth = 0;
        for (CoreMap sentenceD : paper.getAnnotations()) {
            for (CoreLabel tokenD : sentenceD.get(TokensAnnotation.class)) {
                String wordD = tokenD.get(TextAnnotation.class).toLowerCase();
                wordDepth++;
                if (wordD.equals(word)) {
                    foundWord = true;
                    break;
                }
            }
            if (foundWord) {
                break;
            }
        }
        double depth = (double) wordDepth / (double) paper.getTokenCounts().values().stream().reduce(0, Integer::sum);
        svm_node svDepth = makeNewNode(4, depth);
        // In first (5) or last (6) sentence?
        int inFirstSentence = 0;
        int inLastSentence = 0;
        CoreMap sentenceFL = paper.getSentenceWithToken(token);
        // TODO consider if this is right (a key word could be in the middle of the text
        // and at the start/end...)
        if (paper.getAnnotations().get(0) == sentenceFL) {
            inFirstSentence = 1;
        } else if (paper.getAnnotations().get(paper.getAnnotations().size() - 1) == sentenceFL) {
            inLastSentence = 1;
        }
        svm_node svFS = makeNewNode(5, inFirstSentence);
        svm_node svLS = makeNewNode(6, inLastSentence);
        // Was the previous word a key phrase (7)
        svm_node svLWKP = makeNewNode(7, previousWordKeyPhrase ? 1.0 : 0.0);
        // Depth in sentence
        CoreMap sentence = paper.getSentenceWithToken(token);
        svm_node svDepthSentence = makeNewNode(8, (double) sentence.get(TokensAnnotation.class).indexOf(token)
                / (double) sentence.get(TokensAnnotation.class).size());
        // Similarity to task
        svm_node svTask = makeNewNode(9, vec.similarity(word, "task"));
        // Similarity to process
        svm_node svProcess = makeNewNode(10, vec.similarity(word, "process"));
        // Similarity to material
        svm_node svMaterial = makeNewNode(11, vec.similarity(word, "material"));

        // Build the SVs for the given token
        svm_node[] nodes = new svm_node[numOfSVs];
        nodes[0] = svLen;
        nodes[1] = svPos;
        nodes[2] = svTfIdf;
        nodes[3] = svDepth;
        nodes[4] = svFS;
        nodes[5] = svLS;
        nodes[6] = svLWKP;
        nodes[7] = svDepthSentence;
        nodes[8] = svTask;
        nodes[9] = svProcess;
        nodes[10] = svMaterial;
        return nodes;
    }

    /**
     * Creates a new SVM node
     * 
     * @param index
     * @param value
     * @return
     */
    private svm_node makeNewNode(int index, double value) {
        svm_node node = new svm_node();
        node.index = index;
        node.value = value;
        if (!Double.isFinite(value)) {
            node.value = 0.0;
        }
        return node;
    }

    /**
     * Gets the model (mainly for testing use)
     * 
     * @return The currently generated model.
     */
    public svm_problem getProblem() {
        return problem;
    }

    /**
     * Predicts key phrases for a paper
     * 
     * @param paper
     *            The paper to predict papers for
     * @return The key phrases found
     */
    public List<KeyPhrase> predictKeyPhrases(Paper paper) {
        List<KeyPhrase> phrases = new ArrayList<KeyPhrase>();

        String text = paper.getText();
        // Position in text
        int counter = 0;
        // Position of word in next block of text
        int pos = 0;

        // For when building a new KP
        int kpStart = 0;
        int kpEnd = 0;
        boolean previousWordKP = false;

        // So key phrases can be created, ensure to keep a count as we go
        for (CoreMap sentence : paper.getAnnotations()) {
            for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
                String word = token.get(TextAnnotation.class);

                svm_node[] nodes = generateSupportVectors(token, paper, previousWordKP);
                boolean isPredictedKeyPhrase = predictIsKeyword(nodes);

                // Get the position
                pos = text.toLowerCase().indexOf(word.toLowerCase());
                if (pos > -1) {
                    counter += pos;
                    text = text.substring(pos);

                    // If is a key phrase and previous is not, make sure they next loop knows this
                    // is a key phrase
                    if (isPredictedKeyPhrase && !previousWordKP) {
                        previousWordKP = true;
                        kpStart = counter;
                    }

                    // Is not a key phrase, but it has just finished a key phrase area, save this.
                    // TODO what if random characters, then a no key word - this'll include the
                    // random characters
                    if (!isPredictedKeyPhrase && previousWordKP) {
                        previousWordKP = false;
                        kpEnd = counter;

                        try {
                            // Add key phrase
                            // Classification is either the one the SVm was trained with, or UNKNOWN
                            phrases.add(paper.makeKeyPhrase(kpStart, kpEnd,
                                    clazz == null ? Classification.UNKNOWN : clazz));
                        } catch (Exception e) {
                            // Making a new key phrase went wrong somehow...
                            log.error(e.getMessage());
                        }
                    }
                }
            }
        }

        return phrases;
    }

}
