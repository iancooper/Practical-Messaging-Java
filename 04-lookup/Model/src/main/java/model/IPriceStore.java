package model;

import java.time.OffsetDateTime;

/**
 * The local copy of somebody else's reference data, as the domain sees it.
 *
 * <hr>
 * <b>This interface is declared by Model, and that is the whole point of it.</b>
 * <p>
 * {@code Model/pom.xml} depends on SimpleMessaging and a JSON library and nothing else. It does
 * not know that the copy is SQLite, that it is a file, or that a separate process fills it --
 * {@code LocalCopy/} knows all three, {@code Receiver/} puts the two together, and the domain
 * names none of it.
 * <p>
 * It is exercise 1's fix arriving a second time, against a storage technology instead of a broker,
 * and the seam did not have to change to cope. Add {@code org.xerial:sqlite-jdbc} to
 * {@code Model/pom.xml} and you have undone it.
 * <hr>
 *
 * Notice that it answers three different questions, not one. "Have you a price for this?" is the
 * obvious one. "Have you any prices at all?" separates <i>the SKU is unknown</i> from <i>we are not
 * ready yet</i>, which is Probe C. "How old is the newest thing you have?" is the only question
 * that can tell a current copy from a stale one, and it is the one nothing asks often enough.
 * <p>
 * <b>A lookup that can only say yes or no cannot be operated.</b> That is a design decision you
 * make when you write the interface, long before anybody needs the answer.
 */
public interface IPriceStore {
    /** The price for this SKU, or null if the local copy does not have one. */
    Price lookup(String sku);

    /**
     * How many prices the copy holds. <b>Zero means "I have never been filled"</b>, which is not
     * the same fact as "that SKU is not a thing" and must not produce the same behaviour.
     */
    int count();

    /**
     * When the catalogue last changed something we know about, or null if the copy is empty.
     * <p>
     * <b>This is the number Probe C is really about.</b> A copy that cannot say how old it is
     * cannot be monitored, and a copy that cannot be monitored is one you find out about from a
     * customer.
     */
    OffsetDateTime newestChangedAt();
}
