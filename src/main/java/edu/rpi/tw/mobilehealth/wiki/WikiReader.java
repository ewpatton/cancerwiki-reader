package edu.rpi.tw.mobilehealth.wiki;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;

import org.apache.commons.io.IOUtils;

/**
 * WikiReader reads pages from a Semantic MediaWiki in the default namespace,
 * optionally reading only those entries that have been changed since a specific
 * date and time.
 *
 * Example:
 * <code>
 * WikiReader wiki = new WikiReader( "localhost:3306", "wiki", "root", "", null );
 * for ( WikiEntry entry : wiki ) {
 *   System.out.println( "Title: " + entry.get( "title" );
 *   System.out.println( "Last Modified: " + entry.get( "timestamp" );
 *   System.out.println( "Content: ");
 *   System.out.println( entry.get( "text" ) );
 *   System.out.println();
 * }
 * wiki.close();
 * </code>
 * @author ewpatton
 */
public class WikiReader implements Iterable<WikiEntry>, Closeable {
    private static final String BASE_SQL;
    private static final String SQL_FILE =
            "/edu/rpi/tw/mobilehealth/wiki/WikiReader.sql";
    private static final String READ_ONLY_ERROR =
            "Read only iterator. Remove is not supported";
    private static final String READ_EXCEPTION =
            "Could not read wiki data.";
    private final SimpleDateFormat wikiDateFormat =
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

    /**
     * Constructs a new WikiReader that reads from the given host's database.
     * NB: this does not support multiple wikis in the same database with
     * differing prefixes. It is highly recommended (although not required)
     * that one calls {@link #close()} after reading entries to free up SQL
     * resources. Otherwise, these resources will not be cleaned up until
     * the finalizer runs for {@link Connection}.
     * @param host Host and port to connect to the database on, e\.g\.
     * localhost:3306
     * @param db Name of the database containing the wiki tables.
     * @param username User account with SELECT privileges on tables within the
     * database.
     * @param password Password for the given user account.
     * @param minTime An optional {@link Calendar} that limits the selection of
     * articles only to those that have been created or changed since the
     * specified time. If no limit is desired, pass null.
     */
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

    @Override
    public Iterator<WikiEntry> iterator() {
        try {
            return new WikiReaderIterator(timestamp);
        } catch (SQLException e) {
            throw new IllegalStateException( READ_EXCEPTION, e);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            if ( conn != null ) {
                conn.close();
                conn = null;
            }
        } catch(SQLException e) {
            throw new IOException( "Unable to close database", e );
        }
    }

    class WikiReaderIterator implements Iterator<WikiEntry> {

        private PreparedStatement stmt = null;
        private ResultSet results = null;
        private boolean first = true;

        WikiReaderIterator(String time) throws SQLException {
            try {
                stmt = conn.prepareStatement( time == null ? BASE_SQL :
                    BASE_SQL + " and revision.rev_timestamp > ?" );
                if ( time != null ) {
                    stmt.setString( 1, time );
                }
                System.out.println( stmt.toString() );
                results = stmt.executeQuery();
            } catch(SQLException e) {
                if ( stmt != null ) {
                    stmt.close();
                    stmt = null;
                }
                throw e;
            }
        }

        private boolean isValid() {
            return stmt != null && results != null;
        }

        private boolean isLast() throws SQLException {
            if ( first && !results.isBeforeFirst() ) {
                return false;
            }
            return results.isLast() || results.isAfterLast();
        }

        private void close() {
            try {
                stmt.close();
            } catch(SQLException e) {
                throw new IllegalStateException(
                        "Exception attempting to close SQL Statement", e );
            }
        }

        @Override
        public boolean hasNext() {
            try {
                if ( !isValid() ) {
                    return false;
                }
                if ( isLast() ) {
                    close();
                    return false;
                }
                return true;
            } catch (SQLException e) {
                throw new IllegalStateException( READ_EXCEPTION, e );
            }
        }

        @Override
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

        @Override
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
            // TODO process text and extract semantic properties.
            return entry;
        }
    }

}
