package com.ho.account.asset.infrastructure.messaging;

import com.ho.account.asset.application.port.out.AssetEventPort;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class AssetEventAdapter implements AssetEventPort {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Override
    public void sendAssetEvent(String topic, Map<String, Object> eventData) {
        kafkaTemplate.send(topic, eventData);
    }
}
