package io.pillopl.consumption;

/**
 * Tells the operations team that a suspect consumption record waits for its review. The mail implementation comes
 * with the mail templates (monitoring and mails); without one the write service only logs.
 */
public interface SuspectRecordNotifier {

    void suspectRecordCreated(ConsumptionRecord record);
}
