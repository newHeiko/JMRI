package jmri.configurexml;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.nio.file.Path;

import jmri.util.FileUtil;
import jmri.util.JUnitUtil;

import org.junit.jupiter.api.*;
import org.junit.Assert;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for ConfigXmlManager.
 * <p>
 * Uses the local preferences for test files.
 *
 * @author Bob Jacobsen Copyright 2003
 */
public class ConfigXmlManagerTest {

    private boolean innerFlag;

    @Test
    public void testRegisterOK() {
        ConfigXmlManager configxmlmanager = new ConfigXmlManager();

        Object o1 = new jmri.implementation.TripleTurnoutSignalHead("", "", null, null, null);
        configxmlmanager.registerConfig(o1);
        Assert.assertTrue("stored in clist", configxmlmanager.clist.size() == 1);
        configxmlmanager.deregister(o1);
        Assert.assertTrue("removed from clist", configxmlmanager.clist.isEmpty());
    }

    @Test
    public void testLogErrorOnStore() {
        ConfigXmlManager configxmlmanager = new ConfigXmlManager();
        innerFlag = false;
        ConfigXmlManager.setErrorHandler(new ErrorHandler() {
            @Override
            public void handle(ErrorMemo e) {
                innerFlag = true;
            }
        });

        Object o1 = new jmri.ConfigXmlHandle();
        configxmlmanager.registerUser(o1);

        // this will fail before reaching file
        try {
            configxmlmanager.storeUser(new File(FileUtil.getUserFilesPath(), "none"));
        } catch (Exception e) {
            // check that the handler was invoked
            Assert.assertTrue(innerFlag);
        }
    }

    @Test
    public void testFind() throws ClassNotFoundException {
        ConfigXmlManager configxmlmanager = new ConfigXmlManager();
        Object o1 = new jmri.implementation.TripleTurnoutSignalHead("SH1", "", null, null, null);
        Object o2 = new jmri.implementation.TripleTurnoutSignalHead("SH2", "", null, null, null);
        Object o3 = new jmri.implementation.TripleTurnoutSignalHead("SH3", "", null, null, null);
        innerFlag = false;
        configxmlmanager.registerConfig(o1, jmri.Manager.SIGNALHEADS);
        Assert.assertTrue("find found it", configxmlmanager.findInstance(o1.getClass(), 0) == o1);
        Assert.assertTrue("find only one so far", configxmlmanager.findInstance(o1.getClass(), 1) == null);
        configxmlmanager.deregister(o1);
        Assert.assertTrue("find none", configxmlmanager.findInstance(o1.getClass(), 0) == null);
        configxmlmanager.registerConfig(o1, jmri.Manager.SIGNALHEADS);
        configxmlmanager.registerConfig(o2, jmri.Manager.SIGNALHEADS);
        configxmlmanager.registerConfig(o3, jmri.Manager.SIGNALHEADS);
        Object ot = configxmlmanager.findInstance(o1.getClass(), 1);
        Assert.assertNotNull("findInstance(class, 1) not null", ot);
        Assert.assertEquals("findInstance(class, 1) equals o2",o2, ot);
        Assert.assertTrue("find found 2nd", configxmlmanager.findInstance(o1.getClass(), 1) == o2);
        Assert.assertTrue("find found subclass", configxmlmanager.findInstance(Class.forName("jmri.SignalHead"), 1) == o2);

    }

    @Test
    @Disabled("Test requires further development")
    public void testDeregister() {
    }

    @Test
    public void testAdapterName() {
        //ConfigXmlManager c = new ConfigXmlManager();
        Assert.assertEquals("String class adapter", "java.lang.configurexml.StringXml",
                ConfigXmlManager.adapterName(""));
    }

    @Test
    public void testCurrentClassName() {
        Assert.assertEquals("unmigrated", "jmri.managers.configurexml.DccSignalHeadXml",
                ConfigXmlManager.currentClassName("jmri.managers.configurexml.DccSignalHeadXml"));
        Assert.assertEquals("migrated", "jmri.managers.configurexml.DccSignalHeadXml",
                ConfigXmlManager.currentClassName("jmri.configurexml.DccSignalHeadXml"));
    }

    @Test
    public void testFindFile() throws FileNotFoundException, IOException {
        ConfigXmlManager configxmlmanager = new ConfigXmlManager() {
            @Override
            void locateClassFailed(Throwable ex, String adapterName, Object o) {
                innerFlag = true;
            }

            @Override
            void locateFileFailed(String f) {
                // suppress warning during testing
            }
        };
        URL result = configxmlmanager.find("foo.biff");
        Assert.assertNull("dont find foo.biff", result );

        // make sure no test file exists in "layout"
        FileUtil.createDirectory(FileUtil.getUserFilesPath() + "layout");
        File f = new File(FileUtil.getUserFilesPath() + "layout" + File.separator + "testConfigXmlManagerTest.xml");
        if (f.exists()) {
            Assert.assertTrue(f.delete()); // remove it if its there
        }

        // if file is at top level, remove that too
        f = new File("testConfigXmlManagerTest.xml");
        if (f.exists()) {
            Assert.assertTrue(f.delete());
        }

        // check for not found if doesn't exist
        result = configxmlmanager.find("testConfigXmlManagerTest.xml");
        Assert.assertTrue("should not find testConfigXmlManagerTest.xml", result == null);

        // put file back and find
        PrintStream p = new PrintStream(new FileOutputStream(f));
        p.println("stuff"); // load a new one
        p.close();

        result = configxmlmanager.find("testConfigXmlManagerTest.xml");
        Assert.assertTrue("should find testConfigXmlManagerTest.xml", result != null);
        Assert.assertTrue("file deleted 146", f.delete());  // make sure it's gone again

        // check file in the current app dir
        f = new File("testConfigXmlManagerTest.xml");
        Assert.assertFalse("file NOT deleted as already deleted",f.delete());  // remove it if its there
        p = new PrintStream(new FileOutputStream(f));
        p.println("stuff"); // load a new one
        p.close();

        result = configxmlmanager.find("testConfigXmlManagerTest.xml");
        Assert.assertTrue("should find testConfigXmlManagerTest.xml in app dir", result != null);
        Assert.assertTrue(f.delete());  // make sure it's gone again
    }

    @BeforeEach
    public void setUp(@TempDir Path tempDir) throws IOException  {
        JUnitUtil.setUp();
        JUnitUtil.resetProfileManager( new jmri.profile.NullProfile( tempDir.toFile()));
    }

    @AfterEach
    public void tearDown() {
        JUnitUtil.tearDown();
    }
}
