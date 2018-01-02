package xyz.tomclarke.fyp.gui.service;

import xyz.tomclarke.fyp.gui.dao.Paper;

/**
 * Represents what an NLP processor needs to be able to do
 * 
 * @author tbc452
 *
 */
public interface NlpProcessor {

    /**
     * Ensures all objects needed for processing are loaded. If objects are pulled
     * from SQL, if they don't exist then they need to be created
     */
    public void loadObjects() throws Exception;

    /**
     * Processes a paper, adding or updating key phrases or relation records
     * 
     * @param paper
     */
    public void processPaper(Paper paper) throws Exception;

    /**
     * Unload objects created initially to allow for later stages to be loaded
     */
    public void unload() throws Exception;

}
