package io.pillopl.card;

import io.pillopl.config.SystemPropertyService;
import io.pillopl.infrastructure.Pedestal;
import io.pillopl.mail.MailService;
import io.pillopl.persistence.dao.PedestalDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Pushes the current local card lists to all pedestals with automated list updates, on a thread pool sized from the
 * configuration (the pedestals answer slowly over the mobile network).
 */
@Service
public class LocalCardListDistributionService {

    private static final Logger LOG = LoggerFactory.getLogger(LocalCardListDistributionService.class);

    private final PedestalDao pedestalDao;

    private final LocalCardListService localCardListService;

    private final LocalCardListSender localCardListSender;

    private final SystemPropertyService systemPropertyService;

    private final MailService mailService;

    public LocalCardListDistributionService(PedestalDao pedestalDao, LocalCardListService localCardListService,
                                            LocalCardListSender localCardListSender,
                                            SystemPropertyService systemPropertyService, MailService mailService) {
        this.pedestalDao = pedestalDao;
        this.localCardListService = localCardListService;
        this.localCardListSender = localCardListSender;
        this.systemPropertyService = systemPropertyService;
        this.mailService = mailService;
    }

    private class LocalCardListUpdater implements Runnable {

        private final Pedestal pedestal;

        private final long waitAfterDeleteInMillis;

        private final AtomicInteger sent;

        private final AtomicInteger failed;

        LocalCardListUpdater(Pedestal pedestal, long waitAfterDeleteInMillis, AtomicInteger sent,
                             AtomicInteger failed) {
            this.pedestal = pedestal;
            this.waitAfterDeleteInMillis = waitAfterDeleteInMillis;
            this.sent = sent;
            this.failed = failed;
        }

        @Override
        public void run() {
            try {
                LOG.debug("Running local card list update for pedestal " + pedestal.getCode());

                // delete the old list
                LocalCardListSendResult deleted = localCardListSender.deleteLocalList(pedestal);
                if (!deleted.success()) {
                    LOG.warn(String.format("Local card list of %s not deleted: %s", pedestal, deleted.errorMessage()));
                }
                // the controller needs a moment after the delete
                if (waitAfterDeleteInMillis > 0) {
                    Thread.sleep(waitAfterDeleteInMillis);
                }
                // write the list to the pedestal
                if (localCardListService.sendFull(pedestal.getCode(), true)) {
                    sent.incrementAndGet();
                }

                LOG.debug("Finished local card list update for pedestal " + pedestal.getCode());
            } catch (InterruptedException ie) {
                failed.incrementAndGet();
                Thread.currentThread().interrupt();
            } catch (Exception ex) {
                failed.incrementAndGet();
                LOG.error("An exception occurred updating the local card list of pedestal " + pedestal.getCode(), ex);
            }
        }
    }

    /**
     * Distributes the current lists of cards to the pedestals.
     */
    public LocalCardListDistributionResult distribute() {
        LOG.info("Execution of LocalCardListDistributionJob started.");

        int poolSize = systemPropertyService.getLocalCardListDistributionConcurrentThreads();
        int maxMinutes = systemPropertyService.getLocalCardListDistributionMaximumDurationInMinutes();
        long waitAfterDelete = systemPropertyService.getLocalCardListDistributionWaitTimeAfterDeleteInMillis();
        ExecutorService threadPool = Executors.newFixedThreadPool(Math.max(1, poolSize));
        LOG.info("local card list distribution runs on " + poolSize + " threads.");

        AtomicInteger sent = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();

        // find all pedestals that need a local card list
        List<Pedestal> pedestals = pedestalDao.findPedestalsForLocalCardListDistribution();
        // add them all to the pool
        for (Pedestal pedestal : pedestals) {
            threadPool.execute(new LocalCardListUpdater(pedestal, waitAfterDelete, sent, failed));
        }
        // wait for the shutdown
        threadPool.shutdown();
        boolean completed = false;
        int cycles = 0;
        try {
            while (!(completed = threadPool.awaitTermination(1, TimeUnit.MINUTES)) && ++cycles < maxMinutes) {
                LOG.info(String.format("Local card list distribution still running after %d minutes", cycles));
            }
        } catch (InterruptedException ex) {
            LOG.error("Local card list distribution stopped because the threads got interrupted.", ex);
            threadPool.shutdownNow();
            Thread.currentThread().interrupt();
        }
        if (!completed) {
            LOG.error("Local card list distribution was stopped after " + maxMinutes + " minutes."
                    + " Please check for mobile network problems or adjust thread pool or timeout.");
            threadPool.shutdownNow();
            // tell the service team by mail
            mailService.sendErrorMail("Local card list distribution error", "Local card list distribution was"
                    + " stopped after " + maxMinutes + " minutes (" + sent.get() + " of " + pedestals.size()
                    + " pedestals sent). Please check for mobile network problems or adjust thread pool or timeout.");
        } else if (failed.get() > 0) {
            // tell the service team by mail
            mailService.sendErrorMail("Local card list distribution error", failed.get() + " of "
                    + pedestals.size() + " pedestals did not get their local card list, see the log for the codes.");
        }

        LOG.info("Execution of LocalCardListDistributionJob stopped.");
        return new LocalCardListDistributionResult(pedestals.size(), sent.get(), failed.get(), completed);
    }
}
