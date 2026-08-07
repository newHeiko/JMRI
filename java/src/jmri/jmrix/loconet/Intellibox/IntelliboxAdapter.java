package jmri.jmrix.loconet.Intellibox;

import java.util.Arrays;
import jmri.ThrottleManager;
import jmri.jmrix.loconet.LnCommandStationType;
import jmri.jmrix.loconet.LnThrottleManager;
import jmri.jmrix.loconet.locobuffer.LocoBufferAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Update the code in jmri.jmrix.loconet.locobuffer so that it operates
 * correctly with the Intellibox on-board serial port.
 * <p>
 * Since this is by definition connected to an Intellibox, the command station
 * prompt has limited choices.
 *
 * @author Alex Shepherd Copyright (C) 2004
 * @author Bob Jacobsen Copyright (C) 2005, 2010
 */
public class IntelliboxAdapter extends LocoBufferAdapter {

    public IntelliboxAdapter() {
        super();

        // define command station options
        options.remove(option2Name);
        options.put(option2Name, new Option(Bundle.getMessage("CommandStationTypeLabel"), commandStationOptions(), false));

        validSpeeds = new String[]{Bundle.getMessage("Baud19200"), Bundle.getMessage("Baud38400"), Bundle.getMessage("Baud115200")};
        validSpeedValues = new int[]{19200, 38400, 115200};
    }

    /**
     * Set up all of the other objects to operate with a LocoBuffer connected to
     * this port.
     */
    @Override
    public void configure() {

        setCommandStationType(getOptionState(option2Name));
        setTurnoutHandling(getOptionState(option3Name));
        {
            int id = 0x0171;
            try {
            id = Integer.parseInt(getOptionState("LocoNetThrottleID").substring(2), 16);
            }
            catch(NumberFormatException ex) {
                log.warn("Cannot parse LocoNetThrottleID {}. Using default of 0x0171.", getOptionState("LocoNetThrottleID"));
                log.warn("Error was: {}", ex.getMessage());
                id = 0x0171;
            }
            finally {
                if(id > 0x3fff || id < 1) {
                    log.warn("Invalid LocoNetThrottleID {}. Using default of 0x0171.", getOptionState("LocoNetThrottleID"));
                    id = 0x0171;
                }
                setOptionState("LocoNetThrottleID", "0x".concat(Integer.toHexString(id)) );
                ThrottleManager tm = this.getSystemConnectionMemo().getThrottleManager();
                if (tm instanceof LnThrottleManager) {
                    ( (LnThrottleManager) tm).setThrottleID(id);
                }
                else {
                    log.warn("Cannot set LoconetThrottleID, only supported on LnThrottleManager and subclasses");
                }
            }
        }
        // connect to a packetizing traffic controller
        IBLnPacketizer packets = new IBLnPacketizer();
        packets.connectPort(this);

        // create memo
        this.getSystemConnectionMemo().setLnTrafficController(packets);
        // do the common manager config
        this.getSystemConnectionMemo().configureCommandStation(commandStationType,
                mTurnoutNoRetry, mTurnoutExtraSpace, mTranspondingAvailable, mInterrogateAtStart ,false);
        this.getSystemConnectionMemo().configureManagers();

        // start operation
        packets.startThreads();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] validBaudRates() {
        return Arrays.copyOf(validSpeeds, validSpeeds.length);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int[] validBaudNumbers() {
        return Arrays.copyOf(validSpeedValues, validSpeedValues.length);
    }

    /**
     * Rephrase option 1, so that it doesn't talk about LocoBuffer.
     * @return human readable string, TypeSerial.
     */
    public String option1Name() {
        return Bundle.getMessage("XconnectionUsesLabel", Bundle.getMessage("TypeSerial"));
    }

    /**
     * Provide just one valid command station value.
     * @return single value array with name of COMMAND_STATION_IBX_TYPE_1 .
     */
    public String[] commandStationOptions() {
        String[] retval = {
            LnCommandStationType.COMMAND_STATION_IBX_TYPE_1.getName()
        };
        return retval;
    }

    private static final Logger log = LoggerFactory.getLogger(IntelliboxAdapter.class);

}
