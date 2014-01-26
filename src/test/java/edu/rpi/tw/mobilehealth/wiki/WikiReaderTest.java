package edu.rpi.tw.mobilehealth.wiki;

import java.io.IOException;
import java.util.Properties;

import org.junit.BeforeClass;
import org.junit.Test;

import junit.framework.TestCase;

public class WikiReaderTest extends TestCase {

    private static final Properties properties = new Properties();
    private static final String CONFIG =
            "/edu/rpi/tw/mobilehealth/wiki/test.properties";

    @BeforeClass
    public static void onlyOnce() {
        try {
            properties.load( WikiReaderTest.class.getResourceAsStream( CONFIG ) );
        } catch (IOException e) {
            throw new ExceptionInInitializerError( e );
        }
    }

    @Test
    public void testWikiReader() {
        onlyOnce();
        WikiReader reader = new WikiReader( properties.getProperty( "host" ),
                properties.getProperty( "db" ),
                properties.getProperty( "user" ),
                properties.getProperty( "password" ), null );
        int count = 0;
        for ( WikiEntry entry : reader ) {
            System.out.println( "type = " + entry.getType() + "   title = " +
                    entry.get( "title" ) );
            count++;
        }
        assertTrue( count > 0 );
    }
}
