package edu.rpi.tw.mobilehealth.wiki;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;

import org.apache.commons.io.IOUtils;

public class WikiReader implements Iterable<WikiEntry> {
    private static final String BASE_SQL;
    private static final String SQL_FILE =
            "/edu/rpi/tw/mobilehealth/wiki/WikiReader.sql";
    private static final String READ_ONLY_ERROR =
            "Read only iterator. Remove is not supported";
    private static final String READ_EXCEPTION =
            "Could not read wiki data.";
    private static final SimpleDateFormat wikiDateFormat =
            new SimpleDateFormat("yyyyMMddHHmmss");

    private Connection conn = null;
    private String timestamp = null;

    private static String readFromStream(InputStream is) throws IOException {
        StringWriter buffer = new StringWriter();
        IOUtils.copy(is, buffer, Charset.forName("UTF-8"));
        return buffer.toString();
    }

    static {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            InputStream is = WikiReader.class.getResourceAsStream(SQL_FILE);
            BASE_SQL = readFromStream(is);
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError(e);
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public WikiReader(String host, String db, String username, String password, Calendar minTime) {
        try {
            if ( minTime != null ) {
                timestamp = wikiDateFormat.format( minTime.getTime() );
            }
            conn = DriverManager.getConnection( "jdbc:mysql://" + host + "/" +
                    db + "?user=" + username + "&password=" + password );
        } catch (SQLException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public Iterator<WikiEntry> iterator() {
        try {
            return new WikiReaderIterator(timestamp);
        } catch (SQLException e) {
            throw new IllegalStateException( READ_EXCEPTION, e);
        }
    }

    class WikiReaderIterator implements Iterator<WikiEntry> {

        private ResultSet results;

        WikiReaderIterator(String time) throws SQLException {
            Statement stmt = conn.createStatement();
            results = stmt.executeQuery( time == null ? BASE_SQL :
                BASE_SQL + " and revision.rev_timestamp > '" + time + "'" );
        }

        public boolean hasNext() {
            try {
                return !(results.isLast() || results.isAfterLast());
            } catch (SQLException e) {
                throw new IllegalStateException( READ_EXCEPTION, e );
            }
        }

        public WikiEntry next() {
            try {
                if ( !results.next() ) {
                    throw new IllegalStateException();
                }
                return processResult();
            } catch (SQLException e) {
                throw new IllegalStateException( READ_EXCEPTION, e );
            } catch (IOException e) {
                throw new IllegalStateException( READ_EXCEPTION, e );
            }
        }

        public void remove() {
            throw new UnsupportedOperationException( READ_ONLY_ERROR );
        }

        protected WikiEntry processResult() throws SQLException, IOException {
            int pageId = results.getInt( "page_id" );
            int pageNamespace = results.getInt( "page_namespace" );
            String pageTitle = results.getString( "page_title" );
            String revTimestamp = readFromStream(
                    results.getBlob( "rev_timestamp" ).getBinaryStream() );
            String text = readFromStream(
                    results.getBlob( "old_text" ).getBinaryStream() );
            WikiEntry entry = null;
            if ( text.contains( "{{Recommendation") ) {
                entry = new WikiEntry( WikiEntryType.RECOMMENDATION );
            } else if ( text.contains( "{{Experience") ) {
                entry = new WikiEntry( WikiEntryType.EXPERIENCE );
            } else {
                throw new IllegalStateException( "Cannot determine entry type." );
            }
            entry.put( "id", Integer.toString( pageId ) );
            entry.put( "namespace", Integer.toString( pageNamespace ) );
            entry.put( "title", pageTitle );
            entry.put( "timestamp", revTimestamp );
            entry.put( "text", text );
            return entry;
        }
    }
}
