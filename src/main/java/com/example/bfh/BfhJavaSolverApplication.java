
package com.example.bfh;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import com.example.bfh.dto.GenerateRequest;
import com.example.bfh.dto.GenerateResponse;
import com.example.bfh.dto.SubmitRequest;
import com.example.bfh.model.SolutionRecord;
import com.example.bfh.repo.SolutionRepository;
import com.example.bfh.service.SqlChooser;

import java.time.OffsetDateTime;
import java.util.Optional;

@SpringBootApplication
public class BfhJavaSolverApplication {

    public static void main(String[] args) {
        SpringApplication.run(BfhJavaSolverApplication.class, args);
    }

    @Bean
    WebClient webClient(WebClient.Builder builder) {
        return builder.build();
    }

    @Bean
    CommandLineRunner runner(
            WebClient webClient,
            SolutionRepository repo,
            SqlChooser sqlChooser,
            @Value("${bfh.participant.name}") String name,
            @Value("${bfh.participant.regNo}") String regNo,
            @Value("${bfh.participant.email}") String email,
            @Value("${bfh.endpoints.generateWebhook}") String generateUrl,
            @Value("${bfh.endpoints.fallbackSubmit}") String fallbackSubmit,
            @Value("${bfh.http.authScheme}") String authScheme
    ) {
        return args -> {
            // 1) Generate webhook
            GenerateRequest req = new GenerateRequest(name, regNo, email);
            GenerateResponse generated = webClient.post()
                    .uri(generateUrl)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(req)
                    .retrieve()
                    .bodyToMono(GenerateResponse.class)
                    .block();

            if (generated == null || generated.getWebhook() == null || generated.getAccessToken() == null) {
                throw new IllegalStateException("Failed to obtain webhook or accessToken. Response: " + generated);
            }

            // 2) Decide question ODD/EVEN & pick SQL
            String finalQuery = sqlChooser.chooseSqlFor(regNo);
            String questionType = sqlChooser.getTypeFor(regNo);

            // 3) Persist locally
            SolutionRecord rec = new SolutionRecord();
            rec.setRegNo(regNo);
            rec.setQuestionType(questionType);
            rec.setFinalQuery(finalQuery);
            rec.setSubmittedAt(OffsetDateTime.now());
            repo.save(rec);

            // 4) Submit to returned webhook (or fallback)
            String submitUrl = Optional.ofNullable(generated.getWebhook()).orElse(fallbackSubmit);

            String authValue = "PLAIN".equalsIgnoreCase(authScheme) ? generated.getAccessToken()
                    : "Bearer " + generated.getAccessToken();

            SubmitRequest body = new SubmitRequest(finalQuery);

            String submitResponse = webClient.post()
                    .uri(submitUrl)
                    .header(HttpHeaders.AUTHORIZATION, authValue)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            System.out.println("Submission response: " + submitResponse);
        };
    }
}
