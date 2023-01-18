package com.jesperancinha.coffee.system.concurrency;

import com.jesperancinha.coffee.system.api.concurrency.QueueCallable;
import com.jesperancinha.coffee.system.manager.MachineProcessorImpl;
import org.jesperancinha.coffee.system.input.Employees.Employee.Actions.PostAction;
import com.jesperancinha.coffee.system.objects.ActionDescriptor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.MessageFormat;
import java.util.concurrent.TimeUnit;

/**
 * Created by joaofilipesabinoesperancinha on 05-05-16.
 */
@Service
@Getter
@Slf4j
public class PostActionCallableImpl extends ActionCallable implements QueueCallable {

    @Autowired
    private MachineProcessorImpl machineProcessor;

    public PostActionCallableImpl(String name) {
        super(name);

    }

    public void addPostAction(PostAction postAction) {
        this.actionDescriptorList
                .add(ActionDescriptor.builder().description(postAction.getDescription()).time(postAction.getTime())
                        .build());
    }

    public Boolean call() {
        this.actionDescriptorList.forEach(
                actionDescriptor -> {
                    log.info(MessageFormat.format("Ending with {0}", actionDescriptor.getDescription()));
                    try {
                        TimeUnit.MILLISECONDS.sleep(actionDescriptor.getTime());
                    } catch (InterruptedException e) {
                        log.error(e.getMessage(), e);
                    }
                }
        );
        return true;
    }

}