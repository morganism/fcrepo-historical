package fedora.server.resourceIndex;

import java.io.File;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.jrdf.graph.Triple;
import org.trippi.TripleIterator;
import org.trippi.TripleMaker;

import fedora.common.PID;
import fedora.server.errors.ResourceIndexException;
import fedora.server.storage.types.Datastream;
import fedora.server.storage.types.DigitalObject;

/**
 * @author Edwin Shin
 *  
 */
public class TestResourceIndexImpl extends TestResourceIndex {
    private DigitalObject bdef, bmech, dataobject;

    //public static void main(String[] args) {
    //    junit.textui.TestRunner.run(TestResourceIndexImpl.class);
    //}

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
    	if (FEDORA_HOME == null) {
    		throw new Exception("FEDORA_HOME is null");
    	}
        super.setUp();
        
        bdef = getFoxmlObject(new File(DEMO_OBJECTS_ROOT_DIR
                + "/bdefs/demo_ri8.xml"));
        bmech = getFoxmlObject(new File(DEMO_OBJECTS_ROOT_DIR
                + "/bmechs/demo_ri9.xml"));
        dataobject = getFoxmlObject(new File(
                DEMO_OBJECTS_ROOT_DIR + "/dataobjects/demo_ri10.xml"));
    }

    public void testAddDigitalObject() throws Exception {    	
        DigitalObject obj = getFoxmlObject(new File(DEMO_OBJECTS_ROOT_DIR
                + "/dataobjects/demo_ri1000.xml"));
        m_ri.addDigitalObject(obj);
        m_ri.commit();
        
        int count = m_ri.countTriples(null, null, null, 0);
        if (m_ri.getIndexLevel() == ResourceIndex.INDEX_LEVEL_OFF) {
            assertEquals(0, count);
        } else if (m_ri.getIndexLevel() == ResourceIndex.INDEX_LEVEL_ON) {
            assertEquals(3, count);
        } else if (m_ri.getIndexLevel() == ResourceIndex.INDEX_LEVEL_PERMUTATIONS) {
            assertEquals(3, count);
        }
        TripleIterator it = m_ri.findTriples(TripleMaker.createResource(PID.toURI(obj.getPid())), 
                                             null, null, 0);
        Triple t;
        while (it.hasNext()) {
            t = (Triple)it.next();
            assertEquals(PID.toURI(obj.getPid()), t.getSubject().toString());
            assertNotNull(t.getPredicate());
            assertNotNull(t.getObject());
        }
    }
        
    public void testAddAndDelete() throws Exception {  
        m_ri.addDigitalObject(bdef);
        m_ri.commit();
        int a = m_ri.countTriples(null, null, null, 0);
        if (m_ri.getIndexLevel() == ResourceIndex.INDEX_LEVEL_OFF) {
            assertEquals(0, a);
        } else if (m_ri.getIndexLevel() == ResourceIndex.INDEX_LEVEL_ON) {
            assertEquals(32, a);
        } else if (m_ri.getIndexLevel() == ResourceIndex.INDEX_LEVEL_PERMUTATIONS) {
            assertEquals(32, a);
        }
        
        m_ri.addDigitalObject(bmech);
        m_ri.commit();
        int b = m_ri.countTriples(null, null, null, 0);
        export("/tmp/b.rdf");
        if (m_ri.getIndexLevel() == ResourceIndex.INDEX_LEVEL_OFF) {
            assertEquals(0, b);
        } else if (m_ri.getIndexLevel() == ResourceIndex.INDEX_LEVEL_ON) {
            assertEquals(84, b);
        } else if (m_ri.getIndexLevel() == ResourceIndex.INDEX_LEVEL_PERMUTATIONS) {
            assertEquals(84, b);
        }
        
        m_ri.addDigitalObject(dataobject);
        m_ri.commit();
        int c = m_ri.countTriples(null, null, null, 0);
        if (m_ri.getIndexLevel() == ResourceIndex.INDEX_LEVEL_OFF) {
            assertEquals(0, c);
        } else if (m_ri.getIndexLevel() == ResourceIndex.INDEX_LEVEL_ON) {
            assertEquals(166, c);
        } else if (m_ri.getIndexLevel() == ResourceIndex.INDEX_LEVEL_PERMUTATIONS) {
            assertEquals(187, c);
        }
        m_ri.deleteDigitalObject(dataobject);
        
        m_ri.commit();
        export("/tmp/d.rdf");
        int d = m_ri.countTriples(null, null, null, 0);
        assertTrue("Expected " + b + " but was " + d, d == b);
        
        m_ri.deleteDigitalObject(bmech);
        m_ri.commit();
        
        int e = m_ri.countTriples(null, null, null, 0);
        assertTrue(e == a);
        
        m_ri.deleteDigitalObject(bdef);
        m_ri.commit();
        int f = m_ri.countTriples(null, null, null, 0);
        assertEquals(0, f);
    }
    
    public void testDoubleAdd() throws Exception {
        m_ri.addDigitalObject(bdef);
        m_ri.addDigitalObject(bmech);
        m_ri.addDigitalObject(dataobject);
        m_ri.commit();
        int a = m_ri.countTriples(null, null, null, 0);
        
        m_ri.addDigitalObject(dataobject);
        m_ri.commit();
        int b = m_ri.countTriples(null, null, null, 0);
        assertEquals(a, b);
    }
    
    public void testModify() throws Exception {
        m_ri.addDigitalObject(dataobject);
        m_ri.commit();
        int a = m_ri.countTriples(null, null, null, 0);
        
        m_ri.deleteDigitalObject(dataobject);
        m_ri.commit();
        
        m_ri.addDigitalObject(dataobject);
    	dataobject.setLastModDate(new Date());
    	m_ri.modifyDigitalObject(dataobject);
    	m_ri.commit();
    	int b = m_ri.countTriples(null, null, null, 0);
    	assertEquals(a, b);
    	
    	// test adding a datastream with a null mimetype
    	Datastream ds = getLatestDatastream(dataobject.datastreams("DC"));
    	ds.DSMIME = null;
    	m_ri.modifyDigitalObject(dataobject);
    	m_ri.commit();
    	b = m_ri.countTriples(null, null, null, 0);
    	assertEquals(a, b);
    	
    	// test adding a datastream with a null mimetype
    	ds = getLatestDatastream(dataobject.datastreams("DC"));
    	ds.DSMIME = "";
    	m_ri.modifyDigitalObject(dataobject);
    	m_ri.commit();
    	b = m_ri.countTriples(null, null, null, 0);
    	assertEquals(a, b);
    }
    
    private Datastream getLatestDatastream(List datastreams) throws ResourceIndexException {
        Iterator it = datastreams.iterator();
        long latestDSCreateDT = -1;
        Datastream ds, latestDS = null;
        while (it.hasNext()) {
            ds = (Datastream)it.next();
            if (ds.DSCreateDT == null) {
                throw new ResourceIndexException("Datastream, " + ds.DSVersionID + ", is missing create date");
            } else if (ds.DSCreateDT.getTime() > latestDSCreateDT) {
                latestDS = ds;
                latestDSCreateDT = ds.DSCreateDT.getTime();
            }
        }
        return latestDS;
    }
}