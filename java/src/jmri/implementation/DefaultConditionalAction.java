package jmri.implementation;

import java.awt.event.ActionListener;
import javax.swing.Timer;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import jmri.Audio;
import jmri.Conditional;
import jmri.ConditionalAction;
import jmri.InstanceManager;
import jmri.Light;
import jmri.Memory;
import jmri.NamedBean;
import jmri.NamedBeanHandle;
import jmri.Route;
import jmri.RouteManager;
import jmri.Sensor;
import jmri.SignalHead;
import jmri.Turnout;
import jmri.jmrit.Sound;
import jmri.jmrit.beantable.LogixTableAction;
import jmri.jmrit.logix.OBlockManager;
import jmri.jmrit.logix.Warrant;
import jmri.jmrit.logix.WarrantManager;

/**
 * The consequent of the antecedent of the conditional proposition. The data for
 * the action to be taken when a Conditional calculates to True
 * <p>
 * This is in the implementations package because of a Swing dependence via the
 * times. Java 1.5 or Java 1.6 might make it possible to break that, which will
 * simplify things.
 *
 * @author Pete Cressman Copyright (C) 2009, 2010, 2011
 * @author Matthew Harris copyright (c) 2009
 */
public class DefaultConditionalAction implements ConditionalAction {

    private int _option = Conditional.ACTION_OPTION_ON_CHANGE_TO_TRUE;
    private Conditional.Action _type = Conditional.Action.NONE;
    private String _deviceName = " ";
    private int _actionData = 0;
    private String _actionString = "";
    private NamedBeanHandle<?> _namedBean = null;

    private Timer _timer = null;
    private ActionListener _listener = null;
    private boolean _timerActive = false;
    private boolean _indirectAction = false;
    private Sound _sound = null;

    static final java.util.ResourceBundle rbx = java.util.ResourceBundle.getBundle("jmri.jmrit.conditional.ConditionalBundle");
    private final jmri.NamedBeanHandleManager nbhm = InstanceManager.getDefault(jmri.NamedBeanHandleManager.class);

    public DefaultConditionalAction() {
    }

    public DefaultConditionalAction( int option, @Nonnull Conditional.Action type,
            String name, int actionData, String actionStr ) {
        _option = option;
        _type = type;
        _deviceName = name;
        _actionData = actionData;
        _actionString = actionStr;

        NamedBean bean = getIndirectBean(_deviceName);
        if (bean == null) {
            bean = getActionBean(_deviceName);
        }
        if (bean != null) {
            _namedBean = nbhm.getNamedBeanHandle(_deviceName, bean);
        } else {
            _namedBean = null;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }

        if (getClass() != obj.getClass()) {
            return false;
        } else {
            DefaultConditionalAction p = (DefaultConditionalAction) obj;
            if ((p.getOption() != this.getOption())
                    || (p.getType() != this.getType())
                    || (p.getActionData() != this.getActionData())) {
                return false;
            }

            if ((p._namedBean == null && this._namedBean != null)
                    || (p._namedBean != null && this._namedBean == null)
                    || (p._namedBean != null && this._namedBean != null && !p._namedBean.equals(this._namedBean))) {
                return false;
            }

            if ((p._deviceName == null && this._deviceName != null)
                    || (p._deviceName != null && this._deviceName == null)
                    || (p._deviceName != null && this._deviceName != null && !p._deviceName.equals(this._deviceName))) {
                return false;
            }

            if ((p._actionString == null && this._actionString != null)
                    || (p._actionString != null && this._actionString == null)
                    || (p._actionString != null && this._actionString != null && !p._actionString.equals(this._actionString))) {
                return false;
            }

        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = _option * 1000 + _type.getIntValue() * 1000 * 1000 + _actionData;
        if (_deviceName != null) {
            hash += _deviceName.hashCode();
        }
        return hash;
    }

    /**
     * If this is an indirect reference, return the Memory bean.
     *
     */
    @CheckForNull
    private Memory getIndirectBean(@CheckForNull String devName) {
        if (devName != null && devName.length() > 0 && devName.charAt(0) == '@') {
            String memName = devName.substring(1);
            Memory m = InstanceManager.memoryManagerInstance().getMemory(memName);
            if (m != null) {
                _indirectAction = true;
                return m;
            }
            log.error("\"{}\" invalid indirect memory name in action {} of type {}", devName, _actionString, _type);
        } else {
            _indirectAction = false;
        }
        return null;
    }

    /**
     * Return the device bean that will do the action.
     *
     */
    @CheckForNull
    private NamedBean getActionBean(@CheckForNull String devName) {
        if (devName == null) {
            return null;
        }
        NamedBean bean = null;
        try {
            switch (_type.getItemType()) {
                case SENSOR:
                    try {
                        bean = InstanceManager.sensorManagerInstance().provideSensor(devName);
                    } catch (IllegalArgumentException e) {
                        bean = null;
                        log.error("invalid sensor name= \"{}\" in conditional action", devName);
                    }
                    break;
                case TURNOUT:
                    try {
                        bean = InstanceManager.turnoutManagerInstance().provideTurnout(devName);
                    } catch (IllegalArgumentException e) {
                        bean = null;
                        log.error("invalid turnout name= \"{}\" in conditional action", devName);
                    }
                    break;
                case MEMORY:
                    try {
                        bean = InstanceManager.memoryManagerInstance().provideMemory(devName);
                    } catch (IllegalArgumentException e) {
                        bean = null;
                        log.error("invalid memory name= \"{}\" in conditional action", devName);
                    }
                    break;
                case LIGHT:
                    try {
                        bean = InstanceManager.lightManagerInstance().getLight(devName);
                    } catch (IllegalArgumentException e) {
                        bean = null;
                        log.error("invalid light name= \"{}\" in conditional action", devName);
                    }
                    break;
                case SIGNALMAST:
                    try {
                        bean = InstanceManager.getDefault(jmri.SignalMastManager.class).provideSignalMast(devName);
                    } catch (IllegalArgumentException e) {
                        bean = null;
                        log.error("invalid signal mast name= \"{}\" in conditional action", devName);
                    }
                    break;
                case SIGNALHEAD:
                    try {
                        bean = InstanceManager.getDefault(jmri.SignalHeadManager.class).getSignalHead(devName);
                    } catch (IllegalArgumentException e) {
                        bean = null;
                        log.error("invalid signal head name= \"{}\" in conditional action", devName);
                    }
                    break;
                case WARRANT:
                    try {
                        bean = InstanceManager.getDefault(WarrantManager.class).getWarrant(devName);
                    } catch (IllegalArgumentException e) {
                        bean = null;
                        log.error("invalid Warrant name= \"{}\" in conditional action", devName);
                    }
                    break;
                case OBLOCK:
                    try {
                        bean = InstanceManager.getDefault(OBlockManager.class).getOBlock(devName);
                    } catch (IllegalArgumentException e) {
                        bean = null;
                        log.error("invalid OBlock name= \"{}\" in conditional action", devName);
                    }
                    break;
                case ENTRYEXIT:
                    try {
                        bean = InstanceManager.getDefault(jmri.jmrit.entryexit.EntryExitPairs.class).getNamedBean(devName);
                    } catch (IllegalArgumentException e) {
                        bean = null;
                        log.error("invalid NX name= \"{}\" in conditional action", devName);
                    }
                    break;
                case LOGIX:
                    try {
                        bean = InstanceManager.getDefault(jmri.LogixManager.class).getLogix(devName);
                    } catch (IllegalArgumentException e) {
                        bean = null;
                        log.error("invalid Logix name= \"{}\" in conditional action", devName);
                    }
                    break;
                default:
                    if (getType() == Conditional.Action.TRIGGER_ROUTE) {
                        try {
                            bean = InstanceManager.getDefault(RouteManager.class).getRoute(devName);
                        } catch (IllegalArgumentException e) {
                            bean = null;
                            log.error("invalid Route name= \"{}\" in conditional action", devName);
                        }
                    }
            }
        } catch (java.lang.NumberFormatException ex) {
            // ingonred, can be considered normal if the logixs are loaded prior to any other beans
        }
        return bean;
    }

    /**
     * The consequent device or element type.
     */
    @Override
    @Nonnull
    public Conditional.Action getType() {
        return _type;
    }

    @Override
    public void setType(@Nonnull Conditional.Action type) {
        _type = type;
    }

    /**
     * Set type from user name for it.
     */
    @Override
    public void setType(String type) {
        _type = stringToActionType(type);
    }

    /**
     * Name of the device or element that is affected.
     */
    @Override
    public String getDeviceName() {
        if (_namedBean != null) {
            return _namedBean.getName();
        }
        /* As we have a trigger for something using the action, then hopefully
         all the managers have been loaded and we can get the bean, which prevented
         the bean from being loaded in the first place */
        setDeviceName(_deviceName);
        return _deviceName;
    }

    @Override
    public void setDeviceName(String deviceName) {
        _deviceName = deviceName;
        NamedBean bean = getIndirectBean(_deviceName);
        if (bean == null) {
            bean = getActionBean(_deviceName);
        }
        if (bean != null) {
            _namedBean = nbhm.getNamedBeanHandle(_deviceName, bean);
        } else {
            _namedBean = null;
        }
    }

    @Override
    @CheckForNull
    public NamedBeanHandle<?> getNamedBean() {
        if (_indirectAction) {
            Memory m = (Memory) (_namedBean.getBean());
            String actionName = (String) m.getValue();
            NamedBean bean = getActionBean(actionName);
            if (bean != null) {
                return nbhm.getNamedBeanHandle(actionName, bean);
            } else {
                return null;
            }
        }
        return _namedBean;
    }

    @Override
    @CheckForNull
    public NamedBean getBean() {
        NamedBean x = beanFromHandle();
        if (x != null) {
            return x;
        }
        setDeviceName(_deviceName); //ReApply name as that will create namedBean, save replicating it here
        return beanFromHandle();
    }

    @CheckForNull
    private NamedBean beanFromHandle(){
        var handle = getNamedBean();
        if (handle != null) {
            return handle.getBean();
        }
        return null;
    }

    /**
     * Options on when action is taken.
     */
    @Override
    public int getOption() {
        return _option;
    }

    @Override
    public void setOption(int option) {
        _option = option;
    }

    /**
     * Integer data for action.
     */
    @Override
    public int getActionData() {
        return _actionData;
    }

    @Override
    public void setActionData(int actionData) {
        _actionData = actionData;
    }

    /**
     * Set action data from I18N name for it.
     */
    @Override
    public void setActionData(String actionData) {
        _actionData = stringToActionData(actionData);
    }

    /**
     * String data for action.
     */
    @Nonnull
    @Override
    public String getActionString() {
        if (_actionString == null) {
            _actionString = getTypeString();
        }
        return _actionString;
    }

    @Override
    public void setActionString(String actionString) {
        _actionString = actionString;
    }

    /*
     * Get timer for delays and other timed events.
     */
    @Override
    @CheckForNull
    public Timer getTimer() {
        return _timer;
    }

    /*
     * Set timer for delays and other timed events.
     */
    @Override
    public void setTimer(Timer timer) {
        _timer = timer;
    }

    @Override
    public boolean isTimerActive() {
        return _timerActive;
    }

    @Override
    public void startTimer() {
        if (_timer != null) {
            _timer.start();
            _timerActive = true;
        } else {
            log.error("timer is null for {} of type {}", _deviceName, getTypeString());
        }
    }

    @Override
    public void stopTimer() {
        if (_timer != null) {
            _timer.stop();
            _timerActive = false;
        }
    }

    /*
     * Set listener for delays and other timed events.
     */
    @Override
    @CheckForNull
    public ActionListener getListener() {
        return _listener;
    }

    /*
     * set listener for delays and other timed events
     */
    @Override
    public void setListener(ActionListener listener) {
        _listener = listener;
    }

    /**
     * Get Sound file.
     */
    @Override
    @CheckForNull
    public Sound getSound() {
        return _sound;
    }

    /**
     * Set the sound file.
     *
     * @param sound the new sound file
     */
    protected void setSound(Sound sound) {
        _sound = sound;
    }

    /*
     * Methods that return user interface strings ****
     */

    /**
     * @return name of this consequent type
     */
    @Nonnull
    @Override
    public String getTypeString() {
        return _type.toString();
    }

    /**
     * @return name of the option for this consequent type
     */
    @Nonnull
    @Override
    public String getOptionString(boolean type) {
        return getOptionString(_option, type);
    }

    @Nonnull
    @Override
    public String getActionDataString() {
        return getActionDataString(_type, _actionData);
    }

    /**
     * Convert Variable Type to Text String.
     *
     * @.param t the Action type
     * @.return a human readable description of the type or an empty String
     *./
    public static String getItemTypeString(int t) {
        switch (t) {
            case Conditional.ITEM_TYPE_SENSOR:
                return (Bundle.getMessage("BeanNameSensor"));
            case Conditional.ITEM_TYPE_TURNOUT:
                return (Bundle.getMessage("BeanNameTurnout"));
            case Conditional.ITEM_TYPE_LIGHT:
                return (Bundle.getMessage("BeanNameLight"));
            case Conditional.ITEM_TYPE_SIGNALHEAD:
                return (Bundle.getMessage("BeanNameSignalHead"));
            case Conditional.ITEM_TYPE_SIGNALMAST:
                return (Bundle.getMessage("BeanNameSignalMast"));
            case Conditional.ITEM_TYPE_MEMORY:
                return (Bundle.getMessage("BeanNameMemory"));
            case Conditional.ITEM_TYPE_LOGIX:
                return (Bundle.getMessage("BeanNameLogix"));
            case Conditional.ITEM_TYPE_WARRANT:
                return (Bundle.getMessage("BeanNameWarrant"));
            case Conditional.ITEM_TYPE_OBLOCK:
                return (Bundle.getMessage("BeanNameOBlock"));
            case Conditional.ITEM_TYPE_ENTRYEXIT:
                return (Bundle.getMessage("BeanNameEntryExit"));
            case Conditional.ITEM_TYPE_CLOCK:
                return (Bundle.getMessage("FastClock"));
            case Conditional.ITEM_TYPE_AUDIO:
                return (Bundle.getMessage("BeanNameAudio"));
            case Conditional.ITEM_TYPE_SCRIPT:
                return (Bundle.getMessage("Script"));
            case Conditional.ITEM_TYPE_OTHER:
                return (rbx.getString("Other"));
            default:
                // fall through
                break;
        }
        return "";
    }
*/
    /**
     * Convert Consequent Type to text String.
     *
     * @.param t the Action type
     * @.return a human readable description of the type or an empty String
     *./
    public static String getActionTypeString(int t) {
        switch (t) {
            case Conditional.ACTION_NONE:
                return (rbx.getString("ActionNone"));
            case Conditional.ACTION_SET_TURNOUT:
                return (rbx.getString("ActionSetTurnout"));
            case Conditional.ACTION_SET_SIGNAL_APPEARANCE:
                return (rbx.getString("ActionSetSignal"));
            case Conditional.ACTION_SET_SIGNAL_HELD:
                return (rbx.getString("ActionSetSignalHeld"));
            case Conditional.ACTION_CLEAR_SIGNAL_HELD:
                return (rbx.getString("ActionClearSignalHeld"));
            case Conditional.ACTION_SET_SIGNAL_DARK:
                return (rbx.getString("ActionSetSignalDark"));
            case Conditional.ACTION_SET_SIGNAL_LIT:
                return (rbx.getString("ActionSetSignalLit"));
            case Conditional.ACTION_TRIGGER_ROUTE:
                return (rbx.getString("ActionTriggerRoute"));
            case Conditional.ACTION_SET_SENSOR:
                return (rbx.getString("ActionSetSensor"));
            case Conditional.ACTION_DELAYED_SENSOR:
                return (rbx.getString("ActionDelayedSensor"));
            case Conditional.ACTION_SET_LIGHT:
                return (rbx.getString("ActionSetLight"));
            case Conditional.ACTION_SET_MEMORY:
                return (rbx.getString("ActionSetMemory"));
            case Conditional.ACTION_ENABLE_LOGIX:
                return (rbx.getString("ActionEnableLogix"));
            case Conditional.ACTION_DISABLE_LOGIX:
                return (rbx.getString("ActionDisableLogix"));
            case Conditional.ACTION_PLAY_SOUND:
                return (rbx.getString("ActionPlaySound"));
            case Conditional.ACTION_RUN_SCRIPT:
                return (rbx.getString("ActionRunScript"));
            case Conditional.ACTION_DELAYED_TURNOUT:
                return (rbx.getString("ActionDelayedTurnout"));
            case Conditional.ACTION_LOCK_TURNOUT:
                return (rbx.getString("ActionTurnoutLock"));
            case Conditional.ACTION_RESET_DELAYED_SENSOR:
                return (rbx.getString("ActionResetDelayedSensor"));
            case Conditional.ACTION_CANCEL_SENSOR_TIMERS:
                return (rbx.getString("ActionCancelSensorTimers"));
            case Conditional.ACTION_RESET_DELAYED_TURNOUT:
                return (rbx.getString("ActionResetDelayedTurnout"));
            case Conditional.ACTION_CANCEL_TURNOUT_TIMERS:
                return (rbx.getString("ActionCancelTurnoutTimers"));
            case Conditional.ACTION_SET_FAST_CLOCK_TIME:
                return (rbx.getString("ActionSetFastClockTime"));
            case Conditional.ACTION_START_FAST_CLOCK:
                return (rbx.getString("ActionStartFastClock"));
            case Conditional.ACTION_STOP_FAST_CLOCK:
                return (rbx.getString("ActionStopFastClock"));
            case Conditional.ACTION_COPY_MEMORY:
                return (rbx.getString("ActionCopyMemory"));
            case Conditional.ACTION_SET_LIGHT_INTENSITY:
                return (rbx.getString("ActionSetLightIntensity"));
            case Conditional.ACTION_SET_LIGHT_TRANSITION_TIME:
                return (rbx.getString("ActionSetLightTransitionTime"));
            case Conditional.ACTION_CONTROL_AUDIO:
                return (rbx.getString("ActionControlAudio"));
            case Conditional.ACTION_JYTHON_COMMAND:
                return (rbx.getString("ActionJythonCommand"));
            case Conditional.ACTION_ALLOCATE_WARRANT_ROUTE:
                return (rbx.getString("ActionAllocateWarrant"));
            case Conditional.ACTION_DEALLOCATE_WARRANT_ROUTE:
                return (rbx.getString("ActionDeallocateWarrant"));
            case Conditional.ACTION_SET_ROUTE_TURNOUTS:
                return (rbx.getString("ActionSetWarrantTurnouts"));
            case Conditional.ACTION_AUTO_RUN_WARRANT:
                return (rbx.getString("ActionAutoRunWarrant"));
            case Conditional.ACTION_MANUAL_RUN_WARRANT:
                return (rbx.getString("ActionManualRunWarrant"));
            case Conditional.ACTION_CONTROL_TRAIN:
                return (rbx.getString("ActionControlTrain"));
            case Conditional.ACTION_SET_TRAIN_ID:
                return (rbx.getString("ActionSetTrainId"));
            case Conditional.ACTION_SET_TRAIN_NAME:
                return (rbx.getString("ActionSetTrainName"));
            case Conditional.ACTION_SET_SIGNALMAST_ASPECT:
                return (rbx.getString("ActionSetSignalMastAspect"));
            case Conditional.ACTION_THROTTLE_FACTOR:
                return (rbx.getString("ActionSetThrottleFactor"));
            case Conditional.ACTION_SET_SIGNALMAST_HELD:
                return (rbx.getString("ActionSetSignalMastHeld"));
            case Conditional.ACTION_CLEAR_SIGNALMAST_HELD:
                return (rbx.getString("ActionClearSignalMastHeld"));
            case Conditional.ACTION_SET_SIGNALMAST_DARK:
                return (rbx.getString("ActionSetSignalMastDark"));
            case Conditional.ACTION_SET_SIGNALMAST_LIT:
                return (rbx.getString("ActionClearSignalMastDark"));
            case Conditional.ACTION_SET_BLOCK_VALUE:
                return (rbx.getString("ActionSetBlockValue"));
            case Conditional.ACTION_SET_BLOCK_ERROR:
                return (rbx.getString("ActionSetBlockError"));
            case Conditional.ACTION_CLEAR_BLOCK_ERROR:
                return (rbx.getString("ActionClearBlockError"));
            case Conditional.ACTION_DEALLOCATE_BLOCK:
                return (rbx.getString("ActionDeallocateBlock"));
            case Conditional.ACTION_SET_BLOCK_OUT_OF_SERVICE:
                return (rbx.getString("ActionSetBlockOutOfService"));
            case Conditional.ACTION_SET_BLOCK_IN_SERVICE:
                return (rbx.getString("ActionBlockInService"));
            case Conditional.ACTION_SET_NXPAIR_ENABLED:
                return (rbx.getString("ActionNXPairEnabled"));
            case Conditional.ACTION_SET_NXPAIR_DISABLED:
                return (rbx.getString("ActionNXPairDisabled"));
            case Conditional.ACTION_SET_NXPAIR_SEGMENT:
                return (rbx.getString("ActionNXPairSegment"));
            default:
                // fall through
                break;
        }
        log.warn("Unexpected parameter to getActionTypeString({})", t);
        return ("");
    }
*/
    /**
     * Convert consequent option to I18N String.
     *
     * @param opt  the option.
     * @param type true if option is a change; false if option is a trigger.
     * @return a human readable description of the option or an empty String.
     */
    @Nonnull
    public static String getOptionString(int opt, boolean type) {
        switch (opt) {
            case Conditional.ACTION_OPTION_ON_CHANGE_TO_TRUE:
                if (type) {
                    return (rbx.getString("OnChangeToTrue"));
                } else {
                    return (rbx.getString("OnTriggerToTrue"));
                }
            case Conditional.ACTION_OPTION_ON_CHANGE_TO_FALSE:
                if (type) {
                    return (rbx.getString("OnChangeToFalse"));
                } else {
                    return (rbx.getString("OnTriggerToFalse"));
                }
            case Conditional.ACTION_OPTION_ON_CHANGE:
                if (type) {
                    return (rbx.getString("OnChange"));
                } else {
                    return (rbx.getString("OnTrigger"));
                }
            default:
                // fall through
                break;
        }
        log.warn("Unexpected parameter to getOptionString({})", opt);
        return "";
    }

    /**
     * Get action type from a String.
     *
     * @param str the string to get the type for
     * @return the type or 0 if str is not a recognized action
     */
    @Nonnull
    public static Conditional.Action stringToActionType(@CheckForNull String str) {
        if (str != null) {
            for (Conditional.Action action : Conditional.Action.values()) {
                if (str.equals(action.toString())) {
                    return action;
                }
            }
        }
        log.warn("Unexpected parameter to stringToActionType({})", str);
        return Conditional.Action.NONE;
    }

    /**
     * Get action Data from an I18N String.
     *
     * @param str the I18N string to get the action data for.
     * @return the action data of -1 is str is not recognized.
     */
    public static int stringToActionData(@Nonnull String str) {
        if (str.equals(Bundle.getMessage("TurnoutStateClosed"))) {
            return Turnout.CLOSED;
        } else if (str.equals(Bundle.getMessage("TurnoutStateThrown"))) {
            return Turnout.THROWN;
        } else if (str.equals(Bundle.getMessage("SensorStateActive"))) {
            return Sensor.ACTIVE;
        } else if (str.equals(Bundle.getMessage("SensorStateInactive"))) {
            return Sensor.INACTIVE;
        } else if (str.equals(rbx.getString("LightOn"))) {
            return Light.ON;
        } else if (str.equals(rbx.getString("LightOff"))) {
            return Light.OFF;
        } else if (str.equals(rbx.getString("TurnoutUnlock"))) {
            return Turnout.UNLOCKED;
        } else if (str.equals(rbx.getString("TurnoutLock"))) {
            return Turnout.LOCKED;
        } else if (str.equals(Bundle.getMessage("SignalHeadStateRed"))) {
            return SignalHead.RED;
        } else if (str.equals(Bundle.getMessage("SignalHeadStateYellow"))) {
            return SignalHead.YELLOW;
        } else if (str.equals(Bundle.getMessage("SignalHeadStateGreen"))) {
            return SignalHead.GREEN;
        } else if (str.equals(Bundle.getMessage("SignalHeadStateDark"))) {
            return SignalHead.DARK;
        } else if (str.equals(Bundle.getMessage("SignalHeadStateFlashingRed"))) {
            return SignalHead.FLASHRED;
        } else if (str.equals(Bundle.getMessage("SignalHeadStateFlashingYellow"))) {
            return SignalHead.FLASHYELLOW;
        } else if (str.equals(Bundle.getMessage("SignalHeadStateFlashingGreen"))) {
            return SignalHead.FLASHGREEN;
        } else if (str.equals(Bundle.getMessage("SignalHeadStateLunar"))) {
            return SignalHead.LUNAR;
        } else if (str.equals(Bundle.getMessage("SignalHeadStateFlashingLunar"))) {
            return SignalHead.FLASHLUNAR;
        } else if (str.equals(rbx.getString("AudioSourcePlay"))) {
            return Audio.CMD_PLAY;
        } else if (str.equals(rbx.getString("AudioSourceStop"))) {
            return Audio.CMD_STOP;
        } else if (str.equals(rbx.getString("AudioSourcePlayToggle"))) {
            return Audio.CMD_PLAY_TOGGLE;
        } else if (str.equals(rbx.getString("AudioSourcePause"))) {
            return Audio.CMD_PAUSE;
        } else if (str.equals(rbx.getString("AudioSourceResume"))) {
            return Audio.CMD_RESUME;
        } else if (str.equals(rbx.getString("AudioSourcePauseToggle"))) {
            return Audio.CMD_PAUSE_TOGGLE;
        } else if (str.equals(rbx.getString("AudioSourceRewind"))) {
            return Audio.CMD_REWIND;
        } else if (str.equals(rbx.getString("AudioSourceFadeIn"))) {
            return Audio.CMD_FADE_IN;
        } else if (str.equals(rbx.getString("AudioSourceFadeOut"))) {
            return Audio.CMD_FADE_OUT;
        } else if (str.equals(rbx.getString("AudioResetPosition"))) {
            return Audio.CMD_RESET_POSITION;
        }
        // empty strings can occur frequently with types that have no integer data
        if (str.length() > 0) {
            log.warn("Unexpected parameter to stringToActionData({})", str);
        }
        return -1;
    }

    /**
     * Get the Action Data String in I18N form.
     * @param t the Condition.Action, must be null.
     * @param data the constant for the action command type.
     * @return the action / data string.
     */
    @Nonnull
    public static String getActionDataString(@Nonnull Conditional.Action t, int data) {
        switch (t) {
            case SET_TURNOUT:
            case DELAYED_TURNOUT:
            case RESET_DELAYED_TURNOUT:
                switch (data) {
                    case Turnout.CLOSED:
                        return (Bundle.getMessage("TurnoutStateClosed"));
                    case Turnout.THROWN:
                        return (Bundle.getMessage("TurnoutStateThrown"));
                    case Route.TOGGLE:
                        return (Bundle.getMessage("Toggle"));
                    default:
                        break;
                }
                break;
            case SET_SIGNAL_APPEARANCE:
                return DefaultSignalHead.getDefaultStateName(data);
            case SET_SENSOR:
            case DELAYED_SENSOR:
            case RESET_DELAYED_SENSOR:
                switch (data) {
                    case Sensor.ACTIVE:
                        return (Bundle.getMessage("SensorStateActive"));
                    case Sensor.INACTIVE:
                        return (Bundle.getMessage("SensorStateInactive"));
                    case Route.TOGGLE:
                        return (Bundle.getMessage("Toggle"));
                    default:
                        break;
                }
                break;
            case SET_LIGHT:
                switch (data) {
                    case Light.ON:
                        return (rbx.getString("LightOn"));
                    case Light.OFF:
                        return (rbx.getString("LightOff"));
                    case Route.TOGGLE:
                        return (Bundle.getMessage("Toggle"));
                    default:
                        break;
                }
                break;
            case LOCK_TURNOUT:
                switch (data) {
                    case Turnout.UNLOCKED:
                        return (rbx.getString("TurnoutUnlock"));
                    case Turnout.LOCKED:
                        return (rbx.getString("TurnoutLock"));
                    case Route.TOGGLE:
                        return (Bundle.getMessage("Toggle"));
                    default:
                        break;
                }
                break;
            case CONTROL_AUDIO:
                switch (data) {
                    case Audio.CMD_PLAY:
                        return (rbx.getString("AudioSourcePlay"));
                    case Audio.CMD_STOP:
                        return (rbx.getString("AudioSourceStop"));
                    case Audio.CMD_PLAY_TOGGLE:
                        return (rbx.getString("AudioSourcePlayToggle"));
                    case Audio.CMD_PAUSE:
                        return (rbx.getString("AudioSourcePause"));
                    case Audio.CMD_RESUME:
                        return (rbx.getString("AudioSourceResume"));
                    case Audio.CMD_PAUSE_TOGGLE:
                        return (rbx.getString("AudioSourcePauseToggle"));
                    case Audio.CMD_REWIND:
                        return (rbx.getString("AudioSourceRewind"));
                    case Audio.CMD_FADE_IN:
                        return (rbx.getString("AudioSourceFadeIn"));
                    case Audio.CMD_FADE_OUT:
                        return (rbx.getString("AudioSourceFadeOut"));
                    case Audio.CMD_RESET_POSITION:
                        return (rbx.getString("AudioResetPosition"));
                    default:
                        log.error("Unhandled Audio operation command: {}", data);
                        break;
                }
                break;
            case CONTROL_TRAIN:
                switch (data) {
                    case Warrant.HALT:
                        return (rbx.getString("WarrantHalt"));
                    case Warrant.RESUME:
                        return (rbx.getString("WarrantResume"));
                    case Warrant.RETRY_FWD:
                        return (rbx.getString("WarrantMoveToNext"));
                    case Warrant.SPEED_UP:
                        return (rbx.getString("WarrantSpeedUp"));
                    case Warrant.STOP:
                        return (rbx.getString("WarrantStop"));
                    case Warrant.ESTOP:
                        return (rbx.getString("WarrantEStop"));
                    case Warrant.ABORT:
                        return (rbx.getString("WarrantAbort"));
                    default:
                        log.error("Unhandled Warrant control: {}", data);
                }
                break;
            default:
                // fall through
                break;
        }
        return "";
    }

    /**
     * {@inheritDoc }
     */
    @Nonnull
    @Override
    public String description(boolean triggerType) {
        String str = getOptionString(triggerType) + ", " + getTypeString();
        if (_deviceName.length() > 0) {
            switch (_type) {
                case CANCEL_TURNOUT_TIMERS:
                case SET_SIGNAL_HELD:
                case CLEAR_SIGNAL_HELD:
                case SET_SIGNAL_DARK:
                case SET_SIGNAL_LIT:
                case TRIGGER_ROUTE:
                case CANCEL_SENSOR_TIMERS:
                case SET_MEMORY:
                case ENABLE_LOGIX:
                case DISABLE_LOGIX:
                case COPY_MEMORY:
                case SET_LIGHT_INTENSITY:
                case SET_LIGHT_TRANSITION_TIME:
                case ALLOCATE_WARRANT_ROUTE:
                case DEALLOCATE_WARRANT_ROUTE:
                case SET_SIGNALMAST_HELD:
                case CLEAR_SIGNALMAST_HELD:
                case SET_SIGNALMAST_DARK:
                case SET_SIGNALMAST_LIT:
                case SET_BLOCK_ERROR:
                case CLEAR_BLOCK_ERROR:
                case DEALLOCATE_BLOCK:
                case SET_BLOCK_OUT_OF_SERVICE:
                case SET_BLOCK_IN_SERVICE:
                    str = str + ", \"" + _deviceName + "\".";
                    break;
                case SET_NXPAIR_ENABLED:
                case SET_NXPAIR_DISABLED:
                case SET_NXPAIR_SEGMENT:
                    NamedBean bean = getBean();
                    str = str + ", \"" + ( bean == null ? "<NULL>" : bean.getUserName() )+ "\".";
                    break;
                case SET_ROUTE_TURNOUTS:
                case AUTO_RUN_WARRANT:
                case MANUAL_RUN_WARRANT:
                    str = str + " " + rbx.getString("onWarrant") + ", \"" + _deviceName + "\".";
                    break;
                case SET_SENSOR:
                case SET_TURNOUT:
                case SET_LIGHT:
                case LOCK_TURNOUT:
                case RESET_DELAYED_SENSOR:
                case SET_SIGNAL_APPEARANCE:
                case RESET_DELAYED_TURNOUT:
                case DELAYED_TURNOUT:
                case DELAYED_SENSOR:
                case CONTROL_AUDIO:
                    str = str + ", \"" + _deviceName + "\" " + rbx.getString("to")
                            + " " + getActionDataString();
                    break;
                case GET_TRAIN_LOCATION:
                case GET_BLOCK_WARRANT:
                case GET_BLOCK_TRAIN_NAME:
                    str = str + " \"" + _deviceName + "\" " + rbx.getString("intoMemory")
                              + " " + _actionString;
                    break;
                case SET_SIGNALMAST_ASPECT:
                    str = str + ", \"" + _deviceName + "\" " + rbx.getString("to")
                            + " " + _actionString;
                    break;
                case CONTROL_TRAIN:
                    str = str + " " + rbx.getString("onWarrant") + " \"" + _deviceName + "\" "
                            + rbx.getString("to") + " " + getActionDataString();
                    break;
                default:
                    break; // nothing needed for others
            }
        }
        if (_actionString.length() > 0) {
            switch (_type) {
                case SET_MEMORY:
                case COPY_MEMORY:
                    str = str + " " + rbx.getString("to") + " " + _actionString + ".";
                    break;
                case PLAY_SOUND:
                case RUN_SCRIPT:
                    str = str + " " + rbx.getString("FromFile") + " " + _actionString + ".";
                    break;
                case RESET_DELAYED_TURNOUT:
                case RESET_DELAYED_SENSOR:
                case DELAYED_TURNOUT:
                case DELAYED_SENSOR:
                    str = str + rbx.getString("After") + " ";
                    try {
                        Float.valueOf(_actionString);
                        str = str + _actionString + " " + rbx.getString("Seconds") + ".";
                    } catch (NumberFormatException nfe) {
                        str = str + _actionString + " " + rbx.getString("ValueInMemory")
                                + " " + rbx.getString("Seconds") + ".";
                    }
                    break;
                case SET_LIGHT_TRANSITION_TIME:
                case SET_LIGHT_INTENSITY:
                    try {
                        //int t = Integer.parseInt(_actionString);
                        str = str + " " + rbx.getString("to") + " " + _actionString + ".";
                    } catch (NumberFormatException nfe) {
                        str = str + " " + rbx.getString("to") + " " + _actionString + " "
                                + rbx.getString("ValueInMemory") + ".";
                    }
                    break;
                case JYTHON_COMMAND:
                    str = str + " " + rbx.getString("ExecJythonCmd") + " " + _actionString + ".";
                    break;
                case SET_TRAIN_ID:
                case SET_TRAIN_NAME:
                    str = str + ", \"" + _actionString + "\" " + rbx.getString("onWarrant")
                            + " \"" + _deviceName + "\".";
                    break;
                case SET_BLOCK_VALUE:
                    str = str + ", \"" + _actionString + "\" " + rbx.getString("onBlock")
                            + " \"" + _deviceName + "\".";
                    break;
                default:
                    break; // nothing needed for others
            }
        }
        switch (_type) {
            case SET_LIGHT_INTENSITY:
            case SET_LIGHT_TRANSITION_TIME:
                str = str + " " + rbx.getString("to") + " " + _actionData + ".";
                break;
            case SET_FAST_CLOCK_TIME:
                str = str + " " + rbx.getString("to") + " "
                        + LogixTableAction.formatTime(_actionData / 60, _actionData - ((_actionData / 60) * 60));
                break;
            default:
                break; // nothing needed for others
        }
        return str;
    }

    /** {@inheritDoc} */
    @Override
    public void dispose() {
        if (_sound != null) {
            _sound.dispose();
        }
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DefaultConditionalAction.class);

}
