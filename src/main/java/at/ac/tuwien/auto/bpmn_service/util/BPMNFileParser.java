package at.ac.tuwien.auto.bpmn_service.util;

import lombok.Getter;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Daniel Ramsauer
 * @since 12.04.2022, Di.
 */
public class BPMNFileParser {

    @Getter
    private final List<String> newInstanceNames = new ArrayList<>();
    @Getter
    private final List<String> bpmnSubscribeTopics = new ArrayList<>();

    public BPMNFileParser(File file) throws XMLStreamException, FileNotFoundException {
        XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
        XMLEventReader reader = xmlInputFactory.createXMLEventReader(new FileInputStream(file));
        boolean startEventOpen = false;
        boolean intermediateMessageEventOpen = false;
        List<String> messageRefsForNewInstances = new ArrayList<>();
        List<String> messageRefsForTopics = new ArrayList<>();

        while (reader.hasNext()) {
            XMLEvent nextEvent = reader.nextEvent();

            if (nextEvent.isStartElement()) {
                StartElement startElement = nextEvent.asStartElement();
                if (startElement.getName().getLocalPart().equals("startEvent")) {
                    startEventOpen = true;
                }
                if (startElement.getName().getLocalPart().equals("intermediateCatchEvent")) {
                    intermediateMessageEventOpen = true;
                }

                if (startEventOpen && startElement.getName().getLocalPart().equals("messageEventDefinition")) {
                    messageRefsForNewInstances.add(startElement.getAttributeByName(new QName("messageRef")).getValue());
                }
                if (intermediateMessageEventOpen && startElement.getName().getLocalPart().equals("messageEventDefinition")) {
                    messageRefsForTopics.add(startElement.getAttributeByName(new QName("messageRef")).getValue());
                }
//                if (startElement.getName().getLocalPart().equals("message")) {
//                    if (startElement.getAttributeByName(new QName("id")) != null) {
//                        String id = startElement.getAttributeByName(new QName("id")).getValue();
//                        if (messageRefsForNewInstances.contains(id)) {
//                            newInstanceNames.add(startElement.getAttributeByName(new QName("name")).getValue());
//                        }
//                        if (messageRefsForTopics.contains(id)) {
//                            bpmnSubscribeTopics.add(startElement.getAttributeByName(new QName("name")).getValue());
//                        }
//                    }
//                }
            }
            if (nextEvent.isEndElement()) {
                EndElement endElement = nextEvent.asEndElement();
                if (endElement.getName().getLocalPart().equals("startEvent")) {
                    startEventOpen = false;
                }
                if (endElement.getName().getLocalPart().equals("intermediateCatchEvent")) {
                    intermediateMessageEventOpen = false;
                }
            }
        }
        reader = xmlInputFactory.createXMLEventReader(new FileInputStream(file));
        while (reader.hasNext()) {
            XMLEvent nextEvent = reader.nextEvent();

            if (nextEvent.isStartElement()) {
                StartElement startElement = nextEvent.asStartElement();
                if (startElement.getName().getLocalPart().equals("message")) {
                    if (startElement.getAttributeByName(new QName("id")) != null) {
                        String id = startElement.getAttributeByName(new QName("id")).getValue();
                        if (messageRefsForNewInstances.contains(id)) {
                            newInstanceNames.add(startElement.getAttributeByName(new QName("name")).getValue());
                        }
                        if (messageRefsForTopics.contains(id)) {
                            bpmnSubscribeTopics.add(startElement.getAttributeByName(new QName("name")).getValue());
                        }
                    }
                }
            }
        }
    }
}
