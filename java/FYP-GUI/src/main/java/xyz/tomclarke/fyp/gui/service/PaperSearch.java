package xyz.tomclarke.fyp.gui.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
import xyz.tomclarke.fyp.gui.model.SearchResultAndDetails;
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

    private static final Logger log = LogManager.getLogger(PaperSearch.class);
    private static final int MAX_RESULTS_PER_PAGE = 15;
    private static final int SNIPPET_MAX_SIZE = 80;
    private static final int SNIPPET_EXTENSION = 50;

    @Autowired
    private PaperUtil util;
    @Autowired
    private PaperScoreCalculator scoreCalc;
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
     * @return A list of relevant papers and some details about the search
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public SearchResultAndDetails search(SearchQuery search) throws InterruptedException, ExecutionException {
        SearchResultAndDetails rAndD = new SearchResultAndDetails();
        List<PaperDAO> searchResults;
        List<SearchResult> resultList;
        long start = System.currentTimeMillis();

        // Do searching!
        if (search.getText() == null || search.getText().isEmpty()) {
            // No actual search, just do all
            Iterable<PaperDAO> papers = paperRepo.findAll();
            searchResults = new ArrayList<PaperDAO>();
            papers.forEach(searchResults::add);
            resultList = buildResultList(searchResults, false, null);
        } else {
            // Get TD-IDF of the query
            String[] queryTokens = NlpUtil.getAllTokens(search.getText());
            Map<String, Double> queryValues = new HashMap<String, Double>();
            // Need a pseudo paper
            Paper queryPaper = new PseudoPaper(search.getText());
            for (String query : queryTokens) {
                queryValues.put(query, util.calculateTfIdf(query, queryPaper));
            }

            searchResults = searchByTokens(search, queryValues);
            resultList = buildResultList(searchResults, true, search);
        }

        long end = System.currentTimeMillis();

        // Present the results nicely
        rAndD.setResults(resultList);
        rAndD.setSearchTime(end - start);
        rAndD.setResultsFound(searchResults.size());

        return rAndD;
    }

    /**
     * Performs a more naive search for papers based on token occurrence
     * 
     * @param search
     *            The search information, supplied by the user
     * @param queryValues
     *            The query with TF-IDF values calculated
     * @return A list of papers in order to display to the user
     * @throws ExecutionException
     * @throws InterruptedException
     */
    private List<PaperDAO> searchByTokens(SearchQuery search, Map<String, Double> queryValues)
            throws InterruptedException, ExecutionException {
        String regex = "";
        for (String key : queryValues.keySet()) {
            regex += key + "|";
        }

        // Remove the last |
        regex = regex.substring(0, regex.length() - 1);

        // Get results from the database
        List<PaperDAO> matchingPapers = paperRepo.findByContentRegex(regex);
        List<KeyPhraseDAO> matchingKps;
        if (!search.isFocusOnAny()) {
            // Don't care what classification
            matchingKps = kpRepo.findByPaperAndText(regex);
        } else {
            matchingKps = kpRepo.findByPaperAndTextAndClassification(regex, search.getFocusRegex());
        }
        // Note: doing 1 call doesn't actually really make it much faster - definitely
        // less than 10% (on local that is, if the database was remote it would likely
        // be much better) - although the timings are more consistent

        // These papers have at least one of the terms in the query
        // Find their scores...
        Map<PaperDAO, Double> paperScores = new HashMap<PaperDAO, Double>();
        List<CompletableFuture<Double>> futureScores = new ArrayList<CompletableFuture<Double>>();
        for (PaperDAO paper : matchingPapers) {
            futureScores.add(scoreCalc.calculatePaperScore(paper, search, queryValues, matchingKps));
        }
        CompletableFuture.allOf(futureScores.toArray(new CompletableFuture[futureScores.size()])).join();
        for (int i = 0; i < matchingPapers.size(); i++) {
            paperScores.put(matchingPapers.get(i), futureScores.get(i).get());
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

        log.debug("paper, score");
        for (PaperDAO paper : matchingPapers) {
            log.debug(paper.getId() + ", " + paperScores.get(paper));
        }

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
                    String termReplaceRegex = "";
                    String caseInsensetiveRegex = "(?i)";
                    for (char c : term.toCharArray()) {
                        termReplaceRegex += caseInsensetiveRegex + c;
                    }

                    int tIndex = paper.getText().toLowerCase().indexOf(term);
                    if (tIndex > -1) {
                        String snippet = result.getSnippet();
                        // Is the term actually already there?
                        if (snippet != null && snippet.toLowerCase().contains(term)) {
                            result.setSnippet(snippet.replaceAll(termReplaceRegex, "<b>" + term + "</b>"));
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
                        snippet += paper.getText().substring(sIndexStart, sIndexEnd).replaceAll(termReplaceRegex,
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
