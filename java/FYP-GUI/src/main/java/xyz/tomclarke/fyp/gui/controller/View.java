package xyz.tomclarke.fyp.gui.controller;

import java.io.IOException;
import java.util.List;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import xyz.tomclarke.fyp.gui.dao.KeyPhrase;
import xyz.tomclarke.fyp.gui.dao.KeyPhraseRepository;
import xyz.tomclarke.fyp.gui.dao.PaperRepository;
import xyz.tomclarke.fyp.gui.dao.Synonym;
import xyz.tomclarke.fyp.gui.dao.SynonymRepository;

/**
 * Allows viewing and actions on a paper
 * 
 * @author tbc452
 *
 */
@Controller
@RequestMapping(value = "/view")
public class View {

    @Autowired
    private PaperRepository paperRepo;
    @Autowired
    private KeyPhraseRepository kpRepo;
    // @Autowired
    // private HyponymRepository hypRepo;
    @Autowired
    private SynonymRepository synRepo;

    @RequestMapping(method = RequestMethod.GET)
    public ModelAndView index(@RequestParam("paper") Long paperId) {
        ModelAndView mv = new ModelAndView("view");
        boolean validPaper = false;

        xyz.tomclarke.fyp.gui.dao.Paper paper = paperRepo.findOne(paperId);
        if (paper != null) {
            validPaper = true;
            mv.addObject("paper", paper);
            List<KeyPhrase> kps = kpRepo.findByPaper(paper);
            mv.addObject("kps", kps);
            // List<Hyponym> hyps = hypRepo.findByKp(kps);
            // mv.addObject("hyps", hyps);
            List<Synonym> syns = synRepo.findByKp(kps);
            mv.addObject("syns", syns);
        }
        mv.addObject("id", paperId);
        mv.addObject("validPaper", validPaper);
        mv.addObject("title", validPaper ? paper.getTitle() : "Paper not found");

        return mv;
    }

    @RequestMapping(value = "/download", method = RequestMethod.GET)
    public void download(@RequestParam("paper") Long paperId, HttpServletResponse response) throws IOException {
        // Get the paper information
        xyz.tomclarke.fyp.gui.dao.Paper paper = paperRepo.findOne(paperId);
        List<KeyPhrase> kps = kpRepo.findByPaper(paper);
        // List<Hyponym> hyps = hypRepo.findByKp(kps);
        List<Synonym> syns = synRepo.findByKp(kps);

        // Send information
        response.setContentType("text/plain");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + paper.getTitle() + ".ann\"");

        ServletOutputStream out = response.getOutputStream();

        for (KeyPhrase kp : kps) {
            out.println(kp.toString());
        }
        // for (Hyponym hyp : hyps) {
        // out.println(hyp.toString());
        // }
        String synToSend = "";
        Long currentId = 0L;
        for (Synonym syn : syns) {
            // If a new ID, write info and clear data
            if (currentId != syn.getId() && !synToSend.isEmpty()) {
                out.println(synToSend);
                synToSend = "";
            }

            if (synToSend.isEmpty()) {
                synToSend = syn.toString();
                currentId = syn.getId();
            } else {
                synToSend += " " + syn.getId();
            }
        }
        // Write any remaining data
        if (!synToSend.isEmpty()) {
            out.println(synToSend);
            synToSend = "";
        }

        out.flush();
    }

}
