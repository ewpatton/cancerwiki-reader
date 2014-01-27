package edu.rpi.tw.mobilehealth.wiki;

import java.util.HashMap;

/**
 * WikiEntry provides access to different attributes about a page on the wiki
 * and provides access to the various fields used in wiki templates. While the
 * implementation is writeable, calling methods such as
 * {@link #put(String, String)} will not cause any changes to the database, but
 * they will cause local modifications, making it useful for passing parameters
 * between algorithms.
 * @author ewpatton
 */
public class WikiEntry extends HashMap<String, String> {
    private static final long serialVersionUID = 6896858695079717475L;

    private final WikiEntryType type;

    /**
     * Constructs a new wiki entry of the given type (which cannot be changed).
     * The constructor will be called by {@link WikiReader#iterator()} during
     * iteration.
     * @param type
     */
    WikiEntry(WikiEntryType type) {
        this.type = type;
    }

    /**
     * Gets the declared template for this page. For available types, see
     * {@link WikiEntryType}
     * @return
     */
    public WikiEntryType getType() {
        return type;
    }
}
