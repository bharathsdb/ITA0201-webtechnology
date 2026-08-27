import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/**
 * Lightweight local development and demonstration server.
 * Serves the HTML frontend and routes POST requests through the exact
 * Student Result processing logic without requiring an external Tomcat installation.
 */
public class DevServer {

    private static final int PORT = 8081;

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

        // Handler for root and static files
        server.createContext("/", new StaticFileHandler());

        // Handler for Servlet POST endpoint
        server.createContext("/StudentResultServlet", new ServletHandler());

        server.setExecutor(java.util.concurrent.Executors.newFixedThreadPool(10));
        System.out.println("==================================================================");
        System.out.println(" Student Result Processing System - Live Server Started");
        System.out.println(" Access URL: http://localhost:" + PORT + "/index.html");
        System.out.println(" Endpoint:   http://localhost:" + PORT + "/StudentResultServlet");
        System.out.println(" Press Ctrl+C to stop the server.");
        System.out.println("==================================================================");
        server.start();
    }

    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/") || path.isEmpty()) {
                path = "/index.html";
            }

            File file = new File("." + path);
            if (!file.exists() || file.isDirectory()) {
                String response = "404 (Not Found)\nFile not found: " + path;
                exchange.sendResponseHeaders(404, response.getBytes(StandardCharsets.UTF_8).length);
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes(StandardCharsets.UTF_8));
                os.close();
                return;
            }

            String contentType = "text/plain";
            if (path.endsWith(".html")) contentType = "text/html; charset=UTF-8";
            else if (path.endsWith(".css")) contentType = "text/css; charset=UTF-8";
            else if (path.endsWith(".js")) contentType = "application/javascript; charset=UTF-8";
            else if (path.endsWith(".svg")) contentType = "image/svg+xml";

            byte[] bytes = Files.readAllBytes(file.toPath());
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.sendResponseHeaders(200, bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        }
    }

    static class ServletHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().set("Location", "index.html");
                exchange.sendResponseHeaders(302, -1);
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String msg = "Method Not Allowed";
                exchange.sendResponseHeaders(405, msg.length());
                OutputStream os = exchange.getResponseBody();
                os.write(msg.getBytes());
                os.close();
                return;
            }

            // Read POST body
            InputStream is = exchange.getRequestBody();
            String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            Map<String, String> params = parseFormData(body);

            // Process with local variables (Thread-Safe Concurrency)
            String rawStudentName = params.get("studentName");
            String rawRegNo = params.get("regNo");
            String rawSub1 = params.get("sub1");
            String rawSub2 = params.get("sub2");
            String rawSub3 = params.get("sub3");

            List<String> validationErrors = new ArrayList<>();

            String studentName = (rawStudentName != null) ? rawStudentName.trim() : "";
            String regNo = (rawRegNo != null) ? rawRegNo.trim() : "";

            if (studentName.isEmpty()) {
                validationErrors.add("Student Name is required and cannot be empty.");
            }
            if (regNo.isEmpty()) {
                validationErrors.add("Register Number is required and cannot be empty.");
            }

            double mark1 = parseAndValidateMark(rawSub1, "Subject 1 (Data Structures & Algorithms)", validationErrors);
            double mark2 = parseAndValidateMark(rawSub2, "Subject 2 (Database Management Systems)", validationErrors);
            double mark3 = parseAndValidateMark(rawSub3, "Subject 3 (Web Technologies & Servlets)", validationErrors);

            String htmlResponse;
            if (!validationErrors.isEmpty()) {
                htmlResponse = buildErrorPage(validationErrors);
            } else {
                double totalMarks = mark1 + mark2 + mark3;
                double averageMarks = totalMarks / 3.0;
                double highestMark = Math.max(mark1, Math.max(mark2, mark3));

                boolean isPass = (mark1 >= 40.0) && (mark2 >= 40.0) && (mark3 >= 40.0);
                String resultStatus = isPass ? "PASS" : "FAIL";

                String grade;
                String gradeColor;
                if (!isPass) {
                    grade = "Reappear / Arrear (F)";
                    gradeColor = "#f87171";
                } else if (averageMarks >= 75.0) {
                    grade = "First Class with Distinction";
                    gradeColor = "#34d399";
                } else if (averageMarks >= 60.0) {
                    grade = "First Class";
                    gradeColor = "#38bdf8";
                } else if (averageMarks >= 50.0) {
                    grade = "Second Class";
                    gradeColor = "#fbbf24";
                } else {
                    grade = "Third / Pass Class";
                    gradeColor = "#a3e635";
                }

                htmlResponse = buildSuccessPage(studentName, regNo, mark1, mark2, mark3,
                                                totalMarks, averageMarks, highestMark, resultStatus, grade, gradeColor);
            }

            byte[] responseBytes = htmlResponse.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, responseBytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(responseBytes);
            os.close();
        }

        private Map<String, String> parseFormData(String formData) {
            Map<String, String> map = new HashMap<>();
            if (formData == null || formData.isEmpty()) return map;
            String[] pairs = formData.split("&");
            for (String pair : pairs) {
                String[] parts = pair.split("=", 2);
                if (parts.length > 0) {
                    String key = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
                    String val = (parts.length > 1) ? URLDecoder.decode(parts[1], StandardCharsets.UTF_8) : "";
                    map.put(key, val);
                }
            }
            return map;
        }

        private double parseAndValidateMark(String rawValue, String subjectLabel, List<String> errors) {
            if (rawValue == null || rawValue.trim().isEmpty()) {
                errors.add(subjectLabel + ": Mark cannot be empty.");
                return 0.0;
            }
            try {
                double mark = Double.parseDouble(rawValue.trim());
                if (mark < 0.0 || mark > 100.0) {
                    errors.add(subjectLabel + ": Mark (" + rawValue.trim() + ") is invalid. Must be between 0 and 100.");
                    return 0.0;
                }
                return mark;
            } catch (NumberFormatException e) {
                errors.add(subjectLabel + ": Mark ('" + rawValue.trim() + "') must be a valid numeric value.");
                return 0.0;
            }
        }

        private String buildErrorPage(List<String> errors) {
            StringBuilder sb = new StringBuilder();
            sb.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n");
            sb.append("    <meta charset=\"UTF-8\">\n    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
            sb.append("    <title>Validation Error | Student Result Processing</title>\n");
            sb.append("    <link rel=\"stylesheet\" href=\"css/style.css\">\n</head>\n<body>\n");
            sb.append("    <div class=\"container\">\n        <div class=\"card\">\n");
            sb.append("            <div class=\"card-header\">\n");
            sb.append("                <span class=\"badge-tag\" style=\"background: rgba(239, 68, 68, 0.15); color: #f87171; border-color: rgba(239, 68, 68, 0.3);\">Validation Warning</span>\n");
            sb.append("                <h1>Invalid Form Submission</h1>\n");
            sb.append("                <p>The servlet detected invalid or missing input values during validation.</p>\n            </div>\n");
            sb.append("            <div class=\"error-card\">\n");
            sb.append("                <strong style=\"color: #f87171; font-size: 1rem;\">Please correct the following errors:</strong>\n");
            sb.append("                <ul class=\"error-list\">\n");
            for (String err : errors) {
                sb.append("                    <li>").append(escapeHtml(err)).append("</li>\n");
            }
            sb.append("                </ul>\n            </div>\n");
            sb.append("            <a href=\"index.html\" class=\"btn-action btn-secondary\">&larr; Return to Form</a>\n");
            sb.append("            <div class=\"footer-note\">Processed by StudentResultServlet (doPost Validation Phase)</div>\n");
            sb.append("        </div>\n    </div>\n</body>\n</html>");
            return sb.toString();
        }

        private String buildSuccessPage(String studentName, String regNo,
                                       double mark1, double mark2, double mark3,
                                       double totalMarks, double averageMarks, double highestMark,
                                       String resultStatus, String grade, String gradeColor) {
            StringBuilder sb = new StringBuilder();
            sb.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n");
            sb.append("    <meta charset=\"UTF-8\">\n    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
            sb.append("    <title>Student Result Card | ").append(escapeHtml(studentName)).append("</title>\n");
            sb.append("    <link rel=\"stylesheet\" href=\"css/style.css\">\n</head>\n<body>\n");
            sb.append("    <div class=\"container\">\n        <div class=\"card\">\n");
            sb.append("            <img src=\"campus.jpg\" alt=\"SIMATS Engineering Campus\" style=\"width: 100%; height: 200px; object-fit: cover; border-radius: var(--radius-md) var(--radius-md) 0 0; margin-top: -2.5rem; margin-left: -2.5rem; width: calc(100% + 5rem); margin-bottom: 2rem; border-bottom: 1px solid var(--border-color);\">\n");
            sb.append("            <div class=\"result-header\">\n");
            sb.append("                <span class=\"badge-tag\">Official Mark Statement</span>\n");
            sb.append("                <h1>Academic Evaluation Report</h1>\n");
            sb.append("                <p>Generated dynamically via Java Servlet <code>doPost()</code></p>\n");
            sb.append("            </div>\n");
            
            sb.append("            <div class=\"student-info-grid\">\n");
            sb.append("                <div class=\"info-item\"><span class=\"info-label\">Student Name</span><span class=\"info-value\">").append(escapeHtml(studentName)).append("</span></div>\n");
            sb.append("                <div class=\"info-item\"><span class=\"info-label\">Register Number</span><span class=\"info-value\">").append(escapeHtml(regNo)).append("</span></div>\n");
            sb.append("            </div>\n");

            sb.append("            <div class=\"stats-grid\">\n");
            sb.append("                <div class=\"stat-card\"><div class=\"stat-title\">TOTAL MARKS</div><div class=\"stat-num\">").append(String.format("%.1f", totalMarks)).append(" <span style=\"font-size:0.85rem;color:#64748b;\">/ 300</span></div></div>\n");
            sb.append("                <div class=\"stat-card\"><div class=\"stat-title\">AVERAGE SCORE</div><div class=\"stat-num\">").append(String.format("%.2f", averageMarks)).append("%</div></div>\n");
            sb.append("                <div class=\"stat-card\"><div class=\"stat-title\">HIGHEST MARK</div><div class=\"stat-num\" style=\"color:#34d399;\">").append(String.format("%.1f", highestMark)).append("</div></div>\n");
            sb.append("            </div>\n");

            sb.append("            <table class=\"marks-table\">\n                <thead><tr><th>Subject Details</th><th>Max Marks</th><th>Marks Obtained</th><th>Subject Status</th></tr></thead>\n<tbody>\n");
            appendSubjectRow(sb, "Subject 1: Data Structures & Algorithms", mark1, highestMark);
            appendSubjectRow(sb, "Subject 2: Database Management Systems", mark2, highestMark);
            appendSubjectRow(sb, "Subject 3: Web Technologies & Servlets", mark3, highestMark);
            sb.append("                </tbody>\n            </table>\n");

            sb.append("            <div style=\"background: rgba(15, 23, 42, 0.6); border: 1px solid var(--border-color); border-radius: var(--radius-md); padding: 1.25rem; margin-bottom: 1.5rem; display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 1rem;\">\n");
            sb.append("                <div>\n                    <div style=\"font-size: 0.75rem; color: var(--text-muted); font-weight: 700; text-transform: uppercase;\">Overall Result Status</div>\n");
            if ("PASS".equals(resultStatus)) {
                sb.append("                    <div class=\"status-badge pass\" style=\"margin-top: 0.35rem;\">&check; PASSED</div>\n");
            } else {
                sb.append("                    <div class=\"status-badge fail\" style=\"margin-top: 0.35rem;\">&cross; FAILED (Reappear)</div>\n");
            }
            sb.append("                </div>\n                <div style=\"text-align: right;\">\n");
            sb.append("                    <div style=\"font-size: 0.75rem; color: var(--text-muted); font-weight: 700; text-transform: uppercase;\">Classification / Grade</div>\n");
            sb.append("                    <div style=\"font-size: 1.15rem; font-weight: 800; color: ").append(gradeColor).append("; margin-top: 0.35rem;\">").append(grade).append("</div>\n");
            sb.append("                </div>\n            </div>\n");

            sb.append("            <div style=\"display: grid; grid-template-columns: 1fr 1fr; gap: 1rem;\">\n");
            sb.append("                <button onclick=\"window.print()\" class=\"btn-action btn-secondary\" style=\"margin-top:0;\"><svg width=\"16\" height=\"16\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\"><polyline points=\"6 9 6 2 18 2 18 9\"></polyline><path d=\"M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2\"></path><rect x=\"6\" y=\"14\" width=\"12\" height=\"8\"></rect></svg> Print Marksheet</button>\n");
            sb.append("                <a href=\"index.html\" class=\"btn-action\" style=\"margin-top:0;\"><svg width=\"16\" height=\"16\" viewBox=\"0 0 24 24\" fill=\"none\" stroke=\"currentColor\" stroke-width=\"2\"><line x1=\"12\" y1=\"5\" x2=\"12\" y2=\"19\"></line><line x1=\"5\" y1=\"12\" x2=\"19\" y2=\"12\"></line></svg> Process Another Student</a>\n");
            sb.append("            </div>\n");
            sb.append("            <div class=\"footer-note\">&bull; Concurrency Verified: Processed via thread-safe local stack variables.<br>&bull; Server Time: ").append(new java.util.Date()).append(" &bull; Thread: ").append(Thread.currentThread().getName()).append("</div>\n");
            sb.append("        </div>\n    </div>\n</body>\n</html>");
            return sb.toString();
        }

        private void appendSubjectRow(StringBuilder sb, String subjectName, double mark, double highestMark) {
            boolean passed = mark >= 40.0;
            boolean isHighest = (Double.compare(mark, highestMark) == 0);
            sb.append("                    <tr><td><strong>").append(escapeHtml(subjectName)).append("</strong>");
            if (isHighest) {
                sb.append(" <span style=\"font-size: 0.7rem; background: rgba(52, 211, 153, 0.15); color: #34d399; border: 1px solid rgba(52, 211, 153, 0.3); border-radius: 4px; padding: 2px 6px; margin-left: 6px; font-weight: 700;\">HIGHEST</span>");
            }
            sb.append("</td><td style=\"color: var(--text-muted);\">100</td><td class=\"mark-score\">").append(String.format("%.1f", mark)).append("</td>");
            if (passed) {
                sb.append("<td><span style=\"color: #34d399; font-weight: 700;\">&check; Pass (&#8805; 40)</span></td></tr>\n");
            } else {
                sb.append("<td><span style=\"color: #f87171; font-weight: 700;\">&cross; Fail (&lt; 40)</span></td></tr>\n");
            }
        }

        private String escapeHtml(String input) {
            if (input == null) return "";
            return input.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#x27;");
        }
    }
}
