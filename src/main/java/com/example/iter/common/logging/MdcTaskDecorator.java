package com.example.iter.common.logging;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;
import org.springframework.stereotype.Component;

import java.util.Map;

/** 요청 스레드의 MDC 값을 비동기 작업 스레드로 전달하고 작업 종료 후 원래 값으로 복원한다. */
@Component
public class MdcTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        Map<String, String> submittingContext = MDC.getCopyOfContextMap();

        return () -> {
            Map<String, String> workerContext = MDC.getCopyOfContextMap();
            try {
                replaceContext(submittingContext);
                runnable.run();
            } finally {
                replaceContext(workerContext);
            }
        };
    }

    private void replaceContext(Map<String, String> context) {
        if (context == null) {
            MDC.clear();
            return;
        }
        MDC.setContextMap(context);
    }
}
