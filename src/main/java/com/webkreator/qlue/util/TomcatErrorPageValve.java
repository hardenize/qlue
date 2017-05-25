package com.webkreator.qlue.util;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ErrorReportValve;
import org.apache.jasper.runtime.ExceptionUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;

public class TomcatErrorPageValve extends ErrorReportValve {

    private String errorPagesLocation = null;

    public TomcatErrorPageValve() {
        super();
        errorPagesLocation = System.getProperties().getProperty("QLUE_ERROR_PAGES");
    }

    @Override
    protected void report(Request request, Response response, Throwable throwable) {
        try {
            outputErrorPage(request, response, errorPagesLocation);
        } catch(Exception e) {
            ExceptionUtils.handleThrowable(e);
        }
    }

    public static void outputErrorPage(HttpServletRequest request, HttpServletResponse response, String errorPagesLocation) throws IOException {
        int statusCode = response.getStatus();

        // Servlet error properties of interest:
        // - javax.servlet.error.exception
        // - javax.servlet.error.status_code
        // - javax.servlet.error.servlet_name
        // - javax.servlet.error.request_uri

        Exception exception = (Exception)request.getAttribute("javax.servlet.error.exception");
        if (exception != null) {
            statusCode = 500;
        }

        File errorFile = findErrorPage(request, statusCode, errorPagesLocation);
        if (errorFile != null) {
            response.setContentType("text/html; charset=utf-8");
            sendFile(response, errorFile);
        } else {
            // No file, send the hardcoded error response.
            String message = WebUtil.getStatusMessage(statusCode);
            if (message == null) {
                message = "Unknown Status Code (" + statusCode + ")";
            }

            response.setContentType("text/html; charset=utf-8");
            PrintWriter out = response.getWriter();
            out.print("<!DOCTYPE html>\n<html><head><title>");
            out.print(HtmlEncoder.html(message));
            out.println("</title></head>");
            out.print("<body><h1>");
            out.print(HtmlEncoder.html(message));
            out.println("</h1>");
            WebUtil.writePagePaddingforInternetExplorer(out);
            out.println("</body></html>");
        }
    }

    protected static File findErrorPage(HttpServletRequest request, int statusCode, String errorPagesLocation) {
        if (errorPagesLocation == null) {
            return null;
        }

        File errorPagesHome = new File(errorPagesLocation);
        File f = null;

        f = new File(errorPagesHome, "error-" + statusCode + ".html");
        if (f.exists() && f.canRead()) {
            return f;
        }

        f = new File(errorPagesHome, "catch-all.html");
        if (f.exists() && f.canRead()) {
            return f;
        }

        return null;
    }

    protected static void sendFile(HttpServletResponse response, File file) throws IOException {
        PrintWriter out = response.getWriter();
        try (InputStream in = new FileInputStream(file);
             BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            String line;
            while ((line = reader.readLine()) != null) {
                out.write(line);
            }
        }
    }
}
