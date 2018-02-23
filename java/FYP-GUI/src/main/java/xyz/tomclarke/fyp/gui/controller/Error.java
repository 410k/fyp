package xyz.tomclarke.fyp.gui.controller;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.ErrorAttributes;
import org.springframework.boot.autoconfigure.web.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.ModelAndView;

/**
 * Handles GUI errors
 * 
 * @author tbc452
 *
 */
@Controller
public class Error implements ErrorController {

    private static final Logger log = LogManager.getLogger(Error.class);
    private static final String PATH = "/error";
    @Value("${debug}")
    private boolean debug;
    @Autowired
    private ErrorAttributes errorAttributes;

    @RequestMapping(value = PATH)
    @ExceptionHandler(Exception.class)
    public ModelAndView error(HttpServletRequest request) {
        ModelAndView mv = new ModelAndView("error");
        RequestAttributes requestAttributes = new ServletRequestAttributes(request);
        Map<String, Object> errorInfo = errorAttributes.getErrorAttributes(requestAttributes, debug);
        mv.addAllObjects(errorInfo);
        mv.addObject("debug", debug);
        String queryString = request.getQueryString();
        if (queryString == null || queryString.isEmpty()) {
            // No query
            queryString = "";
        } else {
            // Query string there, so make it look valid in printing with a ?
            queryString = "?" + queryString;
        }
        log.error("Spring error: " + errorInfo.get("status") + " for " + request.getRequestURL().toString()
                + queryString + " - " + errorInfo.get("message"));
        return mv;
    }

    @Override
    public String getErrorPath() {
        return PATH;
    }

}
