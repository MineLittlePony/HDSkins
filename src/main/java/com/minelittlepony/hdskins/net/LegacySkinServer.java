package com.minelittlepony.hdskins.net;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.minelittlepony.hdskins.HDSkins;
import com.minelittlepony.hdskins.profile.EtagProfileTexture;
import com.minelittlepony.hdskins.profile.SkinType;
import com.minelittlepony.hdskins.util.CallableFutures;
import com.minelittlepony.hdskins.util.IndentedToStringStyle;
import com.minelittlepony.hdskins.util.net.HttpException;
import com.minelittlepony.hdskins.util.net.MoreHttpResponses;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import com.mojang.util.UUIDTypeAdapter;

import net.minecraft.client.MinecraftClient;

@Deprecated
@ServerType("legacy")
public class LegacySkinServer implements SkinServer {

    private static final String SERVER_ID = "7853dfddc358333843ad55a2c7485c4aa0380a51";

    private static final Logger logger = LogManager.getLogger();

    private final String address;
    private final String gateway;

    public LegacySkinServer(String address, @Nullable String gateway) {
        this.address = address;
        this.gateway = gateway;
    }

    @Override
    public CompletableFuture<TexturePayload> getPreviewTextures(GameProfile profile) {
        return CallableFutures.asyncFailableFuture(() -> {
            SkinServer.verifyServerConnection(MinecraftClient.getInstance().getSession(), SERVER_ID);

            if (Strings.isNullOrEmpty(gateway)) {
                throw gatewayUnsupported();
            }

            Map<SkinType, MinecraftProfileTexture> map = new HashMap<>();

            for (SkinType type : SkinType.values()) {
                map.put(type, new MinecraftProfileTexture(getPath(gateway, type, profile), null));
            }

            return new TexturePayload(profile, map);
        }, HDSkins.skinDownloadExecutor);
    }

    @Override
    public TexturePayload loadProfileData(GameProfile profile) throws IOException {
        ImmutableMap.Builder<SkinType, MinecraftProfileTexture> builder = ImmutableMap.builder();
        for (SkinType type : SkinType.values()) {

            String url = getPath(address, type, profile);
            try {
                builder.put(type, loadProfileTexture(profile, url));
            } catch (IOException e) {
                logger.trace("Couldn't find texture for {} at {}. Does it exist?", profile.getName(), url, e);
            }
        }

        Map<SkinType, MinecraftProfileTexture> map = builder.build();
        if (map.isEmpty()) {
            throw new HttpException(String.format("No textures found for %s at %s", profile, this.address), 404, null);
        }

        return new TexturePayload(profile, map);
    }

    private MinecraftProfileTexture loadProfileTexture(GameProfile profile, String url) throws IOException {
        try (MoreHttpResponses resp = MoreHttpResponses.execute(HDSkins.httpClient, new HttpHead(url))) {
            if (!resp.ok()) {
                throw new HttpException(resp.getResponse());
            }
            logger.debug("Found skin for {} at {}", profile.getName(), url);

            Header eTagHeader = resp.getResponse().getFirstHeader(HttpHeaders.ETAG);
            final String eTag = eTagHeader == null ? "" : StringUtils.strip(eTagHeader.getValue(), "\"").replaceAll("[^a-z0-9/._-]", "");

            // Add the ETag onto the end of the texture hash. Should properly cache the textures.
            return new EtagProfileTexture(url, eTag, null);
        }
    }

    @Override
    public SkinUpload.Response performSkinUpload(SkinUpload upload) throws IOException, AuthenticationException {
        if (Strings.isNullOrEmpty(gateway)) {
            throw gatewayUnsupported();
        }

        SkinServer.verifyServerConnection(upload.getSession(), SERVER_ID);

        RequestBuilder request = RequestBuilder.post().setUri(gateway);
        MultipartEntityBuilder entity = MultipartEntityBuilder.create();

        putFormData(entity, upload);

        if (upload.getImage() != null) {
            File f = new File(upload.getImage());
            entity.addBinaryBody(upload.getType().toString().toLowerCase(Locale.US), f,
                    ContentType.create("image/png"), f.getName());
        }

        MoreHttpResponses resp = MoreHttpResponses.execute(HDSkins.httpClient,
                request.setEntity(entity.build()).build());

        String response = resp.text();

        if (response.startsWith("ERROR: ")) {
            response = response.substring(7);
        }

        if (!response.equalsIgnoreCase("OK") && !response.endsWith("OK")) {
            throw new HttpException(response, resp.getResponseCode(), null);
        }

        return new SkinUpload.Response(response);
    }

    private UnsupportedOperationException gatewayUnsupported() {
        return new UnsupportedOperationException("Server does not have a gateway.");
    }

    private void putFormData(MultipartEntityBuilder entity, SkinUpload upload) {

        entity.addTextBody("user", upload.getSession().getUsername())
              .addTextBody("uuid", UUIDTypeAdapter.fromUUID(upload.getSession().getProfile().getId()))
              .addTextBody("type", upload.getType().toString().toLowerCase(Locale.US));

        if (upload.getImage() == null) {
            entity.addTextBody("clear", "1");
        }
    }

    private static String getPath(String address, SkinType type, GameProfile profile) {
        String uuid = UUIDTypeAdapter.fromUUID(profile.getId());
        String path = type.name().toLowerCase() + "s";
        return String.format("%s/%s/%s.png?%s", address, path, uuid, Long.toString(new Date().getTime() / 1000));
    }

    @Override
    public boolean verifyGateway() {
        return !Strings.isNullOrEmpty(gateway);
    }

    @Override
    public boolean supportsFeature(Feature feature) {
        switch (feature) {
            case DOWNLOAD_USER_SKIN:
            case UPLOAD_USER_SKIN:
            case DELETE_USER_SKIN:
                return true;
            default: return false;
        }
    }

    @Override
    public String toString() {
        return new IndentedToStringStyle.Builder(this)
                .append("address", address)
                .append("gateway", gateway)
                .build();
    }

}
