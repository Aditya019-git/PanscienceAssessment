package com.panscience.assessment.service;

import java.util.List;

public interface EmbeddingService {

    boolean isAvailable();

    String model();

    List<List<Double>> embed(List<String> inputs);
}
