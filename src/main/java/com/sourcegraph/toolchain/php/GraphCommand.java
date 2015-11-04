package com.sourcegraph.toolchain.php;

import com.beust.jcommander.Parameter;
import com.google.gson.Gson;
import com.sourcegraph.toolchain.core.GraphData;
import com.sourcegraph.toolchain.core.GraphWriter;
import com.sourcegraph.toolchain.core.JSONUtil;
import com.sourcegraph.toolchain.core.SourceUnit;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.FileSystems;
import java.nio.file.Files;

public class GraphCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(GraphCommand.class);

    @Parameter(names = {"--debug-unit-file"}, description = "The path to a source unit input file, which will be read as though it came from stdin. Used to mimic stdin when you can't actually pipe to stdin (e.g., in IntelliJ run configurations).")
    String debugUnitFile;

    /**
     * The Source Unit that is read in from STDIN. Defined here, so that it can be
     * accessed within the anonymous classes below.
     */
    public static SourceUnit unit;

    /**
     * Main method
     */
    @SuppressWarnings("unchecked")
    public void Execute() {

        try {
            Reader r;
            if (!StringUtils.isEmpty(debugUnitFile)) {
                LOGGER.debug("Reading source unit JSON data from {}", debugUnitFile);
                r = Files.newBufferedReader(FileSystems.getDefault().getPath(debugUnitFile));
            } else {
                r = new InputStreamReader(System.in);
            }
            unit = new Gson().fromJson(r, SourceUnit.class);
            r.close();
        } catch (IOException e) {
            LOGGER.error("Failed to read source unit data", e);
            System.exit(1);
        }
        LOGGER.info("Building graph for {}", unit.Name);

        GraphWriter writer = new GraphData();

        try {
            LOGGER.debug("Starting graph collection");
            PHPGraph graph = new PHPGraph(writer);
            graph.process(unit.Files);
            LOGGER.debug("Graph collection complete");
            writer.flush();
        } catch (Exception e) {
            LOGGER.error("Unexpected error occurred while building graph", e);
            System.exit(1);
        }

        JSONUtil.writeJSON(writer);
    }
}
