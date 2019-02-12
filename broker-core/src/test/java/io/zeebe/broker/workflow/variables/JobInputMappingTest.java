/*
 * Zeebe Broker Core
 * Copyright © 2017 camunda services GmbH (info@camunda.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.zeebe.broker.workflow.variables;

import io.zeebe.broker.test.EmbeddedBrokerRule;
import io.zeebe.exporter.record.Record;
import io.zeebe.exporter.record.value.JobRecordValue;
import io.zeebe.model.bpmn.Bpmn;
import io.zeebe.model.bpmn.builder.ServiceTaskBuilder;
import io.zeebe.protocol.intent.JobIntent;
import io.zeebe.test.broker.protocol.clientapi.ClientApiRule;
import io.zeebe.test.broker.protocol.clientapi.PartitionTestClient;
import io.zeebe.test.util.JsonUtil;
import io.zeebe.test.util.record.RecordingExporter;
import java.util.function.Consumer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class JobInputMappingTest {

  private static final String PROCESS_ID = "process";

  public EmbeddedBrokerRule brokerRule = new EmbeddedBrokerRule();
  public ClientApiRule apiRule = new ClientApiRule(brokerRule::getClientAddress);

  @Rule public RuleChain ruleChain = RuleChain.outerRule(brokerRule).around(apiRule);

  @Parameter(0)
  public String initialPayload;

  @Parameter(1)
  public Consumer<ServiceTaskBuilder> mappings;

  @Parameter(2)
  public String expectedPayload;

  @Parameters(name = "from {0} to {2}")
  public static Object[][] parameters() {
    return new Object[][] {
      {"{}", mapping(b -> {}), "{}"},
      {"{'x': 1, 'y': 2}", mapping(b -> {}), "{'x': 1, 'y': 2}"},
      {"{'x': {'y': 2}}", mapping(b -> {}), "{'x': {'y': 2}}"},
      {"{'x': 1}", mapping(b -> b.zeebeInput("$.x", "$.y")), "{'y': 1}"},
      {
        "{'x': 1}",
        mapping(b -> b.zeebeInput("$.x", "$.y").zeebeInput("$.x", "$.z")),
        "{'y': 1, 'z': 1}"
      },
      {"{'x': {'y': 2}}", mapping(b -> b.zeebeInput("$.x.y", "$.y")), "{'y': 2}"},
    };
  }

  private PartitionTestClient testClient;

  @Before
  public void init() {
    testClient = apiRule.partitionClient();
  }

  @Test
  public void shouldApplyInputMappings() {
    // given
    testClient.deploy(
        Bpmn.createExecutableProcess(PROCESS_ID)
            .startEvent()
            .serviceTask(
                "service",
                builder -> {
                  builder.zeebeTaskType("test");
                  mappings.accept(builder);
                })
            .endEvent()
            .done());

    // when
    testClient.createWorkflowInstance(PROCESS_ID, initialPayload);

    // then
    final Record<JobRecordValue> jobCreated =
        RecordingExporter.jobRecords(JobIntent.CREATED).getFirst();

    JsonUtil.assertEquality(jobCreated.getValue().getPayload(), expectedPayload);
  }

  private static Consumer<ServiceTaskBuilder> mapping(Consumer<ServiceTaskBuilder> mappingBuilder) {
    return mappingBuilder;
  }
}
