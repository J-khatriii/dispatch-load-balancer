package com.jaswant.dispatch;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OpenApiDocsTest {

    @Autowired
    private MockMvcTester mvc;

    @Test
    void apiDocsDescribeTheApiAndItsThreeEndpoints() {
        var docs = assertThat(mvc.get().uri("/v3/api-docs").exchange());

        docs.hasStatusOk();
        docs.bodyJson().extractingPath("$.info.title").isEqualTo("Dispatch Load Balancer API");
        docs.bodyJson().extractingPath("$.paths['/api/dispatch/orders'].post.summary").isNotNull();
        docs.bodyJson().extractingPath("$.paths['/api/dispatch/vehicles'].post.summary").isNotNull();
        docs.bodyJson().extractingPath("$.paths['/api/dispatch/plan'].get.summary").isNotNull();
    }

    @Test
    void errorResponsesAreDocumented() {
        var docs = assertThat(mvc.get().uri("/v3/api-docs").exchange());

        docs.bodyJson().extractingPath("$.paths['/api/dispatch/orders'].post.responses['400']").isNotNull();
        docs.bodyJson().extractingPath("$.paths['/api/dispatch/vehicles'].post.responses['400']").isNotNull();
        docs.bodyJson().extractingPath("$.paths['/api/dispatch/plan'].get.responses['409']").isNotNull();
    }

    @Test
    void swaggerUiIsServed() {
        assertThat(mvc.get().uri("/swagger-ui/index.html").exchange()).hasStatusOk();
    }
}