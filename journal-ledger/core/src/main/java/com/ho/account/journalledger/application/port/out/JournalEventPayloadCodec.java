package com.ho.account.journalledger.application.port.out;

import java.util.Map;

/** Keeps JSON technology outside the event-processing application service. */
public interface JournalEventPayloadCodec {
    String encode(Map<String, Object> event);

    Map<String, Object> decode(String payloadJson);
}
