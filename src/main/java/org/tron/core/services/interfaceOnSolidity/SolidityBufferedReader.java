package org.tron.core.services.interfaceOnSolidity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.stream.Stream;

public class SolidityBufferedReader extends BufferedReader {

    private List<String> lines;

    public SolidityBufferedReader(Reader in, List<String> lines) {
        super(in);
        this.lines = lines;
    }

    @Override
    public Stream<String> lines() {
        return lines.stream();
    }
}
