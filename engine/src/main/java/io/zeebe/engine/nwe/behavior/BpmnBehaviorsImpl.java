/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Zeebe Community License 1.0. You may not use this file
 * except in compliance with the Zeebe Community License 1.0.
 */
package io.zeebe.engine.nwe.behavior;

import io.zeebe.engine.metrics.WorkflowEngineMetrics;
import io.zeebe.engine.nwe.BpmnElementContainerProcessor;
import io.zeebe.engine.nwe.WorkflowInstanceStateTransitionGuard;
import io.zeebe.engine.processor.TypedCommandWriter;
import io.zeebe.engine.processor.TypedResponseWriter;
import io.zeebe.engine.processor.TypedStreamWriter;
import io.zeebe.engine.processor.workflow.CatchEventBehavior;
import io.zeebe.engine.processor.workflow.ExpressionProcessor;
import io.zeebe.engine.processor.workflow.deployment.model.element.ExecutableFlowElement;
import io.zeebe.engine.state.ZeebeState;
import io.zeebe.protocol.record.value.BpmnElementType;
import java.util.function.Function;

public final class BpmnBehaviorsImpl implements BpmnBehaviors {

  private final ExpressionProcessor expressionBehavior;
  private final BpmnVariableMappingBehavior variableMappingBehavior;
  private final BpmnEventPublicationBehavior eventPublicationBehavior;
  private final BpmnEventSubscriptionBehavior eventSubscriptionBehavior;
  private final BpmnIncidentBehavior incidentBehavior;
  private final BpmnStateBehavior stateBehavior;
  private final BpmnStateTransitionBehavior stateTransitionBehavior;
  private final BpmnDeferredRecordsBehavior deferredRecordsBehavior;
  private final WorkflowInstanceStateTransitionGuard stateTransitionGuard;
  private final TypedStreamWriter streamWriter;
  private final BpmnWorkflowResultSenderBehavior workflowResultSenderBehavior;
  private final BpmnBufferedMessageStartEventBehavior bufferedMessageStartEventBehavior;

  public BpmnBehaviorsImpl(
      final ExpressionProcessor expressionBehavior,
      final TypedStreamWriter streamWriter,
      final TypedResponseWriter responseWriter,
      final ZeebeState zeebeState,
      final CatchEventBehavior catchEventBehavior,
      final Function<BpmnElementType, BpmnElementContainerProcessor<ExecutableFlowElement>>
          processorLookup) {

    this.streamWriter = streamWriter;
    this.expressionBehavior = expressionBehavior;

    stateBehavior = new BpmnStateBehavior(zeebeState);
    stateTransitionGuard = new WorkflowInstanceStateTransitionGuard(stateBehavior);
    variableMappingBehavior = new BpmnVariableMappingBehavior(expressionBehavior, zeebeState);
    stateTransitionBehavior =
        new BpmnStateTransitionBehavior(
            streamWriter,
            zeebeState.getKeyGenerator(),
            stateBehavior,
            new WorkflowEngineMetrics(zeebeState.getPartitionId()),
            stateTransitionGuard,
            processorLookup);
    eventSubscriptionBehavior =
        new BpmnEventSubscriptionBehavior(
            stateBehavior, stateTransitionBehavior, catchEventBehavior, streamWriter, zeebeState);
    incidentBehavior = new BpmnIncidentBehavior(zeebeState, streamWriter);
    deferredRecordsBehavior = new BpmnDeferredRecordsBehavior(zeebeState);
    eventPublicationBehavior = new BpmnEventPublicationBehavior(zeebeState, streamWriter);
    workflowResultSenderBehavior = new BpmnWorkflowResultSenderBehavior(zeebeState, responseWriter);
    bufferedMessageStartEventBehavior =
        new BpmnBufferedMessageStartEventBehavior(zeebeState, streamWriter);
  }

  @Override
  public ExpressionProcessor expressionBehavior() {
    return expressionBehavior;
  }

  @Override
  public BpmnVariableMappingBehavior variableMappingBehavior() {
    return variableMappingBehavior;
  }

  @Override
  public BpmnEventPublicationBehavior eventPublicationBehavior() {
    return eventPublicationBehavior;
  }

  @Override
  public BpmnEventSubscriptionBehavior eventSubscriptionBehavior() {
    return eventSubscriptionBehavior;
  }

  @Override
  public BpmnIncidentBehavior incidentBehavior() {
    return incidentBehavior;
  }

  @Override
  public BpmnStateBehavior stateBehavior() {
    return stateBehavior;
  }

  @Override
  public TypedCommandWriter commandWriter() {
    return streamWriter;
  }

  @Override
  public BpmnStateTransitionBehavior stateTransitionBehavior() {
    return stateTransitionBehavior;
  }

  @Override
  public BpmnDeferredRecordsBehavior deferredRecordsBehavior() {
    return deferredRecordsBehavior;
  }

  @Override
  public WorkflowInstanceStateTransitionGuard stateTransitionGuard() {
    return stateTransitionGuard;
  }

  @Override
  public BpmnWorkflowResultSenderBehavior workflowResultSenderBehavior() {
    return workflowResultSenderBehavior;
  }

  @Override
  public BpmnBufferedMessageStartEventBehavior bufferedMessageStartEventBehavior() {
    return bufferedMessageStartEventBehavior;
  }
}
