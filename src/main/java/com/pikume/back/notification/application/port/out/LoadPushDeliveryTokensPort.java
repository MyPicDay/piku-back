package com.pikume.back.notification.application.port.out;

import java.util.Set;

public interface LoadPushDeliveryTokensPort {

	Set<String> loadPushDeliveryTokens(String userId);
}
