package com.example.threadpooladvanced;

import com.example.threadpooladvanced.service.ThreadPoolExperimentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class QueueExperimentTest {

    @Autowired
    private ThreadPoolExperimentService experimentService;

    @Test
    void arrayBlockingQueueShouldRejectWhenFull() {
        com.example.threadpooladvanced.dto.QueueExperimentResult result = experimentService.experimentQueue("ArrayBlockingQueue", 3, 10);
        assertThat(result.getSubmitted()).isEqualTo(10);
        assertThat(result.getAccepted()).isEqualTo(3);
        assertThat(result.getRejected()).isEqualTo(7);
    }

    @Test
    void synchronousQueueShouldAcceptNoneWithoutConsumer() {
        com.example.threadpooladvanced.dto.QueueExperimentResult result = experimentService.experimentQueue("SynchronousQueue", 5, 5);
        assertThat(result.getAccepted()).isZero();
        assertThat(result.getRejected()).isEqualTo(5);
    }
}
