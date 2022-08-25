package at.ac.tuwien.auto.bpmn_service;

import at.ac.tuwien.auto.bpmn_service.util.BPMNFileParser;
import at.ac.tuwien.auto.bpmn_service.config.ConfigurationManager;
import io.zeebe.spring.client.EnableZeebeClient;
import io.zeebe.spring.client.ZeebeClientLifecycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.io.Resource;

import javax.xml.stream.XMLStreamException;
import java.io.FileNotFoundException;
import java.util.*;

/**
 * @author Daniel Ramsauer
 * @since 23.09.2021, Do.
 */
@SpringBootApplication
@EnableZeebeClient
public class BPMNServiceApplication implements CommandLineRunner {

	private static final Logger LOG = LoggerFactory.getLogger(BPMNServiceApplication.class);
	private final ZeebeClientLifecycle client;
	private final ConfigurationManager cm;

	@Value("classpath:workflows/*")
	private Resource[] resources;

	public BPMNServiceApplication(ZeebeClientLifecycle client, ConfigurationManager cm) {
		this.client = client;
		this.cm = cm;
	}

	public static void main(String[] args) {
		SpringApplication.run(BPMNServiceApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		LOG.info("Starting BPMN Service...");
		Set<String> bpmnInstanceNames = new HashSet<>();
		Set<String> bpmnSubscribeTopics = new HashSet<>();
		BPMNFileParser parser;
		for (final Resource res : resources) {
			try {
				if (!Objects.requireNonNull(res.getFilename()).endsWith(".bpmn")) {
					LOG.info("File " + res.getFilename() + " dose not end with .bpmn");
					continue;
				}
			} catch (NullPointerException np) {
				LOG.error("Could not properly read " + res.getFilename(), np);
				continue;
			}
			try {
				parser = new BPMNFileParser(res.getFile());
			} catch (XMLStreamException xmle) {
				LOG.error("Could not parse " + res.getFilename(), xmle);
				continue;
			} catch (FileNotFoundException fe) {
				LOG.error("Could not find " + res.getFilename(), fe);
				continue;
			}

			bpmnInstanceNames.addAll(parser.getNewInstanceNames());
			bpmnSubscribeTopics.addAll(parser.getBpmnSubscribeTopics());
			this.client.newDeployCommand().addResourceFromClasspath("workflows/" + res.getFilename()).send().join();
		}
		cm.setBpmnNewInstanceNames(bpmnInstanceNames.toArray(new String[0]));
		cm.setServiceNames(bpmnSubscribeTopics.toArray(new String[0]));
	}
}
