package localcopy;

import model.IPriceStore;
import model.Price;
import model.PriceChanged;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * The local copy of the catalogue's prices, in a SQLite file.
 * <p>
 * <b>A file is the cheapest durable store there is</b>, and durable is the only property the
 * exercise actually needs: the price consumer that fills this runs in its own process, and Probes
 * B, C and D all turn on what is still here after that process dies.
 * <p>
 * It implements {@link IPriceStore}, which Model declares, so the domain reads prices without
 * knowing any of this exists. It also exposes {@link #apply(PriceChanged)}, which Model does
 * <i>not</i> know about: reading the copy is the domain's business, and maintaining it is the price
 * consumer's.
 */
public final class SqlitePriceStore implements IPriceStore, AutoCloseable {
    /** Sits next to whatever you ran, so all four processes share one copy. */
    public static final String DEFAULT_PATH = "prices.db";

    /**
     * Timestamps go in as fixed-width UTC text, because {@code MAX(changed_at)} on a TEXT column is
     * a string comparison and a string comparison is only a date comparison if every value is the
     * same shape. Mix offsets, or let the fractional seconds vary in length, and the newest row
     * stops being the largest string -- which would quietly break the one number Probe C is about.
     */
    private static final DateTimeFormatter STORED =
            DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSSSS'Z'").withZone(ZoneOffset.UTC);

    private final Connection connection;
    private final String path;

    private SqlitePriceStore(Connection connection, String path) {
        this.connection = connection;
        this.path = path;
    }

    public static SqlitePriceStore open() {
        return open(DEFAULT_PATH);
    }

    public static SqlitePriceStore open(String path) {
        try {
            Connection connection = DriverManager.getConnection("jdbc:sqlite:" + path);

            try (Statement statement = connection.createStatement()) {
                // WAL, because a reader and a writer are two different processes here and the
                // default journal would have them locking each other out. This is a real decision
                // and not boilerplate: "my local copy is a file" stops being free the moment two
                // processes want it at once.
                statement.execute("PRAGMA journal_mode=WAL");
                statement.execute("PRAGMA busy_timeout=5000");

                // The price is TEXT rather than REAL on purpose. A price is a decimal and SQLite's
                // REAL is a double, and 9.99 is not a double. Storing money in a float is a bug
                // that takes months to surface and this is the line that prevents it.
                statement.execute("""
                        CREATE TABLE IF NOT EXISTS prices (
                            sku        TEXT PRIMARY KEY,
                            price      TEXT NOT NULL,
                            changed_at TEXT NOT NULL,
                            applied_at TEXT NOT NULL
                        )
                        """);
            }

            return new SqlitePriceStore(connection, path);
        } catch (SQLException e) {
            throw new IllegalStateException("could not open the local copy at " + path, e);
        }
    }

    /** Where the copy lives, for the line the Receiver prints at startup. */
    public String path() {
        return path;
    }

    @Override
    public Price lookup(String sku) {
        String sql = "SELECT price, changed_at, applied_at FROM prices WHERE sku = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, sku);
            try (ResultSet results = statement.executeQuery()) {
                if (!results.next()) {
                    return null;
                }
                return new Price(
                        sku,
                        new BigDecimal(results.getString(1)),
                        OffsetDateTime.parse(results.getString(2)),
                        OffsetDateTime.parse(results.getString(3)));
            }
        } catch (SQLException e) {
            throw new IllegalStateException("could not read the price for " + sku, e);
        }
    }

    @Override
    public int count() {
        try (Statement statement = connection.createStatement();
             ResultSet results = statement.executeQuery("SELECT COUNT(*) FROM prices")) {
            return results.next() ? results.getInt(1) : 0;
        } catch (SQLException e) {
            throw new IllegalStateException("could not count the local copy", e);
        }
    }

    @Override
    public OffsetDateTime newestChangedAt() {
        try (Statement statement = connection.createStatement();
             ResultSet results = statement.executeQuery("SELECT MAX(changed_at) FROM prices")) {
            String newest = results.next() ? results.getString(1) : null;
            return newest == null ? null : OffsetDateTime.parse(newest);
        } catch (SQLException e) {
            throw new IllegalStateException("could not date the local copy", e);
        }
    }

    /**
     * Write a price change into the copy. <b>Last writer wins</b>, which is only safe because
     * {@link PriceChanged} is a snapshot -- run this twice with the same event and the row ends up
     * the same. That is Probe D's whole payout, and it was decided in step 1.
     *
     * @return when the row was written, for Probe A's arithmetic
     */
    public OffsetDateTime apply(PriceChanged event) {
        OffsetDateTime appliedAt = OffsetDateTime.now();

        String sql = """
                INSERT INTO prices (sku, price, changed_at, applied_at)
                VALUES (?, ?, ?, ?)
                ON CONFLICT(sku) DO UPDATE SET
                    price      = excluded.price,
                    changed_at = excluded.changed_at,
                    applied_at = excluded.applied_at
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, event.sku());
            statement.setString(2, event.price().toPlainString());
            statement.setString(3, STORED.format(event.changedAt()));
            statement.setString(4, STORED.format(appliedAt));
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("could not apply a price for " + event.sku(), e);
        }

        return appliedAt;
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            throw new IllegalStateException("could not close the local copy", e);
        }
    }
}
