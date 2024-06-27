package za.co.telkom.bpmn.test;

import static io.camunda.zeebe.process.test.assertions.BpmnAssert.assertThat;
import static io.camunda.zeebe.protocol.Protocol.USER_TASK_JOB_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.ActivatedJob;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import io.camunda.zeebe.process.test.api.ZeebeTestEngine;
import io.camunda.zeebe.spring.test.ZeebeSpringTest;
import lombok.extern.slf4j.Slf4j;
import za.co.telkom.bpmn.WorkflowApplication;

@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT, classes = WorkflowApplication.class)
@ZeebeSpringTest
@Slf4j
public class RicaTest {

	@Autowired
	private ZeebeClient zeebe;

	@Autowired
	private ZeebeTestEngine zeebeTestEngine;

	// Test RICA end to end happy day

	@Test
	public void rica_end_to_end_happy_day_test() throws Exception{
		// start a process instance
		log.info("Step 1 : Start Process");
		ProcessInstanceEvent processInstance = zeebe.
				newCreateInstanceCommand().
				bpmnProcessId("web-self-service-rica") /// Channel-CustomerJourney-Function
				.latestVersion().
				variables(Map.of("name", "vutlhari", "surname", "ndlovhu", "iccid",
						"983484939832983298", "identificationNumber", 
						"0011245516088", "identificationType", "RSA-ID"))
				.send().join();
		// Find the human task , add contact information and complete the human
		waitForUserTaskAndComplete("capture-contact-information",Map.of("cellNumber", "0810001111", 
				"email", "ndlovhu@telkom.co.za", "address",
				"64 Oak Avenue"));
//		 Check if process has some variables and also pasted some tasks
	    assertThat(processInstance)
        .hasPassedElement("capture-contact-information")
        .hasVariable("name")
        .hasVariable("surname")
        .hasVariable("iccid")
        .hasVariable("identificationNumber")
        .hasVariable("identificationType")
        .hasVariable("cellNumber")
        .hasVariable("email")
        .hasVariable("address")
        .isCompleted();

	}

	// Test Invalid ICCID

	// Test Invalid ID From CRM/Party Service

	// Test Invalid ID From Credit B Service

	// Test Invalid ID From SAFPS (Home A) Service (Phase 2)

	// Test Invalid ID From IVS (Home A) Service

	// Test ID Document upload Failure

	// Test POA Document upload Failure

	// Test Selfie upload Failure

	// Facial Reg (Phase 2)

	// Fail RICA Integration Service

	// Test Sharepoint ID Document upload Failure

	// Test Sharepoint POA Document upload Failure

	// Test Sharepoint Selfie upload Failure

	public void waitForUserTaskAndComplete(String userTaskId, Map<String, Object> variables)
			throws InterruptedException, TimeoutException {
		// Let the workflow engine do whatever it needs to do
		zeebeTestEngine.waitForIdleState(Duration.ofMinutes(5));

		// Now get all user tasks
		List<ActivatedJob> jobs = zeebe.newActivateJobsCommand().jobType(USER_TASK_JOB_TYPE).maxJobsToActivate(1)
				.workerName("waitForUserTaskAndComplete").send().join().getJobs();

		// Should be only one
		assertTrue(jobs.size() > 0, "Job for user task '" + userTaskId + "' does not exist");
		ActivatedJob userTaskJob = jobs.get(0);
		// Make sure it is the right one
		if (userTaskId != null) {
			assertEquals(userTaskId, userTaskJob.getElementId());
		}

		// And complete it passing the variables
		if (variables != null && variables.size() > 0) {
			zeebe.newCompleteCommand(userTaskJob.getKey()).variables(variables).send().join();
		} else {
			zeebe.newCompleteCommand(userTaskJob.getKey()).send().join();
		}
	}

}


