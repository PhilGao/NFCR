package com.example.nfcr;

import android.util.Log;

import com.microsoft.azure.sdk.iot.device.IotHubConnectionStatusChangeCallback;
import com.microsoft.azure.sdk.iot.device.IotHubConnectionStatusChangeReason;
import com.microsoft.azure.sdk.iot.device.transport.IotHubConnectionStatus;

class IotHubConnectionStatusChangeCallbackLogger implements IotHubConnectionStatusChangeCallback {
    @Override
    public void execute(IotHubConnectionStatus status, IotHubConnectionStatusChangeReason statusChangeReason, Throwable throwable, Object callbackContext) {
        System.out.println();
        Log.d("IOT", "CONNECTION STATUS UPDATE: " + status);
        Log.d("IOT", "CONNECTION STATUS REASON: " + statusChangeReason);
        Log.d("IOT", "CONNECTION STATUS THROWABLE: " + (throwable == null ? "null" : throwable.getMessage()));
        System.out.println();

        if (throwable != null) {
            throwable.printStackTrace();
        }

        if (status == IotHubConnectionStatus.DISCONNECTED) {
            //connection was lost, and is not being re-established. Look at provided exception for
            // how to resolve this issue. Cannot send messages until this issue is resolved, and you manually
            // re-open the device client
        } else if (status == IotHubConnectionStatus.DISCONNECTED_RETRYING) {
            //connection was lost, but is being re-established. Can still send messages, but they won't
            // be sent until the connection is re-established
        } else if (status == IotHubConnectionStatus.CONNECTED) {
            //Connection was successfully re-established. Can send messages.
        }
    }
}
