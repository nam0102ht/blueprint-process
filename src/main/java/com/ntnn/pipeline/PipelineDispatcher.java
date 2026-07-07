package com.ntnn.pipeline;

import com.ntnn.database.DatabaseClient;
import com.ntnn.messaging.ActiveMqClient;
import com.ntnn.stdf.ByteOrderHelper;
import com.ntnn.stdf.StdfParser;
import com.ntnn.stdf.StdfRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.foreign.MemorySegment;
import java.util.concurrent.Semaphore;
import java.util.concurrent.StructuredTaskScope;

@Component
public class PipelineDispatcher {
    private final DatabaseClient dbClient;
    private final ActiveMqClient activeMqClient;
    private final Semaphore semaphore;

    @Autowired
    public PipelineDispatcher(DatabaseClient dbClient, ActiveMqClient activeMqClient) {
        this.dbClient = dbClient;
        this.activeMqClient = activeMqClient;
        this.semaphore = new Semaphore(50);
    }

    public void ingest(String filePath) throws Exception {
        try (StdfParser parser = new StdfParser(filePath)) {
            ByteOrderHelper orderHelper = parser.getOrderHelper();
            String currentLotId = "UNKNOWN_LOT";

            try (var scope = StructuredTaskScope.open(StructuredTaskScope.Joiner.awaitAllSuccessfulOrThrow())) {
                while (parser.hasNext()) {
                    StdfParser.RawRecord rawRecord = parser.nextRecord();
                    if (rawRecord == null) {
                        break;
                    }

                    int type = rawRecord.type();
                    int subtype = rawRecord.subtype();
                    MemorySegment body = rawRecord.body();

                    if (type == 1 && subtype == 10) {
                        StdfRecord.MirRecord mir = StdfParser.parseMir(body, orderHelper);
                        dbClient.insertLot(mir);
                        currentLotId = mir.lotId();
                        activeMqClient.publishRecord("stdf.lot.meta", mir);
                    } else if (type == 15 && subtype == 10) {
                        final String lotId = currentLotId;
                        
                        scope.fork(() -> {
                            semaphore.acquire();
                            try {
                                StdfRecord.PtrRecord ptr = StdfParser.parsePtr(body, orderHelper);
                                dbClient.insertTestResult(lotId, ptr);
                                activeMqClient.publishRecord("stdf.test.results", ptr);
                            } finally {
                                semaphore.release();
                            }
                            return null;
                        });
                    } else if (type == 1 && subtype == 20) {
                        StdfRecord.MrrRecord mrr = StdfParser.parseMrr(body, orderHelper);
                        activeMqClient.publishRecord("stdf.lot.summary", mrr);
                    }
                }
                
                scope.join();
            }
        }
    }
}
