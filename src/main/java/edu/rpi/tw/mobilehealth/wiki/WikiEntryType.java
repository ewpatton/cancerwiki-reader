package edu.rpi.tw.mobilehealth.wiki;

/**
 * Identifies a category/template defined in the Semantic MediaWiki instance.
 * A {@link WikiEntry} has at most one type. Updates to this enum must also be
 * reflected in {@link WikiReader} and WikiReader.sql.
 * @author ewpatton
 */
public enum WikiEntryType {
    /**
     * An Experience, as declared by the Exprience template on the wiki.
     * {@link https://mobilehealth.tw.rpi.edu/wiki/Template:Experience}
     */
    EXPERIENCE,
    /**
     * A Recommendation, as declared by the Recommendation template on the wiki.
     * {@link https://mobilehealth.tw.rpi.edu/wiki/Template:Recommendation}
     */
    RECOMMENDATION
}
