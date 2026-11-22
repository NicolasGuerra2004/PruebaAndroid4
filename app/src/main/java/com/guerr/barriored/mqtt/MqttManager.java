package com.guerr.barriored.mqtt;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.gson.Gson;
import com.guerr.barriored.model.Alert;
import com.guerr.barriored.utils.NotificationHelper;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLSocketFactory;

public class MqttManager {
    private static final String TAG = "MqttManager";
    public static final String BROKER_URL = "ssl://m4a7b642.ala.us-east-1.emqxsl.com:8883";
    public static final String MQTT_USER = "test";
    public static final String MQTT_PASSWORD = "test";

    // Tópicos especificados: comunidades/comunidad01/alertas/+
    public static final String TOPIC_ALERTS_SUB = "comunidades/comunidad01/alertas/+";
    public static final String TOPIC_ALERTS_PUB_PREFIX = "comunidades/comunidad01/alertas/";

    private static volatile MqttManager instance;

    private final Context appContext;
    private final ExecutorService executorService;
    private final Handler mainHandler;
    private final Gson gson;
    private final CopyOnWriteArrayList<MqttStatusListener> statusListeners;

    // Filtro anti-duplicados en memoria para QoS 1
    private final Set<String> processedAlertIds;

    private MqttClient mqttClient;
    private boolean isConnecting = false;

    public interface MqttStatusListener {
        void onConnectionStatusChanged(boolean connected, String statusMessage);
        void onAlertMessageReceived(Alert alert);
    }

    private MqttManager(Context context) {
        this.appContext = context.getApplicationContext();
        this.executorService = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.gson = new Gson();
        this.statusListeners = new CopyOnWriteArrayList<>();
        this.processedAlertIds = Collections.synchronizedSet(new HashSet<>());
    }

    public static MqttManager getInstance(Context context) {
        if (instance == null) {
            synchronized (MqttManager.class) {
                if (instance == null) {
                    instance = new MqttManager(context);
                }
            }
        }
        return instance;
    }

    public void addListener(MqttStatusListener listener) {
        if (listener != null && !statusListeners.contains(listener)) {
            statusListeners.add(listener);
            boolean connected = isConnected();
            listener.onConnectionStatusChanged(connected, connected ? "Conectado a BarrioRed en vivo" : "Reconectando...");
        }
    }

    public void removeListener(MqttStatusListener listener) {
        if (listener != null) {
            statusListeners.remove(listener);
        }
    }

    public boolean isConnected() {
        return mqttClient != null && mqttClient.isConnected();
    }

    public synchronized void connect() {
        if (isConnected() || isConnecting) {
            return;
        }

        isConnecting = true;
        notifyStatus(false, "Conectando con BarrioRed...");

        executorService.execute(() -> {
            try {
                String clientId = "BarrioRed_" + UUID.randomUUID().toString().substring(0, 8);
                if (mqttClient == null) {
                    mqttClient = new MqttClient(BROKER_URL, clientId, new MemoryPersistence());
                }

                MqttConnectOptions options = new MqttConnectOptions();
                options.setUserName(MQTT_USER);
                options.setPassword(MQTT_PASSWORD.toCharArray());
                options.setCleanSession(true);
                options.setAutomaticReconnect(true);
                options.setConnectionTimeout(15);
                options.setKeepAliveInterval(30);

                options.setSocketFactory((SSLSocketFactory) SSLSocketFactory.getDefault());

                mqttClient.setCallback(new MqttCallbackExtended() {
                    @Override
                    public void connectComplete(boolean reconnect, String serverURI) {
                        Log.i(TAG, "MQTT Connected! Reconnect=" + reconnect);
                        isConnecting = false;
                        subscribeToAlerts();
                        notifyStatus(true, "Conectado a BarrioRed en vivo");
                    }

                    @Override
                    public void connectionLost(Throwable cause) {
                        Log.w(TAG, "MQTT Connection lost: " + (cause != null ? cause.getMessage() : "Unknown"));
                        isConnecting = false;
                        notifyStatus(false, "Reconectando con BarrioRed...");
                    }

                    @Override
                    public void messageArrived(String topic, MqttMessage message) {
                        try {
                            String payload = new String(message.getPayload(), StandardCharsets.UTF_8);
                            Log.d(TAG, "MQTT Message arrived on [" + topic + "]: " + payload);
                            Alert alert = gson.fromJson(payload, Alert.class);
                            if (alert != null) {
                                handleIncomingAlert(alert);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing incoming MQTT alert", e);
                        }
                    }

                    @Override
                    public void deliveryComplete(IMqttDeliveryToken token) {
                        Log.d(TAG, "MQTT Delivery complete");
                    }
                });

                mqttClient.connect(options);

            } catch (Exception e) {
                Log.e(TAG, "MQTT Connect failed: " + e.getMessage(), e);
                isConnecting = false;
                notifyStatus(false, "Desconectado: " + e.getMessage());
            }
        });
    }

    private void subscribeToAlerts() {
        if (!isConnected()) return;
        try {
            mqttClient.subscribe(TOPIC_ALERTS_SUB, 1);
            Log.i(TAG, "Subscribed successfully to: " + TOPIC_ALERTS_SUB);
        } catch (MqttException e) {
            Log.e(TAG, "Error subscribing to " + TOPIC_ALERTS_SUB, e);
        }
    }

    private void handleIncomingAlert(Alert alert) {
        String alertId = alert.getId();

        // Filtro anti-duplicados en memoria para QoS 1
        if (alertId != null && !alertId.isEmpty()) {
            if (!processedAlertIds.add(alertId)) {
                Log.d(TAG, "Alerta duplicada omitida por filtro anti-duplicados: " + alertId);
                return;
            }
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String currentUid = currentUser != null ? currentUser.getUid() : "";

        // Solo disparar notificación del sistema si la alerta fue emitida por otro usuario
        if (alert.getAutorUid() == null || !alert.getAutorUid().equals(currentUid)) {
            NotificationHelper.showEmergencyNotification(appContext, alert);
        }

        // Notificar a los listeners activos de la interfaz
        mainHandler.post(() -> {
            for (MqttStatusListener listener : statusListeners) {
                listener.onAlertMessageReceived(alert);
            }
        });
    }

    public void publishAlert(Alert alert, PublishCallback callback) {
        executorService.execute(() -> {
            if (!isConnected()) {
                mainHandler.post(() -> {
                    if (callback != null) callback.onError(new Exception("Cliente MQTT desconectado"));
                });
                return;
            }

            try {
                String categoryTopic = alert.getCategoria() != null ? alert.getCategoria().toLowerCase() : "general";
                String topic = TOPIC_ALERTS_PUB_PREFIX + categoryTopic;
                String jsonPayload = gson.toJson(alert);

                MqttMessage message = new MqttMessage(jsonPayload.getBytes(StandardCharsets.UTF_8));
                message.setQos(1);
                // Obligatorio: retained = false para evitar reaparición de alarmas antiguas
                message.setRetained(false);

                // Agregar al filtro para no auto-notificar en caso de echo local
                if (alert.getId() != null) {
                    processedAlertIds.add(alert.getId());
                }

                mqttClient.publish(topic, message);
                Log.i(TAG, "Alert published to " + topic + " (retained=false): " + jsonPayload);

                mainHandler.post(() -> {
                    if (callback != null) callback.onSuccess();
                });
            } catch (Exception e) {
                Log.e(TAG, "Error publishing alert to MQTT", e);
                mainHandler.post(() -> {
                    if (callback != null) callback.onError(e);
                });
            }
        });
    }

    public void disconnect() {
        executorService.execute(() -> {
            try {
                if (mqttClient != null) {
                    if (mqttClient.isConnected()) {
                        mqttClient.disconnect();
                    }
                    mqttClient.close();
                    mqttClient = null;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error disconnecting MQTT", e);
            } finally {
                isConnecting = false;
                notifyStatus(false, "Desconectado");
            }
        });
    }

    private void notifyStatus(boolean connected, String message) {
        mainHandler.post(() -> {
            for (MqttStatusListener listener : statusListeners) {
                listener.onConnectionStatusChanged(connected, message);
            }
        });
    }

    public interface PublishCallback {
        void onSuccess();
        void onError(Exception e);
    }
}
