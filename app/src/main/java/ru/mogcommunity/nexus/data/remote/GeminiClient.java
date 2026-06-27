package ru.mogcommunity.rbrproject.data.remote;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GeminiClient {
    private static final String BASE_URL = ru.mogcommunity.rbrproject.BuildConfig.GEMINI_BASE_URL;
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient client;
    private final Gson gson;
    private final Handler mainHandler;

    public interface GeminiCallback {
        void onSuccess(String plan);
        void onError(String errorMessage);
    }

    public GeminiClient() {
        okhttp3.OkHttpClient.Builder builder = new okhttp3.OkHttpClient.Builder();
        builder.protocols(java.util.Arrays.asList(okhttp3.Protocol.HTTP_1_1));
        builder.dns(new okhttp3.Dns() {
            @NonNull
            @Override
            public List<InetAddress> lookup(@NonNull String hostname) throws UnknownHostException {
                if ("generativelanguage.googleapis.com".equals(hostname)) {
                    android.util.Log.d("RBR_GeminiClient", "Resolving Gemini API hostname through xbox-dns.ru DNS...");
                    try {
                        List<InetAddress> resolved = resolveXboxDns(hostname);
                        if (resolved != null && !resolved.isEmpty()) {
                            android.util.Log.d("RBR_GeminiClient", "Bypassed successfully! Resolved IPs: " + resolved);
                            return resolved;
                        }
                    } catch (Exception e) {
                        android.util.Log.e("RBR_GeminiClient", "Failed to resolve via xbox-dns.ru, falling back to system DNS", e);
                    }
                }
                return okhttp3.Dns.SYSTEM.lookup(hostname);
            }
        });
        this.client = builder.build();
        this.gson = new Gson();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void analyzeError(String apiKey, String prompt, String imageUrl, android.content.Context context, GeminiCallback callback) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            callback.onError("Gemini API key is not specified in Settings.");
            return;
        }

        String cleanApiKey = apiKey.trim();
        String url = BASE_URL + "?key=" + cleanApiKey;

        okhttp3.OkHttpClient clientToUse = this.client;
        
        ru.mogcommunity.rbrproject.data.local.AppDatabase.databaseWriteExecutor.execute(() -> {
            byte[] imageBytes = null;
            String mimeType = "image/jpeg";

            if (imageUrl != null && !imageUrl.trim().isEmpty() && context != null) {
                String cleanUrl = imageUrl.trim();
                if (cleanUrl.startsWith("http://") || cleanUrl.startsWith("https://")) {
                    try {
                        Request imgRequest = new Request.Builder().url(cleanUrl).build();
                        try (Response response = clientToUse.newCall(imgRequest).execute()) {
                            if (response.isSuccessful() && response.body() != null) {
                                imageBytes = response.body().bytes();
                                String contentType = response.header("Content-Type");
                                if (contentType != null && contentType.startsWith("image/")) {
                                    mimeType = contentType;
                                }
                            }
                        }
                    } catch (Exception e) {
                        android.util.Log.e("RBR_GeminiClient", "Failed to download image: " + cleanUrl, e);
                    }
                } else if (cleanUrl.startsWith("content://") || cleanUrl.startsWith("file://") || cleanUrl.startsWith("/")) {
                    try {
                        android.net.Uri uri = android.net.Uri.parse(cleanUrl);
                        String resolvedType = context.getContentResolver().getType(uri);
                        if (resolvedType != null) {
                            mimeType = resolvedType;
                        }
                        try (java.io.InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
                            if (inputStream != null) {
                                java.io.ByteArrayOutputStream byteBuffer = new java.io.ByteArrayOutputStream();
                                byte[] buffer = new byte[1024];
                                int len;
                                while ((len = inputStream.read(buffer)) != -1) {
                                    byteBuffer.write(buffer, 0, len);
                                }
                                imageBytes = byteBuffer.toByteArray();
                            }
                        }
                    } catch (Exception e) {
                        android.util.Log.e("RBR_GeminiClient", "Failed to load local image: " + cleanUrl, e);
                    }
                }
            }

            final byte[] finalImageBytes = imageBytes;
            final String finalMimeType = mimeType;

            GeminiRequest geminiRequest = new GeminiRequest(prompt, finalImageBytes, finalMimeType);
            String jsonPayload = gson.toJson(geminiRequest);
            android.util.Log.d("RBR_GeminiClient", "Request Payload: " + jsonPayload);

            RequestBody body = RequestBody.create(jsonPayload, JSON);
            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();

            clientToUse.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NonNull Call call, @NonNull IOException e) {
                    android.util.Log.e("RBR_GeminiClient", "Network error: " + e.getMessage(), e);
                    mainHandler.post(() -> callback.onError("Network error: " + e.getMessage()));
                }

                @Override
                public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        String errorBody = response.body() != null ? response.body().string() : "";
                        android.util.Log.e("RBR_GeminiClient", "Server error (" + response.code() + "): " + errorBody);
                        
                        String parsedError = "";
                        try {
                            GeminiErrorResponse err = gson.fromJson(errorBody, GeminiErrorResponse.class);
                            if (err != null && err.error != null && err.error.message != null) {
                                parsedError = err.error.message;
                            }
                        } catch (Exception ignored) {}
                        
                        final String finalError = !parsedError.isEmpty() ? parsedError : errorBody;
                        mainHandler.post(() -> callback.onError("Ошибка (" + response.code() + "): " + finalError));
                        return;
                    }

                    if (response.body() == null) {
                        mainHandler.post(() -> callback.onError("Empty response body from server"));
                        return;
                    }

                    try {
                        String jsonResponse = response.body().string();
                        GeminiResponse geminiResponse = gson.fromJson(jsonResponse, GeminiResponse.class);
                        if (geminiResponse != null && geminiResponse.candidates != null && !geminiResponse.candidates.isEmpty()) {
                            GeminiResponse.Candidate candidate = geminiResponse.candidates.get(0);
                            if (candidate.content != null && candidate.content.parts != null && !candidate.content.parts.isEmpty()) {
                                String reply = candidate.content.parts.get(0).text;
                                mainHandler.post(() -> callback.onSuccess(reply));
                                return;
                            }
                        }
                        mainHandler.post(() -> callback.onError("Failed to parse Gemini response"));
                    } catch (Exception e) {
                        mainHandler.post(() -> callback.onError("Parsing error: " + e.getMessage()));
                    }
                }
            });
        });
    }

    private static class GeminiRequest {
        @SerializedName("contents")
        List<Content> contents;

        GeminiRequest(String prompt, byte[] imageBytes, String mimeType) {
            this.contents = new ArrayList<>();
            Content content = new Content();
            content.parts = new ArrayList<>();
            
            Part textPart = new Part();
            textPart.text = prompt;
            content.parts.add(textPart);

            if (imageBytes != null && imageBytes.length > 0) {
                Part imgPart = new Part();
                InlineData inlineData = new InlineData();
                inlineData.mimeType = mimeType;
                inlineData.data = android.util.Base64.encodeToString(imageBytes, android.util.Base64.NO_WRAP);
                imgPart.inlineData = inlineData;
                content.parts.add(imgPart);
            }

            this.contents.add(content);
        }

        static class Content {
            @SerializedName("parts")
            List<Part> parts;
        }

        static class Part {
            @SerializedName("text")
            String text;

            @SerializedName("inlineData")
            InlineData inlineData;
        }

        static class InlineData {
            @SerializedName("mimeType")
            String mimeType;

            @SerializedName("data")
            String data;
        }
    }

    private static class GeminiResponse {
        @SerializedName("candidates")
        List<Candidate> candidates;

        static class Candidate {
            @SerializedName("content")
            Content content;
        }

        static class Content {
            @SerializedName("parts")
            List<Part> parts;
        }

        static class Part {
            @SerializedName("text")
            String text;
        }
    }

    private static class GeminiErrorResponse {
        @SerializedName("error")
        ErrorDetail error;

        static class ErrorDetail {
            @SerializedName("message")
            String message;
        }
    }

    private List<InetAddress> resolveXboxDns(String hostname) throws Exception {
        String[] dnsServers = {"111.88.96.50", "111.88.96.51"};
        Exception lastException = null;
        for (String dnsServer : dnsServers) {
            try {
                return resolveDnsUdp(hostname, dnsServer);
            } catch (Exception e) {
                lastException = e;
            }
        }
        if (lastException != null) {
            throw lastException;
        }
        throw new UnknownHostException("Failed to resolve via xbox-dns.ru DNS servers");
    }

    private List<InetAddress> resolveDnsUdp(String hostname, String dnsServerIp) throws Exception {
        List<InetAddress> result = new ArrayList<>();
        byte[] request = buildDnsQuery(hostname);

        DatagramSocket socket = new DatagramSocket();
        socket.setSoTimeout(3000);

        InetAddress server = InetAddress.getByName(dnsServerIp);
        DatagramPacket sendPacket = new DatagramPacket(request, request.length, server, 53);
        socket.send(sendPacket);

        byte[] buffer = new byte[1024];
        DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
        socket.receive(receivePacket);
        socket.close();

        parseDnsResponse(buffer, receivePacket.getLength(), result);
        return result;
    }

    private byte[] buildDnsQuery(String hostname) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(0x12);
        baos.write(0x34);
        baos.write(0x01);
        baos.write(0x00);
        baos.write(0x00);
        baos.write(0x01);
        baos.write(0x00);
        baos.write(0x00);
        baos.write(0x00);
        baos.write(0x00);
        baos.write(0x00);
        baos.write(0x00);

        String[] parts = hostname.split("\\.");
        for (String part : parts) {
            byte[] bytes = part.getBytes("UTF-8");
            baos.write(bytes.length);
            baos.write(bytes);
        }
        baos.write(0x00);

        baos.write(0x00);
        baos.write(0x01);

        baos.write(0x00);
        baos.write(0x01);

        return baos.toByteArray();
    }

    private void parseDnsResponse(byte[] response, int length, List<InetAddress> result) throws Exception {
        if (length < 12) return;
        
        int answersCount = ((response[6] & 0xFF) << 8) | (response[7] & 0xFF);
        int idx = 12;
        idx = skipName(response, idx, length);
        idx += 4;

        for (int i = 0; i < answersCount; i++) {
            if (idx >= length) break;
            idx = skipName(response, idx, length);
            if (idx + 10 > length) break;

            int type = ((response[idx] & 0xFF) << 8) | (response[idx + 1] & 0xFF);
            int cls = ((response[idx + 2] & 0xFF) << 8) | (response[idx + 3] & 0xFF);
            idx += 8;

            int rdLength = ((response[idx] & 0xFF) << 8) | (response[idx + 1] & 0xFF);
            idx += 2;

            if (idx + rdLength > length) break;

            if (type == 1 && cls == 1 && rdLength == 4) {
                byte[] ip = new byte[4];
                System.arraycopy(response, idx, ip, 0, 4);
                result.add(InetAddress.getByAddress(ip));
            } else if (type == 28 && cls == 1 && rdLength == 16) {
                byte[] ip = new byte[16];
                System.arraycopy(response, idx, ip, 0, 16);
                result.add(InetAddress.getByAddress(ip));
            }
            idx += rdLength;
        }
    }

    private int skipName(byte[] response, int idx, int length) {
        while (idx < length) {
            int len = response[idx] & 0xFF;
            if (len == 0) {
                idx++;
                break;
            }
            if ((len & 0xC0) == 0xC0) {
                idx += 2;
                break;
            }
            idx += 1 + len;
        }
        return idx;
    }
}

