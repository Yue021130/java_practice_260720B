package com.example.threadpooladvanced;

import com.example.threadpooladvanced.service.ThreadPoolExperimentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class RejectionPolicyTest {

    @Autowired
    private ThreadPoolExperimentService experimentService;

    @Test
    void abortPolicyShouldRejectOverflow() {
        com.example.threadpooladvanced.dto.RejectionExperimentResult result = experimentService.experimentRejection("AbortPolicy", 10);
        assertThat(result.getSubmitted()).isEqualTo(10);
        assertThat(result.getRejected()).isGreaterThan(0);
    }

    @Test
    void discardPolicyShouldNotThrowButDrop() {
        com.example.threadpooladvanced.dto.RejectionExperimentResult result = experimentService.experimentRejection("DiscardPolicy", 10);
        assertThat(result.getSubmitted()).isEqualTo(10);
        // DiscardPolicy 静默丢弃，executed 远小于 submitted，且不会抛出 RejectedExecutionException
        assertThat(result.getExecuted()).isLessThan(result.getSubmitted());
        assertThat(result.getRejected()).isZero();
    }
}
