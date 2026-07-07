package com.ntnn.messaging;

import com.ntnn.pipeline.PipelineDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
public class FileIngestConsumer {
    private static final Logger log = LoggerFactory.getLogger(FileIngestConsumer.class);

    private final PipelineDispatcher pipelineDispatcher;

    @Autowired
    public FileIngestConsumer(PipelineDispatcher pipelineDispatcher) {
        this.pipelineDispatcher = pipelineDispatcher;
    }

    @JmsListener(destination = "${app.activemq.queue.file-ingest:stdf.file.ingest}",
            concurrency = "10-15"
    )
    public void processFile(String filePath) {
        log.info("Received file to ingest from ActiveMQ: {}", filePath);
        try {
            pipelineDispatcher.ingest(filePath);
            log.info("Successfully processed file ingestion: {}", filePath);
        } catch (Exception e) {
            log.error("Failed to process file ingestion: {}", filePath, e);
        }
    }
}
