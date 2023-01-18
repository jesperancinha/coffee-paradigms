package com.jesperancinha.coffee.system.concurrency;

import com.jesperancinha.coffee.system.api.concurrency.QueueCallable;
import com.jesperancinha.coffee.system.manager.MachineProcessorImpl;
import com.jesperancinha.coffee.system.manager.PostProcessorImpl;
import org.jesperancinha.coffee.system.input.CoffeeMachines.CoffeMachine.PaymentTypes.Payment;
import org.jesperancinha.coffee.system.input.Employees.Employee;
import org.jesperancinha.coffee.system.input.Employees.Employee.Actions.PostAction;
import com.jesperancinha.coffee.system.manager.GeneralProcessorImpl;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.MessageFormat;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by joaofilipesabinoesperancinha on 01-05-16.
 */
@Service
@Getter
@Slf4j
public class PaymentCallableImpl extends QueueCallableAbstract implements QueueCallable {

    private final Payment chosenPayment;
    @Autowired
    private MachineProcessorImpl machineProcessor;
    private Employee employee;
    private String name;
    private List<PostAction> postActions;


    public PaymentCallableImpl(
            Employee employee,
            String name,
            Payment payment,
            List<PostAction> postActions,
            MachineProcessorImpl machineProcessor
    ) {
        super();
        this.employee = employee;
        this.chosenPayment = payment;
        this.name = name;
        this.postActions = postActions;
        this.machineProcessor = machineProcessor;
    }

    @Override
    public Boolean call() throws Exception {
        log.info(MessageFormat.format("PaymentCallable with {0}", chosenPayment.getName()));
        Integer time = chosenPayment.getTime();
        if (time != null) {
            TimeUnit.MILLISECONDS.sleep(time);
        }
        final PostProcessorImpl postProcessor = machineProcessor.getPostProcessor();
        machineProcessor.callPostActions(employee, GeneralProcessorImpl.MAIN_QUEUE_POST, postActions, this);
        postProcessor.runAllCalls(this);
        postProcessor.waitForAllCalls(this);
        return true;
    }

}