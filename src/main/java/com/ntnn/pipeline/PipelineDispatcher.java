package com.ntnn.pipeline;

import com.ntnn.database.DatabaseClient;
import com.ntnn.messaging.ActiveMqClient;
import com.ntnn.stdf.ByteOrderHelper;
import com.ntnn.stdf.StdfParser;
import com.ntnn.stdf.StdfRecord;
import java.lang.foreign.MemorySegment;
import java.util.concurrent.Semaphore;
import java.util.concurrent.StructuredTaskScope;

public class PipelineDispatcher {
    private final DatabaseClient dbClient;
    private final ActiveMqClient activeMqClient;
    private final Semaphore semaphore;

    public PipelineDispatcher(DatabaseClient dbClient, ActiveMqClient activeMqClient, int maxConcurrency) {
        this.dbClient = dbClient;
        this.activeMqClient = activeMqClient;
        this.semaphore = new Semaphore(maxConcurrency);
    }

    public void ingest(String filePath) throws Exception {
        try (StdfParser parser = new StdfParser(filePath)) {
            ByteOrderHelper orderHelper = parser.getOrderHelper();
            String currentLotId = "UNKNOWN_LOT";

            // StructuredTaskScope guarantees clean cleanup and propagation of failures
            try (var scope = StructuredTaskScope.open()) {
                while (parser.hasNext()) {
                    StdfParser.RawRecord rawRecord = parser.nextRecord();
                    if (rawRecord == null) {
                        break;
                    }

                    int type = rawRecord.type();
                    int subtype = rawRecord.subtype();
                    MemorySegment body = rawRecord.body();

                    if (type == 1 && subtype == 10) {
                        // MIR: Master Information Record (Process synchronously to establish Lot metadata)
                        StdfRecord.MirRecord mir = StdfParser.parseMir(body, orderHelper);
                        dbClient.insertLot(mir);
                        currentLotId = mir.lotId();
                        // Also publish MIR to ActiveMQ
                        activeMqClient.publishRecord("stdf.lot.meta", mir);
                    } else if (type == 15 && subtype == 10) {
                        // PTR: Parametric Test Record
                        // Capture local variables for the Virtual Thread closure
                        final String lotId = currentLotId;
                        
                        // Fork a Virtual Thread to process this record concurrently
                        scope.fork(() -> {
                            semaphore.acquire();
                            try {
                                // Lazy off-heap parsing: extraction of fields happens inside the Virtual Thread
                                StdfRecord.PtrRecord ptr = StdfParser.parsePtr(body, orderHelper);
                                
                                // Write to DB
                                dbClient.insertTestResult(lotId, ptr);
                                
                                // Publish to ActiveMQ queue
                                activeMqClient.publishRecord("stdf.test.results", ptr);
                            } finally {
                                semaphore.release();
                            }
                            return null;
                        });
                    } else if (type == 1 && subtype == 20) {
                        // MRR: Master Results Record (Process synchronously at the end)
                        StdfRecord.MrrRecord mrr = StdfParser.parseMrr(body, orderHelper);
                        activeMqClient.publishRecord("stdf.lot.summary", mrr);
                    }
                }
                
                // Wait for all Virtual Threads to finish and propagate any exceptions
                scope.join();
            }
        }
    }
}
