package model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * One row of the local copy: a price, and when it became true, and when we heard.
 *
 * @param sku       what it is a price for
 * @param amount    the price itself
 * @param changedAt when the catalogue service says it changed. Comes off the event
 * @param appliedAt when <i>we</i> wrote it down. The gap between the two is Probe A
 */
public record Price(String sku, BigDecimal amount, OffsetDateTime changedAt, OffsetDateTime appliedAt) {
}
