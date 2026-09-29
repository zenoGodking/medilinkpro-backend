package com.medilinkpro.backend.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medilinkpro.backend.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/** Lit une empreinte faciale transmise en JSON dans une partie multipart ("[0.01, -0.12, ...]"). */
@Component
@RequiredArgsConstructor
public class DescripteursJson {

    private final ObjectMapper objectMapper;

    public List<Double> lire(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<Double>>() { });
        } catch (Exception e) {
            throw new BadRequestException("Empreinte faciale illisible");
        }
    }
}
