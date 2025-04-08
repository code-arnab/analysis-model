package edu.hm.hafner.analysis.parser;

import org.json.JSONArray;
import org.json.JSONObject;

import edu.hm.hafner.analysis.Issue;
import edu.hm.hafner.analysis.IssueBuilder;
import edu.hm.hafner.analysis.Report;
import edu.hm.hafner.analysis.RevApiInfoExtension;
import edu.hm.hafner.analysis.Severity;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.io.Serial;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *  Parser for Revapi reports.
 */
public class RevApiParser extends JsonIssueParser {
    @Serial
    private static final long serialVersionUID = -2452699725595063377L;
    private static final int CAPACITY = 1024;

    @Override
    protected void parseJsonArray(final Report report, final JSONArray jsonReport, final IssueBuilder issueBuilder) {
        for (Object issue : jsonReport) {
            if (issue instanceof JSONObject object) {
                report.add(convertToIssue(object, issueBuilder));
            }
            else {
                report.logError("RevApi issues no instance of JSON");
            }
        }
    }

    private Issue convertToIssue(final JSONObject jsonIssue, final IssueBuilder builder) {
        builder.setSeverity(evaluateSeverity(jsonIssue.getJSONArray("classification")));
        builder.setDescription(getDescription(jsonIssue));
        addAttachments(jsonIssue.getJSONArray("attachments"), builder);
        builder.setAdditionalProperties(convertToGroup(jsonIssue));
        return builder.build();
    }

    private RevApiInfoExtension convertToGroup(final JSONObject jsonIssue) {
        return new RevApiInfoExtension(
                jsonIssue.getString("code"),
                extractChange(jsonIssue, "old"),
                extractChange(jsonIssue, "new"),
                extractSeverities(jsonIssue));
    }

    private static Map<String, String> extractSeverities(final JSONObject jsonIssue) {
        Map<String, String> allSeverities = new HashMap<>();
        for (Object severity : jsonIssue.getJSONArray("classification")) {
            if (severity instanceof JSONObject object) {
                allSeverities.put(object.getString("compatibility"), object.getString("severity"));
            }
        }
        return allSeverities;
    }

    private static String extractChange(final JSONObject jsonIssue, final String key) {
        var value = jsonIssue.get(key).toString();
        return "null".equals(value) ? "-" : value;
    }

    private void addAttachments(final JSONArray attachments, final IssueBuilder builder) {
        var packageName = attachments.getJSONObject(0).getString("value");
        var classSimpleName = attachments.getJSONObject(2).getString("value");
        var elementKind = attachments.getJSONObject(3).getString("value");
        builder.setFileName(classSimpleName);
        builder.setPackageName(packageName);
        builder.setCategory(elementKind);
    }

    private Severity evaluateSeverity(final JSONArray classification) {
        Set<Severity> allSeverities = new HashSet<>();
        for  (Object severity : classification) {
            if (severity instanceof JSONObject object) {
                allSeverities.add(toSeverity(object.getString("severity")));
            }
        }
        if (allSeverities.contains(Severity.WARNING_HIGH)) {
            return Severity.WARNING_HIGH;
        }
        if (allSeverities.contains(Severity.WARNING_NORMAL)) {
            return Severity.WARNING_NORMAL;
        }
        return Severity.WARNING_LOW;
    }

    private Severity toSeverity(final String level) {
        return switch (level) {
            case "BREAKING" -> Severity.WARNING_HIGH;
            case "POTENTIALLY_BREAKING" -> Severity.WARNING_NORMAL;
            //case "EQUIVALENT" & "NON_BREAKING"
            default -> Severity.WARNING_LOW;
        };
    }

    @SuppressFBWarnings(value = "POTENTIAL_XML_INJECTION", justification = "Message is cleaned in UI")
    private String getDescription(final JSONObject jsonIssue) {
        var severityDescription = new StringBuilder(CAPACITY);
        for  (Object severity :  jsonIssue.getJSONArray("classification")) {
            if (severity instanceof JSONObject object) {
                severityDescription.append("<p>Compatibility: ")
                        .append(object.getString("compatibility"))
                        .append(" Severity: ")
                        .append(object.getString("severity"))
                        .append("</p>");
            }
        }

        return MessageFormat.format(
                "<p><div><b>File</b>: {0}</div><div><b>Description:</b> {1} {2}</div><div><b>Change type:</b> {3}",
                jsonIssue.getJSONArray("attachments").getJSONObject(1).getString("value"),
                jsonIssue.getString("description"),
                jsonIssue.getString("name"),
                jsonIssue.getString("code")) + severityDescription;
    }
}
