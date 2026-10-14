package com.disputeintake.eval;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.core.type.TypeReference;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public final class EvalCaseLoader {

    private EvalCaseLoader() {
    }

    public static List<EvalCase> load(Path file) throws IOException {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper.readValue(file.toFile(), new TypeReference<List<EvalCase>>() {
        });
    }
}
