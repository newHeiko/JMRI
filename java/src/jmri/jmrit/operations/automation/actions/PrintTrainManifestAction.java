package jmri.jmrit.operations.automation.actions;

import jmri.InstanceManager;
import jmri.jmrit.operations.trains.*;

public class PrintTrainManifestAction extends Action {

    private static final int _code = ActionCodes.PRINT_TRAIN_MANIFEST;

    @Override
    public int getCode() {
        return _code;
    }

    @Override
    public String getName() {
        if (InstanceManager.getDefault(TrainManager.class).isPrintPreviewEnabled()) {
            return Bundle.getMessage("PreviewTrainManifest");
        } else {
            return Bundle.getMessage("PrintTrainManifest");
        }
    }

    @Override
    public void doAction() {
        if (getAutomationItem() != null) {
            Train train = getAutomationItem().getTrain();
            if (train != null && train.isBuilt()) {
                setRunning(true);
                try {
                    finishAction(train
                            .printManifest(InstanceManager.getDefault(TrainManager.class).isPrintPreviewEnabled()));
                } catch (BuildFailedException e) {
                    finishAction(false);
                }
            } else {
                finishAction(false);
            }
        }
    }

    @Override
    public void cancelAction() {
        // no cancel for this action
    }

}
