package edu.hm.hafner.analysis.parser;

import edu.hm.hafner.analysis.Issue;
import edu.hm.hafner.analysis.IssueBuilder;
import edu.hm.hafner.analysis.LookaheadParser;
import edu.hm.hafner.analysis.ParsingException;
import edu.hm.hafner.analysis.Severity;
import edu.hm.hafner.analysis.util.IntegerParser;
import edu.hm.hafner.util.LookaheadStream;

import java.io.Serial;
import java.util.Optional;
import java.util.regex.Matcher;

/**
 * A parser for messages from the NAG Fortran Compiler.
 *
 * @author Mat Cross.
 */
public class NagFortranParser extends LookaheadParser {
    @Serial
    private static final long serialVersionUID = 2072414911276743946L;

    private static final String NAGFOR_MSG_PATTERN = "^(Info|Warning|Questionable|Extension|Obsolescent|Deleted "
            + "feature used|Error|Runtime Error|Fatal Error|Panic|Non-standard\\(Obsolete\\)|Extension\\(NAG\\)|Extension\\(F[0-9]+\\)): "
            + "(.+\\.[^,:\\n]+)(, line (\\d+))?: (.+(\\s+detected"
            + " at .+)?)";

    /**
     * Creates a new instance of {@link NagFortranParser}.
     */
    public NagFortranParser() {
        super(NAGFOR_MSG_PATTERN);
    }

    @Override
    protected Optional<Issue> createIssue(final Matcher matcher, final LookaheadStream lookahead,
            final IssueBuilder builder)
            throws ParsingException {
        var messageBuilder = new StringBuilder(matcher.group(5));

        while (lookahead.hasNext("\\s+ .+")) {
            messageBuilder.append("\n");
            messageBuilder.append(lookahead.next());
        }

        return builder.setFileName(matcher.group(2))
                .setLineStart(IntegerParser.parseInt(matcher.group(4)))
                .setCategory(matcher.group(1))
                .setMessage(messageBuilder.toString())
                .setSeverity(mapPriority(matcher.group(1)))
                .buildOptional();
    }

    private Severity mapPriority(final String category) {
        return switch (category) {
            case "Error", "Runtime Error", "Fatal Error", "Panic" -> Severity.WARNING_HIGH;
            case "Info" -> Severity.WARNING_LOW;
            default -> Severity.WARNING_NORMAL;
        };
    }
}
