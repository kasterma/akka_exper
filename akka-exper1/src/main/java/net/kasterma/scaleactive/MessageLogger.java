package net.kasterma.scaleactive;

import akka.actor.AbstractActorWithTimers;
import akka.actor.Props;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import scala.concurrent.duration.FiniteDuration;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Logs incoming messages, and gives occasional metrics about the received
 * messages.  These messages indicate Success or Failure, with for each an
 * id of the entity.
 */
@Slf4j
final class MessageLogger extends AbstractActorWithTimers {
    private final Map<String, Integer> successes = new HashMap<>();
    private final Map<String, Integer> failures = new HashMap<>();

    static Props props() {
        return Props.create(MessageLogger.class);
    }

    public MessageLogger() {
        getTimers()
                .startPeriodicTimer("showmetrics",
                        new ShowMetrics(),
                        FiniteDuration.apply(10, TimeUnit.SECONDS));
    }

    /**
     * Report of success of operation by Person with given id.
     */
    @Data
    @AllArgsConstructor
    static class Success {
        String id;
    }

    /**
     * Record the success.
     *
     * @param s success message containing the id.
     */
    private void success(Success s) {
        log.info("success {}", s.getId());
        successes.merge(s.getId(), 1, Integer::sum);
    }

    /**
     * Report of failure of operation by Person with given id.
     */
    @Data
    @AllArgsConstructor
    static class Failure {
        String id;
    }

    /**
     * Record the failure.
     * @param f failure message containing the id.
     */
    private void failure(Failure f) {
        log.info("failure {}", f.getId());
        failures.merge(f.getId(), 1, Integer::sum);
    }

    /**
     * Timer generated message to show the metrics.
     */
    private static class ShowMetrics {
    }

    /**
     * Log the metrics.
     */
    private void showMetrics() {
        log.info("successes {}", successes.toString());
        log.info("failures {}", failures.toString());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Success.class, this::success)
                .match(Failure.class, this::failure)
                .match(ShowMetrics.class, m -> showMetrics())
                .build();
    }
}
