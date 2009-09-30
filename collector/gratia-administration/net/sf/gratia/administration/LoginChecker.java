package net.sf.gratia.administration;

import net.sf.gratia.util.Configuration;
import java.util.Properties;
import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;

class LoginChecker {
    static boolean checkLogin(HttpServletRequest request, HttpServletResponse response)
        throws IOException {
        String fqan = (String) request.getSession().getAttribute("FQAN");
        boolean login = true;
        if (fqan == null)
            login = false;
        else if (fqan.indexOf("NoPrivileges") > -1)
            login = false;
         
        String uriPart = request.getRequestURI();
        int slash2 = uriPart.substring(1).indexOf("/") + 1;
        uriPart = uriPart.substring(slash2);
        String queryPart = request.getQueryString();
        if (queryPart == null)
            queryPart = "";
        else
            queryPart = "?" + queryPart;
         
        request.getSession().setAttribute("displayLink", "." + uriPart + queryPart);
         
        if (login) {
            
            return true;
            
        } else {
            Properties p = Configuration.getProperties();
            String loginLink = p.getProperty("service.secure.connection") + request.getContextPath() + "/gratia-login.jsp";
            String redirectLocation = response.encodeRedirectURL(loginLink);
            response.sendRedirect(redirectLocation);
            request.getSession().setAttribute("displayLink", "." + uriPart + queryPart);

            return false;
        }
    }
}
