package com.medilinkpro.backend.service;

import com.medilinkpro.backend.dto.mapper.MedecinMapper;
import com.medilinkpro.backend.dto.request.MedecinUpdateRequest;
import com.medilinkpro.backend.dto.response.MedecinResponse;
import com.medilinkpro.backend.entity.EtablissementSante;
import com.medilinkpro.backend.entity.Medecin;
import com.medilinkpro.backend.exception.ResourceNotFoundException;
import com.medilinkpro.backend.repository.EtablissementRepository;
import com.medilinkpro.backend.repository.MedecinRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MedecinService {

    private final MedecinRepository medecinRepository;
    private final EtablissementRepository etablissementRepository;
    private final MedecinMapper medecinMapper;

    @Transactional(readOnly = true)
    public List<MedecinResponse> findAll() {
        return medecinRepository.findAll().stream()
                .map(medecinMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MedecinResponse findById(UUID id) {
        return medecinMapper.toResponse(getMedecinOrThrow(id));
    }

    /**
     * Recherche de specialistes geolocalises (Module 2 - F12).
     * Si lat/lng ne sont pas fournis, la recherche se fait uniquement par specialite.
     */
    @Transactional(readOnly = true)
    public List<MedecinResponse> rechercher(String specialite, Double lat, Double lng) {
        List<Medecin> medecins;
        if (lat != null && lng != null) {
            medecins = medecinRepository.rechercherParSpecialiteEtLocalisation(specialite, lat, lng);
        } else if (specialite != null && !specialite.isBlank()) {
            medecins = medecinRepository.findBySpecialiteIgnoreCaseContaining(specialite);
        } else {
            medecins = medecinRepository.findAll();
        }

        return medecins.stream()
                .map(m -> {
                    MedecinResponse response = medecinMapper.toResponse(m);
                    if (lat != null && lng != null && m.getLatitude() != null && m.getLongitude() != null) {
                        response.setDistanceApprox(distanceApproximativeKm(lat, lng, m.getLatitude(), m.getLongitude()));
                    }
                    return response;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public MedecinResponse update(UUID id, MedecinUpdateRequest request) {
        Medecin medecin = getMedecinOrThrow(id);

        if (request.getNom() != null) medecin.setNom(request.getNom());
        if (request.getPrenom() != null) medecin.setPrenom(request.getPrenom());
        if (request.getTelephone() != null) medecin.setTelephone(request.getTelephone());
        if (request.getSpecialite() != null) medecin.setSpecialite(request.getSpecialite());
        if (request.getNumeroOrdre() != null) medecin.setNumeroOrdre(request.getNumeroOrdre());
        if (request.getLatitude() != null) medecin.setLatitude(request.getLatitude());
        if (request.getLongitude() != null) medecin.setLongitude(request.getLongitude());
        if (request.getTarif() != null) medecin.setTarif(request.getTarif());
        if (request.getVerifie() != null) medecin.setVerifie(request.getVerifie());
        if (request.getEtablissementId() != null) {
            EtablissementSante etablissement = etablissementRepository.findById(request.getEtablissementId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Etablissement non trouve avec l'id : " + request.getEtablissementId()));
            medecin.setEtablissement(etablissement);
        }

        return medecinMapper.toResponse(medecinRepository.save(medecin));
    }

    @Transactional
    public void delete(UUID id) {
        Medecin medecin = getMedecinOrThrow(id);
        medecinRepository.delete(medecin);
    }

    private Medecin getMedecinOrThrow(UUID id) {
        return medecinRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medecin non trouve avec l'id : " + id));
    }

    /**
     * Distance approximative en kilometres (formule de Haversine simplifiee).
     * Suffisante pour un affichage indicatif ; pour une precision cartographique,
     * utiliser une extension geospatiale comme PostGIS.
     */
    private double distanceApproximativeKm(double lat1, double lon1, double lat2, double lon2) {
        final int rayonTerreKm = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return Math.round(rayonTerreKm * c * 100.0) / 100.0;
    }
}
