package com.medilinkpro.backend.service.sms;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NotificationSmsServiceTest {

    @Test
    void lesSmsRestentDansLAlphabetStandard() {
        assertThat(NotificationSmsService.versAlphabetSms("Rendez-vous confirmé à 9h, être à l'heure, « reçu » — œil"))
                .isEqualTo("Rendez-vous confirmé à 9h, etre à l'heure, \" recu \" ? oeil");
    }
}
