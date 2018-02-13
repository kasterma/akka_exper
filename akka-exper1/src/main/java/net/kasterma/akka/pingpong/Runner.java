package net.kasterma.akka.pingpong;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.*;

/**
 * Entrypoint for the ping ping application.
 */
@Slf4j
public class Runner {
    static CommandLine parseArgs(String[] args) throws ParseException {
        Options opts = new Options();
        Option heats = new Option("h", "heats", true, "Number of heats");
        opts.addOption(heats);
        CommandLineParser parser = new DefaultParser();
        return parser.parse(opts, args);
    }

    public static void main(String[] args) {
        log.info("started");
        Integer heats = 0;
        try {
            CommandLine cmd = parseArgs(args);
            heats = Integer.valueOf(cmd.getOptionValue("heats"));
        } catch (ParseException e) {
            log.error("Can't parse command line: {}", e.toString());
            System.exit(1);
        } catch (NumberFormatException e) {
            log.error("One of the passed arguments that should be a number isn't: {}", e.toString());
        }

        log.info("Starting game with {} heats", heats);
        ActorSystem system = ActorSystem.create("system-bk1");
        ActorRef ref = system.actorOf(Referee.props(), "referee");
        ref.tell(new Referee.StartGame(2, heats), ActorRef.noSender());
    }
}
