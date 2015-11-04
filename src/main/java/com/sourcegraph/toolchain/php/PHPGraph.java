package com.sourcegraph.toolchain.php;

import com.sourcegraph.toolchain.core.GraphWriter;
import com.sourcegraph.toolchain.core.PathUtil;
import com.sourcegraph.toolchain.php.antlr4.PHPLexer;
import com.sourcegraph.toolchain.php.antlr4.PHPParser;
import org.antlr.v4.runtime.ANTLRFileStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PHPGraph {

    private static final Logger LOGGER = LoggerFactory.getLogger(PHPGraph.class);

    GraphWriter writer;

    /**
     * keeps global and function-level variables. At each level holds map
     * variable name => is_local
     */
    Stack<Map<String, Boolean>> vars = new Stack<>();

    Map<String, ClassInfo> classes = new HashMap<>();

    private Set<String> visited = new HashSet<>();
    private Set<String> files;

    public PHPGraph(GraphWriter writer) {
        this.writer = writer;
    }

    public void process(Collection<String> files) {
        this.files = new HashSet<>();
        for (String file : files) {
            this.files.add(PathUtil.relativizeCwd(file));
        }
        for (String file : files) {
            process(file, null);
        }
    }

    protected void process(String file, String from) {
        if (from != null) {
            file = PathUtil.concat(new File(from).getParentFile(), file).getPath();
        }
        file = PathUtil.relativizeCwd(file);
        if (visited.contains(file)) {
            return;
        }
        if (!files.contains(file)) {
            return;
        }
        LOGGER.info("Processing {}", file);
        visited.add(file);
        try {
            FileGrapher extractor = new FileGrapher(this, file);

            CharStream stream = new ANTLRFileStream(file);
            PHPLexer lexer = new PHPLexer(stream);
            lexer.removeErrorListeners();
            lexer.addErrorListener(extractor);

            CommonTokenStream tokens = new CommonTokenStream(lexer);
            PHPParser parser = new PHPParser(tokens);
            parser.removeErrorListeners();
            parser.addErrorListener(extractor);
            PHPParser.HtmlDocumentContext tree = parser.htmlDocument(); // parse
            ParseTreeWalker walker = new ParseTreeWalker(); // create standard walker
            walker.walk(extractor, tree); // initiate walk of tree with listener
        } catch (IOException e) {
            LOGGER.warn("Failed to process {}: {}", file, e.getMessage());
        } catch (Exception e) {
            LOGGER.error("Failed to process {} - unexpected error", file, e);
        }
    }
}