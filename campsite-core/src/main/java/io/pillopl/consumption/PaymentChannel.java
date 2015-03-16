package io.pillopl.consumption;

/**
 * How the supply of a consumption record is paid: with a camping card (billed to the guest's account), with a house
 * card of the campsite, or through the QR payment in the app.
 */
public enum PaymentChannel {
    CARD,
    HOUSE_CARD,
    QR_APP;

    public static PaymentChannel of(boolean houseCard, boolean campingCard) {
        if (houseCard) {
            return HOUSE_CARD;
        }
        return campingCard ? CARD : QR_APP;
    }
}
