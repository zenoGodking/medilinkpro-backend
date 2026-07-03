package com.medilinkpro.backend.service;

import com.medilinkpro.backend.dto.mapper.PatientMapper;
import com.medilinkpro.backend.dto.request.PatientUpdateRequest;
import com.medilinkpro.backend.dto.response.PatientResponse;
import com.medilinkpro.backend.entity.Patient;
import com.medilinkpro.backend.exception.ResourceNotFoundException;
import com.medilinkpro.backend.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;
    private final PatientMapper patientMapper;

    @Transactional(readOnly = true)
    public List<PatientResponse> findAll() {
        return patientRepository.findAll().stream()
                .map(patientMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PatientResponse findById(UUID id) {
        return patientMapper.toResponse(getPatientOrThrow(id));
    }

    @Transactional
    public PatientResponse update(UUID id, PatientUpdateRequest request) {
        Patient patient = getPatientOrThrow(id);

        if (request.getNom() != null) patient.setNom(request.getNom());
        if (request.getPrenom() != null) patient.setPrenom(request.getPrenom());
        if (request.getTelephone() != null) patient.setTelephone(request.getTelephone());
        if (request.getDateNaissance() != null) patient.setDateNaissance(request.getDateNaissance());
        if (request.getGroupeSanguin() != null) patient.setGroupeSanguin(request.getGroupeSanguin());
        if (request.getAllergies() != null) patient.setAllergies(request.getAllergies());
        if (request.getAntecedents() != null) patient.setAntecedents(request.getAntecedents());
        if (request.getNumSecuriteSociale() != null) patient.setNumSecuriteSociale(request.getNumSecuriteSociale());

        return patientMapper.toResponse(patientRepository.save(patient));
    }

    @Transactional
    public void delete(UUID id) {
        Patient patient = getPatientOrThrow(id);
        patientRepository.delete(patient);
    }

    private Patient getPatientOrThrow(UUID id) {
        return patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient non trouve avec l'id : " + id));
    }
}
