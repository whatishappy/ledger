package com.ledger.modules.imports.parser;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface BillParser {

    boolean supports(String source, String filename);

    List<RawBillRow> parse(InputStream is) throws IOException;
}
