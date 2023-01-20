
import java.io.IOException;
import java.util.logging.Logger;


public class Main {
    public static final Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
    public static void main(String[] args) {
        if (args.length != 1) {
            logger.info("bad parameters");
            return;
        }
        try {
            int port = Integer.parseInt(args[0]);

            SOCKSProxy proxy = new SOCKSProxy(port);
            logger.info("Proxy ready");
            proxy.start();
        } catch (Exception e) {
           logger.info("Chto-to poshlo ne tak.... " + e.getMessage());
        }


    }
}
