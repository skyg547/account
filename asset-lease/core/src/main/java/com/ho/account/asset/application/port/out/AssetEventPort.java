package com.ho.account.asset.application.port.out;

import java.util.Map;

public interface AssetEventPort {
    void sendAssetEvent(String topic, Map<String, Object> eventData);
}
