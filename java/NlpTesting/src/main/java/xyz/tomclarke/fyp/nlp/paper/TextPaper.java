package xyz.tomclarke.fyp.nlp.paper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Supports reading text (.txt) papers
 * 
 * @author tbc452
 *
 */
public class TextPaper extends Paper {

    private static final Logger log = LogManager.getLogger(Paper.class);

    public TextPaper(String textLocation) {
        super(textLocation);
        try {
            setText(new String(Files.readAllBytes(Paths.get(textLocation))));
        } catch (IOException e) {
            log.error("Problem loading text document", e);
        }
    }

}
