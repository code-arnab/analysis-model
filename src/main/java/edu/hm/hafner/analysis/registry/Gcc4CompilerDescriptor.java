package edu.hm.hafner.analysis.registry;

import edu.hm.hafner.analysis.parser.Gcc4CompilerParser;

/**
 * A Descriptor for the Gcc 4 Compiler parser.
 *
 * @author Lorenz Munsch
 */
class Gcc4CompilerDescriptor extends ParserDescriptor {
    private static final String ID = "gcc4";
    private static final String NAME = "GNU C Compiler (gcc)";

    Gcc4CompilerDescriptor() {
        super(ID, NAME);
    }

    @Override
    public edu.hm.hafner.analysis.IssueParser createParser() {
        return new Gcc4CompilerParser();
    }
}
