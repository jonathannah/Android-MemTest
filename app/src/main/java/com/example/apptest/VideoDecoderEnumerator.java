package com.example.apptest;

import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaCodec;

import java.util.ArrayList;
import java.util.List;

public class VideoDecoderEnumerator {
    public static class DecoderSet
    {
        public final String mimeType;
        public final List<String> decoders;

        public DecoderSet(String mimeType, List<String> decoders){
            this.mimeType = mimeType;
            this.decoders = decoders;
        }
    }

    public static List<DecoderSet> getDecoders() {
        List<DecoderSet> ret = new ArrayList<>();

        String[] mimeTypes = {
                "video/avc",  // H.264
                "video/hevc", // H.265
                "video/x-vnd.on2.vp9", // VP9
                "video/av01", // AV1
                "video/vvc"   // VVC (H.266)
        };

        MediaCodecList codecList = new MediaCodecList(MediaCodecList.ALL_CODECS);
        MediaCodecInfo[] codecInfos = codecList.getCodecInfos();

        for (String mimeType : mimeTypes) {
            List<String> setData = new ArrayList<>();

            for (MediaCodecInfo codecInfo : codecInfos) {
                if (!codecInfo.isEncoder()) { // Only list decoders
                    try {
                        MediaCodecInfo.CodecCapabilities capabilities = codecInfo.getCapabilitiesForType(mimeType);
                        if (capabilities != null) {
                            setData.add(codecInfo.getName());
                        }
                    } catch (IllegalArgumentException ignored) {
                        // Codec does not support this MIME type
                    }
                }
            }

            if(!setData.isEmpty()) {
                ret.add(new DecoderSet(mimeType, setData));
            }
        }

        return ret;
    }

    public static String decodersToString(List<DecoderSet> decoderList) {
        StringBuilder output = new StringBuilder();

        for (DecoderSet set : decoderList) {
            output.append("Decoders for: ").append(set.mimeType).append("\n");
            for (String decoder : set.decoders) {
                output.append("  ").append(decoder).append("\n");
            }
        }

        return output.toString();
    }

    public static String getDecodersString()
    {
        List<DecoderSet> decoderList = getDecoders();
        return decodersToString(decoderList);
    }
}

