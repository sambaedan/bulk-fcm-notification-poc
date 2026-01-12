package com.fcm.fcm_demo.util;

import java.util.ArrayList;
import java.util.List;

public class ChunkingUtils {
    public static <T> List<List<T>> chunks(List<T> list, int size) {
        List<List<T>> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            result.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return result;
    }
}