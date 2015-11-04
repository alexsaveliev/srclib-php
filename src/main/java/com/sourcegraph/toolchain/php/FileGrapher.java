package com.sourcegraph.toolchain.php;

import com.sourcegraph.toolchain.core.Def;
import com.sourcegraph.toolchain.core.Ref;
import com.sourcegraph.toolchain.php.antlr4.PHPParserBaseListener;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.BitSet;

public class FileGrapher extends PHPParserBaseListener implements ANTLRErrorListener {

    private static Logger LOGGER = LoggerFactory.getLogger(FileGrapher.class);

    private PHPGraph graph;

    private String file;

    public FileGrapher(PHPGraph graph, String file) {
        this.graph = graph;
        this.file = file;
    }

    protected Def def(ParserRuleContext ctx, String kind) {
        Def def = new Def();
        def.defStart = ctx.getStart().getStartIndex();
        def.defEnd = ctx.getStop().getStopIndex();
        def.name = ctx.getText();
        def.file = this.file;
        def.kind = kind;
        return def;
    }

    protected Def def(Token token, String kind) {
        Def def = new Def();
        def.defStart = token.getStartIndex();
        def.defEnd = token.getStopIndex();
        def.name = token.getText();
        def.file = this.file;
        def.kind = kind;
        return def;
    }

    protected Ref ref(ParserRuleContext ctx) {
        Ref ref = new Ref();
        ref.start = ctx.getStart().getStartIndex();
        ref.end = ctx.getStop().getStopIndex();
        ref.file = this.file;
        return ref;
    }

    protected Ref ref(TerminalNode node) {
        Ref ref = new Ref();
        ref.start = node.getSymbol().getStartIndex();
        ref.end = node.getSymbol().getStopIndex();
        ref.file = this.file;
        return ref;
    }

    protected void emit(Def def) {
        try {
            graph.writer.writeDef(def);
        } catch (IOException e) {
            e.printStackTrace(); // TODO
        }
        // auto-adding self-references
        Ref ref = new Ref();
        ref.defKey = def.defKey;
        ref.def = true;
        ref.start = def.defStart;
        ref.end = def.defEnd;
        ref.file = def.file;
        emit(ref);
    }

    protected void emit(Ref ref) {
        try {
            graph.writer.writeRef(ref);
        } catch (IOException e) {
            e.printStackTrace(); // TODO
        }
    }

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer,
                            Object offendingSymbol,
                            int line,
                            int charPositionInLine,
                            String msg,
                            RecognitionException e) {
        LOGGER.warn("{} at {}:{}: {}", file, line, charPositionInLine, msg);
    }

    @Override
    public void reportAmbiguity(Parser parser,
                                DFA dfa,
                                int i,
                                int i1,
                                boolean b,
                                BitSet bitSet,
                                ATNConfigSet atnConfigSet) {

    }

    @Override
    public void reportAttemptingFullContext(Parser parser,
                                            DFA dfa,
                                            int i,
                                            int i1,
                                            BitSet bitSet,
                                            ATNConfigSet atnConfigSet) {

    }

    @Override
    public void reportContextSensitivity(Parser parser, DFA dfa, int i, int i1, int i2, ATNConfigSet atnConfigSet) {
    }
}