package com.nukkitx.proxypass.network.bedrock.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nukkitx.proxypass.ProxyPass;
import lombok.Value;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class BlockPaletteUtils {
    /*
    public static void convertToJson(ProxyPass proxy, List<CompoundTag> tags) {

        List<Entry> palette = new ArrayList<>(tags.size());

        for (CompoundTag tag : tags) {
            int id = tag.getAsShort("id");
            CompoundTag blockTag = tag.getAsCompound("block");
            String name = blockTag.getAsString("name");

            Map<String, BlockState> states = new LinkedHashMap<>();

            blockTag.getAsCompound("states").getValue().forEach((key, value) -> {
                states.put(key, new BlockState(value.getValue(), TagType.byClass(value.getClass()).getId()));
            });

            Integer meta = null;
            if (tag.contains("meta")) {
                meta = (int) tag.getAsShort("meta");
            }
            palette.add(new Entry(id, meta, name, states));
        }
        palette.sort((o1, o2) -> {
            int compare = Integer.compare(o1.id, o2.id);
            if (compare == 0) {
                for (Map.Entry<String, BlockState> entry : o1.states.entrySet()) {
                    BlockState bs2 = o2.states.get(entry.getKey());
                    compare = ((Comparable) entry.getValue().val).compareTo(bs2.val);
                    if (compare != 0) {
                        break;
                    }
                }
            }
            return compare;
        });


        proxy.saveJson("runtime_block_states.json", palette);
    }

    @Value
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private static class Entry {
        private final int id;
        private final Integer meta;
        private final String name;
        private final Map<String, BlockState> states;
    }

    @Value
    private static class BlockState {
        private Object val;
        private int type;
    }
     */
}
