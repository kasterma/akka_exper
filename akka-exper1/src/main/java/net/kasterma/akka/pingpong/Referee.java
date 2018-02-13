package net.kasterma.akka.pingpong;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Referee in the simulated game of ping pong.
 */
public class Referee extends AbstractActor {
    private Map<ActorRef, Boolean> playing;
    private Integer heats;
    private final Random random = new Random();
    private Integer totalHeats;

    static Props props() {
        return Props.create(Referee.class);
    }

    @Getter
    @AllArgsConstructor
    static class StartGame {
        private final int players;
        private final int heats;
    }

    @Getter
    @AllArgsConstructor
    static class Done {
        private final ActorRef opponent;
    }

    LoggingAdapter log = Logging.getLogger(getContext().getSystem(), this);

    void newGame() throws Exception {
        List<ActorRef> availablePlayers = playing.entrySet().stream()
                .filter(e -> !e.getValue())
                .map(Map.Entry::getKey).collect(Collectors.toList());
        if (availablePlayers.size() < 2) {
            log.info("not enough players");
            throw new Exception("not enough players");
        }
        Integer idx1 = random.nextInt(availablePlayers.size());
        Integer idx2 = random.nextInt(availablePlayers.size() - 1);
        idx2 = idx2 >= idx1 ? idx2 + 1 : idx2;
        ActorRef player1 = availablePlayers.get(idx1);
        ActorRef player2 = availablePlayers.get(idx2);
        log.info("starting game {} vs {}", player1, player2);
        player1.tell(new Player.Play(player2), getSelf());
    }

    void startGame(StartGame sg) throws Exception {
        log.info("Starting {}", sg.toString());
        playing = new HashMap<>();
        for (Integer idx = 0; idx < sg.getPlayers(); idx++) {
            playing.put(getContext().actorOf(Player.props("player-" + idx), "player-" + idx), false);
        }
        heats = 1;
        totalHeats = sg.getHeats();
        newGame();
    }

    void done(Done d) throws Exception {
        log.info("done called");
        if (heats < totalHeats) {
            heats++;
            log.info("new game");
            newGame();
        }
        log.info("done");
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(StartGame.class, this::startGame)
                .match(Done.class, this::done)
                .build();
    }
}
