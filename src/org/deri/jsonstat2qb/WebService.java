package org.deri.jsonstat2qb;

import com.hp.hpl.jena.rdf.model.Model;
import java.io.IOException;
import java.io.StringWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

public class WebService extends AbstractHandler {

    @Override
    public void handle(String target,
            Request req,
            HttpServletRequest request,
            HttpServletResponse response)
            throws IOException, ServletException {
        if (target.toLowerCase().equals("/convert")) {
            handleConvert(req, request, response);
            return;
        }
        response.setContentType("text/html;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        req.setHandled(true);
        response.getWriter().println("<h1>JSON-state2qb service</h1>");
        response.getWriter().println("<pre>usage: convert?url=fileurl</pre>");
    }

    public void handleConvert(
            Request baseRequest,
            HttpServletRequest request,
            HttpServletResponse response)
            throws IOException, ServletException {
        String url = request.getParameter("url");
        Model model = jsonstat2qb.jsonstat2qb(url);
        StringWriter out = new StringWriter();
        model.write(out, "RDF/XML-ABBREV");
        response.setContentType("application/rdf+xml;charset=utf-8");
        response.setStatus(HttpServletResponse.SC_OK);
        baseRequest.setHandled(true);
        response.getWriter().println(out.toString());
    }

}
