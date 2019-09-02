package com.github.containersolutions.operator;

import com.github.containersolutions.operator.sample.TestCustomResource;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.client.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;


import java.util.HashMap;
import java.util.Map;

import static java.lang.Thread.sleep;
import static org.mockito.Mockito.*;

class EventSchedulerTest {

    TestCustomResource testCustomResource;
    private EventScheduler eventScheduler;
    private EventDispatcher<TestCustomResource> eventDispatcher = mock(EventDispatcher.class);
    private String mockUid = "3a83e9ec-227c-40d7-a3cb-219ae6a22e5b";


    @BeforeEach
    public void setup() {
        eventScheduler = new EventScheduler(eventDispatcher);

        testCustomResource = new TestCustomResource();
        ObjectMeta metadata = new ObjectMeta();
        metadata.setUid(mockUid);
        testCustomResource.setMetadata(metadata);

    }
    @Test
    public void callEventReceived() {
        eventScheduler.eventReceived(Watcher.Action.ADDED, testCustomResource);
        verify(eventDispatcher, times(1)).handleEvent(ArgumentMatchers.eq(Watcher.Action.ADDED), ArgumentMatchers.eq(testCustomResource));
    }

    @Test
    public void callEventReceivedThrowsException() {
        doThrow(new RuntimeException()).when(eventDispatcher).handleEvent(Watcher.Action.ADDED, testCustomResource);
        eventScheduler.eventReceived(Watcher.Action.ADDED, testCustomResource);

        try {
            sleep(5000l);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // calls handleEvent 2 times (one standard, one unsuccessful retry in the time the test takes)
        verify(eventDispatcher, times(2)).handleEvent(ArgumentMatchers.eq(Watcher.Action.ADDED), ArgumentMatchers.eq(testCustomResource));
    }

    @Test
    public void callEventReceivedThrowsExceptionOnce() {
        doThrow(new RuntimeException()).doNothing().when(eventDispatcher).handleEvent(Watcher.Action.ADDED, testCustomResource);
        eventScheduler.eventReceived(Watcher.Action.ADDED, testCustomResource);

        try {
            sleep(10000l);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // calls handleEvent 2 times (one standard, one successful retry)
        verify(eventDispatcher, times(2)).handleEvent(ArgumentMatchers.eq(Watcher.Action.ADDED), ArgumentMatchers.eq(testCustomResource));

    }

    @Test
    public void callEventReceivedWithTwoConflictingEvents() {

        TestCustomResource testCustomResourceModified = new TestCustomResource();
        ObjectMeta metadata = new ObjectMeta();
        metadata.setUid(mockUid);
        Map labels = new HashMap<String,String>();
        labels.put("Object","Modified");
        metadata.setLabels(labels);
        testCustomResourceModified.setMetadata(metadata);

        doThrow(new RuntimeException()).when(eventDispatcher).handleEvent(Watcher.Action.ADDED, testCustomResource);
        doThrow(new RuntimeException()).doNothing().when(eventDispatcher).handleEvent(Watcher.Action.DELETED, testCustomResourceModified);

        eventScheduler.eventReceived(Watcher.Action.ADDED, testCustomResource);
        eventScheduler.eventReceived(Watcher.Action.DELETED, testCustomResourceModified);

        try {
            sleep(10000l);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // tries first event once, fails, second interrupts, tries second once, fails, then succeeds
        verify(eventDispatcher, times(1)).handleEvent(ArgumentMatchers.eq(Watcher.Action.ADDED), ArgumentMatchers.eq(testCustomResource));
        verify(eventDispatcher, times(2)).handleEvent(ArgumentMatchers.eq(Watcher.Action.DELETED), ArgumentMatchers.eq(testCustomResourceModified));
    }

    @Test
    public void callEventReceivedWithTwoConflictingEvents2() {

        TestCustomResource testCustomResourceModified = new TestCustomResource();
        ObjectMeta metadata = new ObjectMeta();
        metadata.setUid(mockUid);
        Map labels = new HashMap<String,String>();
        labels.put("Object","Modified");
        metadata.setLabels(labels);
        testCustomResourceModified.setMetadata(metadata);

        doThrow(new RuntimeException()).when(eventDispatcher).handleEvent(Watcher.Action.ADDED, testCustomResource);
        doThrow(new RuntimeException()).when(eventDispatcher).handleEvent(Watcher.Action.DELETED, testCustomResourceModified);

        eventScheduler.eventReceived(Watcher.Action.ADDED, testCustomResource);
        eventScheduler.eventReceived(Watcher.Action.DELETED, testCustomResourceModified);

        try {
            sleep(10000l);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // tries first event once, fails, second interrupts, tries second, fails three times
        verify(eventDispatcher, times(1)).handleEvent(ArgumentMatchers.eq(Watcher.Action.ADDED), ArgumentMatchers.eq(testCustomResource));
        verify(eventDispatcher, times(3)).handleEvent(ArgumentMatchers.eq(Watcher.Action.DELETED), ArgumentMatchers.eq(testCustomResourceModified));
    }
}


