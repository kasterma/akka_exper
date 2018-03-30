package net.kasterma.scaleactive;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Random;

/**
 * Site that a Person actor sends Pings to.  Determines if the ping was
 * successfull, and tells the logger about that.
 */
final class Site extends AbstractActor {
    private final ActorRef logger;

    static Props props(ActorRef logger) {
        return Props.create(Site.class, logger);
    }

    public Site(ActorRef logger) {
        this.logger = logger;
    }

    /**
     * Action from the Person on this Site.
     */
    @Data
    @AllArgsConstructor
    static class Ping {
        final String id;
    }

    /**
     * Determing if Ping was a Success, and tell the logger about it.
     *
     * @param p the Ping containing the human readable id of the sender.
     */
    void ping(Ping p) {
        if (new Random().nextFloat() < 0.5) {
            logger.tell(new MessageLogger.Success(p.id), getSelf());
        } else {
            logger.tell(new MessageLogger.Failure(p.id), getSelf());
        }
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Ping.class, this::ping)
                .build();
    }
}
