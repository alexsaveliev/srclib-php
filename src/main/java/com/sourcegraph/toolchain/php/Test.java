package com.sourcegraph.toolchain.php;

import com.sourcegraph.toolchain.core.GraphData;
import com.sourcegraph.toolchain.core.GraphWriter;
import com.sourcegraph.toolchain.core.JSONUtil;

import java.util.Arrays;

public class Test {

    public static void main(String args[]) throws Exception {

        GraphWriter writer = new GraphData();

        PHPGraph graph = new PHPGraph(writer);
        graph.process(Arrays.asList(args));
        writer.flush();

        JSONUtil.writeJSON(writer);
    }
}
