package com.medilinkpro.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/** Executeur dedie a l'envoi des notifications push (hors des threads de requete). */
@Configuration
public class PushConfig {

    @Bean(name = "executeurPush")
    public TaskExecutor executeurPush() {
        ThreadPoolTaskExecutor executeur = new ThreadPoolTaskExecutor();
        executeur.setCorePoolSize(2);
        executeur.setMaxPoolSize(4);
        executeur.setQueueCapacity(1000);
        executeur.setThreadNamePrefix("push-");
        executeur.initialize();
        return executeur;
    }
}
