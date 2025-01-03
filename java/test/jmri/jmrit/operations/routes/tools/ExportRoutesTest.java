package jmri.jmrit.operations.routes.tools;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

import jmri.jmrit.operations.OperationsTestCase;
import jmri.util.JUnitOperationsUtil;
import jmri.util.swing.JemmyUtil;

/**
 *
 * @author Paul Bender Copyright (C) 2017
 * @author Daniel Boudreau Copyright (C) 2019
 */
public class ExportRoutesTest extends OperationsTestCase {

    @Test
    public void testCTor() {
        ExportRoutes t = new ExportRoutes();
        Assert.assertNotNull("exists", t);
    }

    @Test
    @jmri.util.junit.annotations.DisabledIfHeadless
    public void testCreateFile() {
        ExportRoutes exportRoutes = new ExportRoutes();
        Assert.assertNotNull("exists", exportRoutes);

        JUnitOperationsUtil.initOperationsData();

        // next should cause export complete dialog to appear
        Thread export = new Thread(exportRoutes::writeOperationsRoutesFile);
        export.setName("Export Routes"); // NOI18N
        export.start();

        jmri.util.JUnitUtil.waitFor(() -> {
            return export.getState().equals(Thread.State.WAITING);
        }, "wait for prompt");

        JemmyUtil.pressDialogButton(Bundle.getMessage("ExportComplete"), Bundle.getMessage("ButtonOK"));

        jmri.util.JUnitUtil.waitFor(() -> !export.isAlive(), "wait for export to complete");

        java.io.File file = new java.io.File(ExportRoutes.defaultOperationsFilename());
        Assert.assertTrue("Confirm file creation", file.exists());

    }

    // private final static Logger log = LoggerFactory.getLogger(ExportTrainsTest.class);

}
