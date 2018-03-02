package xyz.tomclarke.fyp.gui.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import xyz.tomclarke.fyp.gui.dao.HyponymRepository;
import xyz.tomclarke.fyp.gui.dao.KeyPhraseDAO;
import xyz.tomclarke.fyp.gui.dao.KeyPhraseRepository;
import xyz.tomclarke.fyp.gui.dao.PaperDAO;
import xyz.tomclarke.fyp.gui.dao.PaperRepository;
import xyz.tomclarke.fyp.gui.dao.SynLinkRepository;
import xyz.tomclarke.fyp.gui.model.SearchQuery;
import xyz.tomclarke.fyp.gui.model.SearchResult;
import xyz.tomclarke.fyp.nlp.paper.Paper;
import xyz.tomclarke.fyp.nlp.paper.PseudoPaper;
import xyz.tomclarke.fyp.nlp.util.NlpUtil;

/**
 * Performs searches to find papers
 * 
 * @author tbc452
 *
 */
@Service
public class PaperSearch {

    private static final int MAX_RESULTS_PER_PAGE = 15;
    private static final int SNIPPET_MAX_SIZE = 80;
    private static final int SNIPPET_EXTENSION = 50;

    @Autowired
    private PaperProcessor pp;
    @Autowired
    private PaperRepository paperRepo;
    @Autowired
    private KeyPhraseRepository kpRepo;
    @Autowired
    private HyponymRepository hypRepo;
    @Autowired
    private SynLinkRepository synLinkRepo;

    /**
     * Interpret a search query to find relevant papers
     * 
     * @param search
     *            The search information, supplied by the user
     * @return A list of relevant papers
     */
    public List<SearchResult> search(SearchQuery search) {
        // No search result, just do all
        if (search.getText() == null || search.getText().isEmpty()) {
            return buildResultList(paperRepo.findAll(), false, null);
        }

        // Get TD-IDF of the query
        String[] queryTokens = NlpUtil.getAllTokens(search.getText());
        Map<String, Double> queryValues = new HashMap<String, Double>();
        // Need a pseudo paper
        Paper queryPaper = new PseudoPaper(search.getText());
        for (String query : queryTokens) {
            queryValues.put(query, NlpUtil.calculateTfIdf(query, queryPaper, pp.getTrainingPapers()));
        }

        return buildResultList(searchByTokens(search, queryValues), true, search);
    }

    /**
     * Performs a more naive search for papers based on token occurrence
     * 
     * @param search
     *            The search information, supplied by the user
     * @param queryValues
     *            The query with TF-IDF values calculated
     * @return A list of papers in order to display to the user
     */
    private List<PaperDAO> searchByTokens(SearchQuery search, Map<String, Double> queryValues) {
        String regex = "";
        for (String key : queryValues.keySet()) {
            regex += key + "|";
        }

        // Remove the last |
        regex = regex.substring(0, regex.length() - 1);

        List<PaperDAO> matchingPapers = paperRepo.findByContentRegex(regex);

        // These papers have at least one of the terms in the query
        // Find their scores...
        Map<PaperDAO, Double> paperScores = new HashMap<PaperDAO, Double>();
        for (PaperDAO paper : matchingPapers) {
            double score = 0.0;
            for (String key : queryValues.keySet()) {
                key = key.toLowerCase();
                if ((paper.getText() != null && paper.getText().toLowerCase().contains(key))
                        || (paper.getTitle() != null && paper.getTitle().toLowerCase().contains(key))
                        || (paper.getAuthor() != null && paper.getAuthor().toLowerCase().contains(key))) {
                    // What if the string is only substring of words - worth considering? yes
                    // Add TF-IDF of token in paper * TF-IDF of query token
                    double tfIdfWeighting = NlpUtil.calculateTfIdf(key, pp.loadPaper(paper), pp.getTrainingPapers())
                            * queryValues.get(key);

                    // Check if in key phrase
                    // TODO convert this so it's one call, not the number of papers initially
                    // returned
                    List<KeyPhraseDAO> kps;
                    if (!search.isFocusOnAny()) {
                        // Don't care what classification
                        kps = kpRepo.findByPaperAndText(paper, key);
                    } else {
                        kps = kpRepo.findByPaperAndTextAndClassification(paper, key, search.getFocusRegex());
                    }

                    double kpWeighting = 1.0;
                    for (KeyPhraseDAO kp : kps) {
                        if (kp.getText().toLowerCase().contains(key)) {
                            // Currently doubling TF-IDF every time the key is used in a kp
                            kpWeighting += 1.0;
                        }
                    }

                    score += kpWeighting * tfIdfWeighting;
                }
            }

            // Add it to the list
            paperScores.put(paper, score);
        }

        // Sort according to their score
        matchingPapers.sort(new Comparator<PaperDAO>() {
            @Override
            public int compare(PaperDAO p1, PaperDAO p2) {
                // This needs to be backwards (i.e. descending order) so the page displays it in
                // the correct order (you don't want to scroll to the bottom for the best
                // result)
                if (paperScores.get(p1) > paperScores.get(p2)) {
                    // Paper 1 is better than paper 2
                    return -1;
                } else if (paperScores.get(p1) < paperScores.get(p2)) {
                    // Paper 1 is worse than paper 2
                    return 1;
                } else {
                    // Equal (unlikely...)
                    return 0;
                }
            }
        });

        return matchingPapers;
    }

    /**
     * Convert a list of found papers into search results
     * 
     * @param papers
     *            The papers to display to the user
     * @param limitResults
     *            Whether or not to limit the returned results
     * @param search
     *            The search information, supplied by the user
     * @return The build results list
     */
    private List<SearchResult> buildResultList(Iterable<PaperDAO> papers, boolean limitResults, SearchQuery search) {
        List<SearchResult> results = new ArrayList<SearchResult>();
        int index = 0;
        for (PaperDAO paper : papers) {
            if (limitResults) {
                if (index < MAX_RESULTS_PER_PAGE) {
                    index++;
                } else {
                    // We have MAX_RESULTS_PER_PAGE, send them back to the client
                    return results;
                }
            }
            SearchResult result = new SearchResult();
            result.setId(paper.getId());
            result.setPaper(paper.getTitle());
            if (paper.getTitle() == null || paper.getTitle().trim().isEmpty()) {
                result.setPaper(paper.getLocation());
            }
            result.setKps(kpRepo.countByPaper(paper));
            result.setRels(hypRepo.countByPaper(paper) + synLinkRepo.countByPaper(paper));

            // Try and extract a snippet
            if (search != null) {
                String[] searchTerms = NlpUtil.getAllTokens(search.getText());
                for (String term : searchTerms) {
                    int tIndex = paper.getText().indexOf(term);
                    if (tIndex > -1) {
                        String snippet = result.getSnippet();
                        // Is the term actually already there?
                        if (snippet != null && snippet.contains(term)) {
                            result.setSnippet(snippet.replaceAll(term, "<b>" + term + "</b>"));
                            continue;
                        }
                        // Ok, add it to the output
                        int sIndexStart = Math.max(0, tIndex - SNIPPET_EXTENSION);
                        sIndexStart = Math.max(0, paper.getText().substring(0, sIndexStart).lastIndexOf(" "));
                        int sIndexEnd = Math.min(paper.getText().length(), tIndex + SNIPPET_EXTENSION);
                        sIndexEnd = Math.max(sIndexEnd, paper.getText().substring(sIndexEnd).indexOf(" "));
                        if (snippet == null) {
                            snippet = "";
                        } else {
                            snippet += ", ";
                        }
                        snippet += paper.getText().substring(sIndexStart, sIndexEnd).replaceAll(term,
                                "<b>" + term + "</b>") + "...";
                        result.setSnippet(snippet);
                    }
                }
            }

            // Make sure there's something to display
            if (result.getSnippet() == null || result.getSnippet().isEmpty()) {
                result.setSnippet(
                        paper.getText().substring(0,
                                Math.max(SNIPPET_MAX_SIZE,
                                        paper.getText().substring(SNIPPET_MAX_SIZE).indexOf(" ") + SNIPPET_MAX_SIZE))
                                + "...");
                // The indexOf + SNIPPET_MAX_SIZE will either be == or larger if the substring
                // is on a space or has a space, or 1 less as it could return -1
            }

            results.add(result);
        }
        return results;
    }

}
