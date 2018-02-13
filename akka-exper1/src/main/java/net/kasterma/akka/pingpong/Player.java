package net.kasterma.akka.pingpong;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Random;

/**
 * Player in a simulated game of ping-pong.
 *
 */
public class Player extends AbstractActor
{
    private final String id;
    private final Random random;

    static Props props(String id) {
        return Props.create(Player.class, id);
    }

    private Player(String id) {
        this.id = id;
        this.random = new Random();
    }

    static class Ping {}

    static class Pong {}

    @AllArgsConstructor
    static class Play {
        ActorRef opponent;
    }

    private LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    void ping(Ping p) {
        log.info("{} got ping", id);
        if (random.nextFloat() < 0.3) {
            log.info("{} done playing", id);
            log.info("telling done to {}", getContext().getParent());
            getContext().getParent().tell(new Referee.Done(getSender()), getSelf());
        } else {
            getSender().tell(new Pong(), getSelf());
        }
    }

    void pong(Pong p) {
        log.info("{} got pong", id);
        getSender().tell(new Ping(), getSelf());
    }

    void play(Play p) {
        p.opponent.tell(new Ping(), getSelf());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(Ping.class, this::ping)
                .match(Pong.class, this::pong)
                .match(Play.class, this::play)
                .build();
    }
}
