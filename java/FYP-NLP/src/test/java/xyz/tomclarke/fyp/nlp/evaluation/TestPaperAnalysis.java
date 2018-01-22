package xyz.tomclarke.fyp.nlp.evaluation;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;

import xyz.tomclarke.fyp.nlp.TestOnPapers;
import xyz.tomclarke.fyp.nlp.paper.Paper;
import xyz.tomclarke.fyp.nlp.util.NlpUtil;

/**
 * Gets some information about papers
 * 
 * @author tbc452
 *
 */
public class TestPaperAnalysis extends TestOnPapers {

    @Ignore
    @Test
    public void printPaperAnalysis() {
        for (Paper paper : trainingPapers) {
            PaperAnalysis analysis = new PaperAnalysis(paper);
            analysis.calculate();
            System.out.println(analysis);
        }
    }

    @Test
    public void printTokenAnalysis() throws IOException {
        Map<String, Double> tfIdfs = new HashMap<String, Double>();
        for (Paper paper : trainingPapers) {
            for (String token : paper.getTokenCounts().keySet()) {
                tfIdfs.put(token, NlpUtil.calculateTfIdf(token, paper, trainingPapers));
            }
        }

        FileWriter fileWriter = new FileWriter("tfidfs.csv");
        fileWriter.append("token|tfidf");
        fileWriter.append(System.lineSeparator());

        for (String token : tfIdfs.keySet()) {
            fileWriter.append(token);

            fileWriter.append("|");

            fileWriter.append(tfIdfs.get(token).toString());
            fileWriter.append(System.lineSeparator());
        }

        fileWriter.close();
    }

}
